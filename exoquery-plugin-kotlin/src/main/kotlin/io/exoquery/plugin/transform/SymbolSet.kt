package io.exoquery.plugin.transform

import io.exoquery.plugin.extensionParam
import io.exoquery.plugin.logging.Location
import io.exoquery.plugin.regularParams
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol

data class PrintableQuery(val query: String, val xr: XR, val location: Location, val queryOutput: XR.ClassId, val label: String? = null) {
  override fun toString(): String = query
}

sealed interface QueryAccumState {
  fun isEmpty(): Boolean
  fun nonEmpty(): Boolean = !isEmpty()

  data object Empty : QueryAccumState {
    override fun isEmpty(): Boolean = true
  }

  data class RealFile(val file: IrFile, val queries: MutableList<PrintableQuery> = mutableListOf()) : QueryAccumState {
    override fun isEmpty(): Boolean = queries.isEmpty()

    fun addQuery(printableQuery: PrintableQuery) {
      queries.add(printableQuery)
    }
  }

  sealed interface PathBehavior {
    data object IncludePaths : PathBehavior
    data object NoIncludePaths : PathBehavior
  }

  sealed interface LabelBehavior {
    data object IncludeOnlyLabeled : LabelBehavior
    data object IncludeAll : LabelBehavior
  }
}


data class FileQueryAccum(private val state: QueryAccumState) {
  companion object {
    fun empty(): FileQueryAccum = FileQueryAccum(QueryAccumState.Empty)
    fun emptyWithFile(file: IrFile): FileQueryAccum = FileQueryAccum(QueryAccumState.RealFile(file))
  }

  fun hasQueries(): Boolean =
    when (val fileQueryAccum = this.state) {
      is QueryAccumState.Empty ->
        false
      is QueryAccumState.RealFile ->
        fileQueryAccum.queries.isNotEmpty()
    }

  fun currentQueries(): List<PrintableQuery> =
    when (val fileQueryAccum = this.state) {
      is QueryAccumState.Empty ->
        emptyList()
      is QueryAccumState.RealFile ->
        fileQueryAccum.queries
    }

  // TODO add optional arguemnt to take a currentFile path to use if none-exists already?
  fun addQuery(printableQuery: PrintableQuery) {
    when (val fileQueryAccum = this.state) {
      is QueryAccumState.Empty ->
        error("------- Cannot add query to empty file: -------\n${printableQuery.query}")
      is QueryAccumState.RealFile ->
        fileQueryAccum.addQuery(printableQuery)
    }
  }
}

// This is the context that the query transformer passes around. I would have liked it to be completely immutable but
// we need to be able to accumulate queries that are created in the TransformCompileQuery instances therefore
// we have a mutable list of queries here.
data class SymbolSet(
  val symbols: List<IrSymbol>,
  val capturedFunctionParameters: List<IrValueParameter> = emptyList()
) {

  fun withSymbols(newSymbols: List<IrSymbol>) = SymbolSet(this.symbols + newSymbols, capturedFunctionParameters)

  // Add parameters for captured functions (don't think there are cases where this is called in a nested fasion i.e. there are symbols that should exist beforehand)
  fun withCapturedFunctionParameters(capFun: IrSimpleFunction) = run {
    // Note that it is possible for dispatch/extension params to be used inside a function e.g:
    // @CapturedFunction fun Person.firstAndLastName() = capture.expression { this.first() + this.last() }
    val extensionParam = capFun.symbol.owner.extensionParam?.let { listOf(it) } ?: emptyList()
    val dispatchParam = capFun.symbol.owner.dispatchReceiverParameter?.let { listOf(it) } ?: emptyList()
    SymbolSet(symbols, this.capturedFunctionParameters + capFun.regularParams + extensionParam + dispatchParam)
  }

}
