package io.exoquery.plugin.transform

import io.exoquery.plugin.refinedStableIdentifier
import io.exoquery.plugin.safeName
import io.exoquery.plugin.stableIdentifier
import io.exoquery.plugin.trees.OwnerChain
import io.exoquery.unpackAction
import io.exoquery.unpackBatchAction
import io.exoquery.unpackExpr
import io.exoquery.unpackQuery
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.mapdb.DB
import org.mapdb.DBMaker

class CompileTimeStoredXRsScope(private val buildDirPath: String, private val sourceSet: String, private val dependentSourceSets: List<String>, private val mode: StorageMode) {
  sealed interface StorageMode {
    object Persistent: StorageMode
    object Transient: StorageMode
  }

  context(scope: CX.Scope)
  fun tryPrintStore(): String = run {
    try {
      load().use {
        it.printStored()
      }
    } catch (ex: Exception) {
      "Error loading stored XRs from build directory $buildDirPath: ${ex.message}"
    }
  }

  private fun load(): CompileTimeStoredXRs {
    val buildDir = java.io.File(buildDirPath)
    if (!buildDir.exists()) {
      val couldMakeDirs = buildDir.mkdirs()
      if (!couldMakeDirs) {
        error("Could not create build directory at $buildDirPath")
      }
    }

    fun makePersistentDB(): DB =
      DBMaker
        .fileDB(java.io.File(buildDir, "StoredXRs.db"))
        .fileLockWait(30_000) // wait up to 30s for lock
        .closeOnJvmShutdown()
        .make()

    fun makeTransientDB(): DB =
      DBMaker.memoryDB().make()

    val db =
      when (mode) {
        StorageMode.Persistent -> makePersistentDB()
        StorageMode.Transient -> makeTransientDB()
      }

    val primaryMap: MutableMap<String, String> =
      db.hashMap("map-${sourceSet}", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).createOrOpen()
    val dependentMaps: List<Pair<String, Map<String, String>>> =
      dependentSourceSets.map { dep ->
        dep to db.hashMap("map-${dep}", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).createOrOpen()
      }


    return CompileTimeStoredXRs(db, sourceSet, primaryMap, dependentMaps)
  }

  fun <R> scoped(block: CX.StoredXRsScope.() -> R): R {
    val output = load().use { storedXRs ->
      block(CX.StoredXRsScope(storedXRs))
    }
    return output
  }
}


private data class ComboMap private constructor(
  private val dependentSourceSetMaps: List<DependentSourceSetMap>,
  private val sourceSetName: String,
  private val sourceSetMap: MutableMap<String, String>,
) {
  private data class DependentSourceSetMap(
    val sourceSet: String,
    val map: Map<String, String>
  )

  // As an optimization put all the dependentSourceSetMaps into a single read-only map that is only created once
  // this will make lookups faster. Last to write the element in wins
  private val unifiedDependentMap =
    // combine the dependen maps, if there is key-overlap the last one in the list wins
    dependentSourceSetMaps.fold(mutableMapOf<String, String>()) { acc, depMap ->
      acc.putAll(depMap.map)
      acc
    }.toMap()

  operator fun get(value: String) =
    sourceSetMap[value] ?: unifiedDependentMap[value]
  fun put(key: String, value: String) =
    sourceSetMap.put(key, value)

  fun listMaps(): List<Pair<String, Map<String, String>>> =
    listOf(sourceSetName to sourceSetMap) + dependentSourceSetMaps.map { it.sourceSet to it.map }

  companion object {
    operator fun invoke(
      sourceSetName: String,
      sourceSetMap: MutableMap<String, String>,
      dependentSourceSetMaps: List<Pair<String, Map<String, String>>>
    ): ComboMap =
      ComboMap(
        dependentSourceSetMaps.map { (key, value) -> DependentSourceSetMap(key, value) },
        sourceSetName,
        sourceSetMap
      )
  }
}

// TODO this needs to use storage
class CompileTimeStoredXRs private constructor(
  private val db: DB,
  private val functionMap: ComboMap
): AutoCloseable {
  override fun close() {
    db.close()
  }

  //private val functionMap = mutableMapOf<String, String>()

  context(scope: CX.Scope)
  fun printStored(): String = run {
    fun printSourceSet(name: String, sourceSetMap: Map<String, String>): String = run {
      "------ SourceSet: ${name} (${sourceSetMap.size} Entries) ------\n" +
          sourceSetMap.entries.sortedBy { it.key }.mapNotNull { entry ->
            unpackAndPrintEntry(entry)
          }.joinToString("\n")
    }
    "================ Stored XRs ================\n" +
      functionMap.listMaps().joinToString("\n") { (name, map) -> printSourceSet(name, map) }
  }

  context(scope: CX.Scope)
  private fun unpackAndPrintEntry(entry: Map.Entry<String, String>): String? =
    decodeValue(entry.value)?.let { (containerType, xrStr) ->
      val xr = when (containerType) {
        OwnerChain.ContainerType.Query -> unpackQuery(xrStr)
        OwnerChain.ContainerType.Expr -> unpackExpr(xrStr)
        OwnerChain.ContainerType.Action -> unpackAction(xrStr)
        OwnerChain.ContainerType.ActionBatch -> unpackBatchAction(xrStr)
      }
      "Function `${entry.key}` stored as ${containerType.toKeyString()}:\n${xr.show()}\n"
    }

  fun OwnerChain.ContainerType.toKeyString(): String = when(this) {
    OwnerChain.ContainerType.Query -> "Query"
    OwnerChain.ContainerType.Expr -> "Expr"
    OwnerChain.ContainerType.Action -> "Action"
    OwnerChain.ContainerType.ActionBatch -> "ActionBatch"
  }
  context(scope: CX.Scope)
  fun String.Companion.fromKeyString(key: String): OwnerChain.ContainerType? = when(key) {
    "Query" -> OwnerChain.ContainerType.Query
    "Expr" -> OwnerChain.ContainerType.Expr
    "Action" -> OwnerChain.ContainerType.Action
    "ActionBatch" -> OwnerChain.ContainerType.ActionBatch
    else -> {
      scope.logger.warn("Unknown ContainerType key string: $key")
      null
    }
  }


  // should be ${ContainerType.keyString()}-${packedXR}
  private fun encodeValue(containerType: OwnerChain.ContainerType, packedXR: String) =
    "${containerType.toKeyString()}-$packedXR"

  context(scope: CX.Scope)
  private fun decodeValue(packed: String): Pair<OwnerChain.ContainerType, String>? {
    val split = packed.indexOf('-')
    if (split < 0) {
      scope.logger.warn("Invalid packed XR value ${packed}, missing container type prefix")
      return null
    }
    val (typeStr, xrStr) = packed.substring(0, split) to packed.substring(split + 1)
    val containerType = String.fromKeyString(typeStr) ?: return null
    return containerType to xrStr
  }

  private fun putProper(functionId: String, containerType: OwnerChain.ContainerType, packedXR: String) =
    functionMap.put(functionId, encodeValue(containerType, packedXR))

  fun putStored(function: IrFunction, containerType: OwnerChain.ContainerType, packedXR: String) =
    putProper(function.stableIdentifier(),  containerType, packedXR)

  context(scope: CX.Scope)
  fun getStored(function: IrFunction, containerType: OwnerChain.ContainerType): String? = run {
    val stableIdentifier = function.refinedStableIdentifier()

    functionMap[stableIdentifier]?.let { packed ->
      val (storedType, xrStr) = decodeValue(packed) ?: return null
      if (storedType != containerType) {
        // Typically this means that the user has change the actual type of the function (e.g. changed it from a SqlQuery to a SqlExpression)
        scope.logger.warn("Stored XR type $storedType does not match requested type $containerType for function `${function.symbol.safeName}`", function)
        null
      }
      xrStr
    }
  }

  companion object {
    operator fun invoke(
      db: DB,
      sourceSet: String,
      sourceSetMap: MutableMap<String, String>,
      dependentSourceSets: List<Pair<String, Map<String, String>>>
    ): CompileTimeStoredXRs =
      CompileTimeStoredXRs(
        db,
        ComboMap(
          sourceSet,
          sourceSetMap,
          dependentSourceSets
        )
      )
  }
}
