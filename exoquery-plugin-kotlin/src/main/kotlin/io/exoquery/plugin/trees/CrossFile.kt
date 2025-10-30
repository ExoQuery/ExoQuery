package io.exoquery.plugin.trees

import io.exoquery.annotation.SqlDynamic
import io.exoquery.parseError
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.safeName
import io.exoquery.plugin.transform.CX
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

object CrossFile {

  context(CX.Scope, CX.StoredXRsScope)
  fun printStoredValues(): Unit =
    logger.warn(storedXRs.printStored())

  // TODO Need to account for what happens if this is a class-member
  context(CX.Scope)
  private fun IrDeclarationParent.isNotCurrentFile() =
    this is IrFile && this.fileEntry.name != currentFile.fileEntry.name

  context(CX.Scope)
  private fun IrDeclarationParent.findRoot(): IrDeclarationParent {
    val origin = this
    fun recurseToRoot(accum: List<IrDeclarationParent>, current: IrDeclarationParent): IrDeclarationParent =
      when (current) {
        is IrFunction -> recurseToRoot(accum + current, current.parent)
        is IrField -> recurseToRoot(accum + current, current.parent)
        is IrFile -> current
        is IrExternalPackageFragment -> current
        is IrClass -> recurseToRoot(accum + current, current.parent)
        // Technically we could use this to cover most things (IrFunction, IrClass, IrFile, IrPackageFragment)
        is IrDeclarationBase -> recurseToRoot(accum + current, current.parent)
        else -> parseError("Cannot get root-parent of ${origin.functionParentInfo()}. Path was: [${accum.map { it.functionParentInfo() }.joinToString(" -> ")}].\n=============== Encountered unknown element ${current::class.simpleName}: ===============\n${current.dumpKotlinLike()}", origin)
      }
    return recurseToRoot(listOf(origin), origin)
  }


  private fun IrDeclarationParent.functionParentInfo() =
    when (this) {
      is IrFile -> "IrFile(${this.fileEntry.name})"
      is IrExternalPackageFragment -> "IrExternalPackage(${this.packageFqName.asString()})"
      is IrClass -> "IrClass(${this.kotlinFqName.asString()})"
      else -> "Other(${this::class.simpleName})"
    }

  context(CX.Scope)
  fun isCrossFile(function: IrFunction): Boolean =
    function.returnType.hasAnnotation(FqName("io.exoquery.Captured")) &&
      !function.hasAnnotation<SqlDynamic>() &&
      (function.parent is IrExternalPackageFragment || function.findRoot().isNotCurrentFile()) &&
      function.isInline

  context(CX.Scope)
  fun validatePossibleCrossFileFunction(function: IrFunction, originalExpression: IrExpression): Unit =
    if (isCrossFile(function) && !function.isInline)
      // TODO mention that runtimes need to marked as @CapturedDynamic and don't need to be inline
      parseError("Any function sharing an SqlQuery (or SqlExpression/SqlAction/SqlBatchAction) across files must be inline so that it can be reified into the calling file but the function (or val) `${function.symbol.safeName}` was not. Please make this function (or val) into an inline function.\n(DebugInfo: parent was ${function.parent.functionParentInfo()})", originalExpression)
    else
      Unit

  context(CX.Scope)
  private fun isCrossFile(function: IrField): Boolean =
    (function.parent is IrExternalPackageFragment || function.findRoot().isNotCurrentFile())

  context(CX.Scope)
  fun validatePossibleCrossFileField(field: IrField, originalExpression: IrExpression): Unit =
    if (isCrossFile(field))
      parseError("Fields cannot be used to share an SqlQuery (or SqlExpression/SqlAction/SqlBatchAction) across files because they cannot be inlined. Please use a inline-function instead of the field `${field.symbol.safeName}`.\n(DebugInfo: parent was ${field.parent.functionParentInfo()})", originalExpression)
    else
      Unit

  context(CX.Scope, CX.StoredXRsScope)
  fun getUprootableFromStore(function: IrFunction, containerType: OwnerChain.ContainerType.Query) =
    storedXRs.getStored(function, containerType)?.let { packedXR -> SqlQueryExpr.Uprootable(packedXR) }
  context(CX.Scope, CX.StoredXRsScope)
  fun getUprootableFromStore(function: IrFunction, containerType: OwnerChain.ContainerType.Expr) =
    storedXRs.getStored(function, containerType)?.let { packedXR -> SqlExpressionExpr.Uprootable(packedXR) }
  context(CX.Scope, CX.StoredXRsScope)
  fun getUprootableFromStore(function: IrFunction, containerType: OwnerChain.ContainerType.Action) =
    storedXRs.getStored(function, containerType)?.let { packedXR -> SqlActionExpr.Uprootable(packedXR) }
  context(CX.Scope, CX.StoredXRsScope)
  fun getUprootableFromStore(function: IrFunction, containerType: OwnerChain.ContainerType.ActionBatch) =
    storedXRs.getStored(function, containerType)?.let { packedXR -> SqlBatchActionExpr.Uprootable(packedXR) }

  context(CX.Scope, CX.StoredXRsScope)
  fun hasUprootableInStore(function: IrFunction, containerType: OwnerChain.ContainerType) =
    storedXRs.getStored(function, containerType) != null

  context(CX.Scope)
  fun hasUprootableInStoreIndependent(function: IrFunction, containerType: OwnerChain.ContainerType) =
    storedXRsScope.scoped {
      storedXRs.getStored(function, containerType) != null
    }


  context(CX.Scope, CX.StoredXRsScope)
  fun putUprootableIfCrossFile(function: IrFunction, containerType: OwnerChain.ContainerType, uprootable: UprootableExpr): Unit {
    if (function.isInline) {
      storedXRs.putStored(function, containerType, uprootable.packedXR)
      // TODO need to add some kind of varaible to the scope to enable debugging the StoredXRs logging
      // logger.warn("PUTTING UPROOTABLE for cross-file function `${function.symbol.safeName}`\n================ Value ================\n${uprootable.show()}\n============ Values in Store ============\n${storedXRs.printStored()}")
    }
  }
}
