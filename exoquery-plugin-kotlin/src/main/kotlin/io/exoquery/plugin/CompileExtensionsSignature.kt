package io.exoquery.plugin

import io.exoquery.annotation.CapturedFunctionSketch
import io.exoquery.parseError
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.trees.Unlifter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
import kotlin.toString


// TODO need to test cases with overloads and captured-functions
// TODO at some point we also want to add the ParamKinds to the stable identifier so that we can differentiate
//      between functions that have the same types but different param kinds (e.g. extension-param vs regular-param)
context(scope: CX.Scope)
fun IrFunction.refinedStableIdentifier() = run {
  val function = this
  // TODO at some point we also want to add the ParamKinds to the stable identifier so that we can differentiate
  //      between functions that have the same types but different param kinds (e.g. extension-param vs regular-param)
  // If it is a processed captured-function (i.e. it was processed in this compilation unit) then the arguments
  // have been removed. The way to find them and the types (which is required for the stable-identifier) is to look at the
  // @SqlFunctionSketch annotation contents
  if (function.hasAnnotation<CapturedFunctionSketch>()) {
    function.getAnnotation<CapturedFunctionSketch>()?.let {
      this.callableId.toString() + "(${Unlifter.unliftCapturedFunctionSketch(it).sketch.map { it.stableIdent }.joinToString(",")})"
    } ?: parseError("Illegal @SqlFunctionSketch was created on function `${function.symbol.safeName}`", function)
  }
  else function.stableIdentifier()
}

context(scope: CX.Scope)
fun IrFunction.refinedStableIdentifierOrErrorText() = run {
  try {
    this.refinedStableIdentifier()
  } catch (e: Throwable) {
    "<NO Stable Identifier for ${this.symbol.safeName}>"
  }
}


/**
 * A unique stable identifier for functions. This is used for tracking cross-file functions in StoredXRs.db. In most cases
 * just the full-path (i.e. callableId) of the function would be sufficient, but we also need to account for overloads and generics.
 *
 * Note that we do not need to include the output type since changing the output type would just cause the whole thing to be recompiled (since a cross-file
 * function needs to be inline the call-site would be recompiled again). The only situation where we need anything more than the full-path of the function is
 * where there are overloads.
 *
 * TODO need to consider the kinds of arguments as well, see extractCapturedFunctionParamKinds for an example of those
 */
fun IrFunction.stableIdentifier(): String =
  this.callableId.toString() + "(${this.parameters.joinToString(",") { it.type.stableIdentifier() }})" //:${this.returnType.stableIdentifier()}

fun IrType.stableIdentifier(): String = run {
  val sb = StringBuilder()
  TypeDumper(Printer(sb, 1, "  "))(this)
  return sb.toString()
}

private class TypeDumper(val p: Printer) {

  operator fun invoke(type: IrType) {
    type.printTypeWithNoIndent()
  }

  private fun p(condition: Boolean, s: String) {
    if (condition) p.printWithNoIndent("$s ")
  }
  private fun <T : Any> p(value: T?, defaultValue: T? = null, getString: T.() -> String) {
    if (value == null) return
    p(value != defaultValue, value.getString())
  }

  private fun IrType.printTypeWithNoIndent() {
    when (this) {
      is IrSimpleType -> {
        p.printWithNoIndent(classFqName?.asString() ?: classifier.safeName)

        if (arguments.isNotEmpty()) {
          p.printWithNoIndent("<")
          arguments.forEachIndexed { i, typeArg ->
            p(i > 0, ",")

            typeArg.printTypeArgumentWithNoIndent()
          }
          p.printWithNoIndent(">")
        }

        if (isMarkedNullable()) p.printWithNoIndent("?")
      }
      is IrDynamicType ->
        p.printWithNoIndent("dynamic")
      is IrErrorType ->
        p.printWithNoIndent("ErrorType")
    }
  }

  private fun IrTypeArgument.printTypeArgumentWithNoIndent() {
    when (this) {
      is IrStarProjection ->
        p.printWithNoIndent("*")
      is IrTypeProjection -> {
        variance.printVarianceWithNoIndent()
        type.printTypeWithNoIndent()
      }
    }
  }

  private fun Variance.printVarianceWithNoIndent() {
    p(this, Variance.INVARIANT) { label }
  }

}
