package io.exoquery.plugin.printing

import io.exoquery.plugin.regularParams
import io.exoquery.plugin.safeName
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.text.compareTo

fun IrElement.dumpSimple(color: Boolean = true, normalizeNames: Boolean = false, stableOrder: Boolean = false): String =
  try {
    StringBuilder().also { sb ->
      accept(
        DumpIrTreeVisitor(color, sb,
          DumpIrTreeOptions(normalizeNames, stableOrder)
        ), ""
      )
    }.toString()
  } catch (e: Exception) {
    "(Full dump is not available: ${e.message})\n" + render()
  }

fun IrFile.dumpTreesFromLineNumber(color: Boolean, lineNumber: Int, normalizeNames: Boolean = false): String {
  if (shouldSkipDump()) return ""
  val sb = StringBuilder()
  accept(DumpTreeFromSourceLineVisitor(color, fileEntry, lineNumber, sb, DumpIrTreeOptions(normalizeNames)), null)
  return sb.toString()
}

private fun IrFile.shouldSkipDump(): Boolean {
  val entry = fileEntry as? NaiveSourceBasedFileEntryImpl ?: return false
  return entry.lineStartOffsetsAreEmpty
}

// From getValueParameterNamesForDebug in DumpIrTree.kt, another thing I need to copy because it's internal there
internal fun IrMemberAccessExpression<*>.getValueParameterNamesForDebug(options: DumpIrTreeOptions): List<String> {
  val function = if (options.printMemberAccessExpressionArgumentNames && symbol.isBound) symbol.owner as? IrFunction else null
  return getValueParameterNamesForDebug(function, arguments.size, options)
}


internal fun List<IrDeclaration>.stableOrdered(): List<IrDeclaration> {
  val strictOrder = hashMapOf<IrDeclaration, Int>()

  var idx = 0

  forEach {
    val shouldPreserveRelativeOrder = when (it) {
      is IrProperty -> it.backingField != null && !it.isConst
      is IrAnonymousInitializer, is IrEnumEntry, is IrField -> true
      else -> false
    }
    if (shouldPreserveRelativeOrder) {
      strictOrder[it] = idx++
    }
  }

  return sortedWith { a, b ->
    val strictA = strictOrder[a] ?: Int.MAX_VALUE
    val strictB = strictOrder[b] ?: Int.MAX_VALUE

    if (strictA == strictB) {
      val rA = a.render()
      val rB = b.render()
      rA.compareTo(rB)
    } else strictA - strictB
  }
}

class DumpIrTreeVisitor(
  color: Boolean,
  out: Appendable,
  private val options: DumpIrTreeOptions = DumpIrTreeOptions(),
) : IrVisitor<Unit, String>() {

  private val printer = Printer(out, "  ")
  private val elementRenderer = RenderIrElementVisitorSimple(color, options, isUsedForIrDump = true)
  private fun IrType.render() = elementRenderer.renderType(this)

  private fun List<IrDeclaration>.ordered(): List<IrDeclaration> = if (options.stableOrder) stableOrdered() else this

  private fun IrDeclaration.isHidden(): Boolean = options.isHiddenDeclaration(this)

  override fun visitElement(element: IrElement, data: String) {
    element.dumpLabeledElementWith(data) {
      if (element is IrAnnotationContainer) {
        dumpAnnotations(element)
      }
      element.acceptChildren(this@DumpIrTreeVisitor, "")
    }
  }

  override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
    declaration.dumpLabeledElementWith(data) {
      declaration.files.dumpElements()
    }
  }

  override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: String) {
    declaration.dumpLabeledElementWith(data) {
      declaration.declarations.ordered().dumpElements()
    }
  }

  override fun visitFile(declaration: IrFile, data: String) {
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
      declaration.declarations.ordered().dumpElements()
    }
  }

  override fun visitClass(declaration: IrClass, data: String) {
    if (declaration.isHidden()) return
    if (declaration.isExpect && !options.printExpectDeclarations) return
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
      runIf(options.printSealedSubclasses) {
        declaration.sealedSubclasses.dumpItems("sealedSubclasses") { it.dumpAsReference() }
      }
      declaration.thisReceiver?.accept(this, "thisReceiver")
      declaration.typeParameters.dumpElements()
      declaration.declarations.ordered().dumpElements()
    }
  }

  override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
      declaration.typeParameters.dumpElements()
    }
  }

  override fun visitTypeParameter(declaration: IrTypeParameter, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
    }
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
    if (declaration.isHidden()) return
    if (declaration.isExpect && !options.printExpectDeclarations) return

    declaration.dumpLabeledElementWith(data) {
      declaration.typeParameters.dumpElements()
      declaration.parameters.dumpElements()
      if (options.printAnnotationsInFakeOverrides || !declaration.isFakeOverride) {
        dumpAnnotations(declaration)
      }
      declaration.correspondingPropertySymbol?.dumpAsDeclaration("correspondingProperty")
      declaration.overriddenSymbols.dumpFakeOverrideSymbols()
      declaration.body?.accept(this, "")
    }
  }

  private fun dumpAnnotations(element: IrAnnotationContainer) {
    element.annotations.filterOutSourceRetentions(options).dumpItems("annotations") { irAnnotation: IrConstructorCall ->
      printer.println(elementRenderer.renderAsAnnotation(irAnnotation))
    }
  }

  override fun visitConstructor(declaration: IrConstructor, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      declaration.typeParameters.dumpElements()
      declaration.parameters.dumpElements()
      dumpAnnotations(declaration)
      declaration.body?.accept(this, "")
    }
  }

  override fun visitProperty(declaration: IrProperty, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      if (options.printAnnotationsInFakeOverrides || !declaration.isFakeOverride) {
        dumpAnnotations(declaration)
      }
      if (options.printFakeOverrideSymbolsInPropertiesOfAnonymousClasses ||
        !declaration.parent.let { it is IrClass && it.name == SpecialNames.NO_NAME_PROVIDED }
      ) declaration.overriddenSymbols.dumpFakeOverrideSymbols()

      declaration.backingField?.accept(this, "")
      declaration.getter?.accept(this, "")
      declaration.setter?.accept(this, "")
    }
  }

  override fun visitField(declaration: IrField, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
      declaration.initializer?.accept(this, "")
    }
  }

  private fun List<IrElement>.dumpElements() {
    forEach { it.accept(this@DumpIrTreeVisitor, "") }
  }

  override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.explicitReceiver?.accept(this, "receiver")
      expression.arguments.dumpElements()
    }
  }

  override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
    if (declaration.isHidden()) return
    declaration.dumpLabeledElementWith(data) {
      dumpAnnotations(declaration)
      declaration.initializerExpression?.accept(this, "init")
      declaration.correspondingClass?.accept(this, "class")
    }
  }

  override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: String) {
    expression.dumpLabeledElementWith(data) {
      //dumpTypeArguments(expression)
      val valueParameterNames = expression.getValueParameterNamesForDebug(options)
      for ((index, value) in expression.arguments.withIndex()) {
        value?.accept(this, "${valueParameterNames[index]}")
      }
    }
  }

  private fun dumpTypeArguments(expression: IrMemberAccessExpression<*>) {
    val typeParameterNames = expression.getTypeParameterNames(expression.typeArguments.size)
    for (index in 0 until expression.typeArguments.size) {
      val typeParameterName = typeParameterNames[index]
      val prefix = if (expression is IrConstructorCall && index < expression.classTypeArgumentsCount) "(of class) " else ""
      printer.println("TYPE_ARG $prefix$typeParameterName: ${expression.renderTypeArgument(index)}")
    }
  }

  private fun IrMemberAccessExpression<*>.getTypeParameterNames(expectedCount: Int): List<String> =
    if (options.printMemberAccessExpressionArgumentNames && symbol.isBound)
      symbol.owner.getTypeParameterNames(expectedCount)
    else
      getPlaceholderParameterNames(expectedCount)

  private fun IrSymbolOwner.getTypeParameterNames(expectedCount: Int): List<String> =
    if (this is IrTypeParametersContainer) {
      val typeParameters = if (this is IrConstructor) getFullTypeParametersList() else this.typeParameters
      (0 until expectedCount).map {
        if (it < typeParameters.size)
          typeParameters[it].name.asString()
        else
          "${it + 1}"
      }
    } else {
      getPlaceholderParameterNames(expectedCount)
    }

  private fun IrConstructor.getFullTypeParametersList(): List<IrTypeParameter> {
    val parentClass = try {
      parent as? IrClass ?: return typeParameters
    } catch (e: Exception) {
      return typeParameters
    }
    return parentClass.typeParameters + typeParameters
  }

  private fun IrMemberAccessExpression<*>.renderTypeArgument(index: Int): String =
    this.typeArguments[index]?.render() ?: "<none>"

  override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: String) {
    inlinedBlock.dumpLabeledElementWith(data) {
      inlinedBlock.inlinedFunctionSymbol?.dumpAsReference("inlinedFunctionSymbol")
      inlinedBlock.inlinedFunctionFileEntry.dumpInternal("inlinedFunctionFileEntry")
      inlinedBlock.acceptChildren(this, "")
    }
  }

  override fun visitGetField(expression: IrGetField, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.receiver?.accept(this, "receiver")
    }
  }

  override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.overriddenFunctionSymbol.dumpAsReference("overriddenFunctionSymbol")
      val parameterNames = getValueParameterNamesForDebug(expression.invokeFunction, expression.boundValues.size, options)
      expression.boundValues.forEachIndexed { index, value ->
        val name = parameterNames[index]
        value.accept(this, "bound $name")
      }
      expression.invokeFunction.accept(this, "invoke")
    }
  }

  override fun visitRichPropertyReference(expression: IrRichPropertyReference, data: String) {
    expression.dumpLabeledElementWith(data) {
      val parameterNames = getValueParameterNamesForDebug(expression.getterFunction, expression.boundValues.size, options)
      expression.boundValues.forEachIndexed { index, value ->
        val name = parameterNames[index]
        value.accept(this, "bound $name")
      }
      expression.getterFunction.accept(this, "getter")
      expression.setterFunction?.accept(this, "setter")
    }
  }

  override fun visitSetField(expression: IrSetField, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.receiver?.accept(this, "receiver")
      expression.value.accept(this, "value")
    }
  }

  override fun visitWhen(expression: IrWhen, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.branches.dumpElements()
    }
  }

  override fun visitBranch(branch: IrBranch, data: String) {
    branch.dumpLabeledElementWith(data) {
      branch.condition.accept(this, "if")
      branch.result.accept(this, "then")
    }
  }

  override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
    loop.dumpLabeledElementWith(data) {
      loop.condition.accept(this, "condition")
      loop.body?.accept(this, "body")
    }
  }

  override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
    loop.dumpLabeledElementWith(data) {
      loop.body?.accept(this, "body")
      loop.condition.accept(this, "condition")
    }
  }

  override fun visitTry(aTry: IrTry, data: String) {
    aTry.dumpLabeledElementWith(data) {
      aTry.tryResult.accept(this, "try")
      aTry.catches.dumpElements()
      aTry.finallyExpression?.accept(this, "finally")
    }
  }

  override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.acceptChildren(this, "")
    }
  }

  override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) {
    expression.dumpLabeledElementWith(data) {
      expression.receiver.accept(this, "receiver")
      for ((i, arg) in expression.arguments.withIndex()) {
        arg.accept(this, i.toString())
      }
    }
  }

  override fun visitConstantArray(expression: IrConstantArray, data: String) {
    expression.dumpLabeledElementWith(data) {
      for ((i, value) in expression.elements.withIndex()) {
        value.accept(this, i.toString())
      }
    }
  }

  override fun visitConstantObject(expression: IrConstantObject, data: String) {
    expression.dumpLabeledElementWith(data) {
      for ((index, argument) in expression.valueArguments.withIndex()) {
        argument.accept(this, expression.constructor.owner.parameters[index].name.toString())
      }
    }
  }

  private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
    printer.println(accept(elementRenderer, null).withLabel(label))
    indented(body)
  }

  private inline fun <T> Collection<T>.dumpItems(caption: String, renderElement: (T) -> Unit) {
    if (isEmpty()) return
    indented(caption) {
      forEach {
        renderElement(it)
      }
    }
  }

  private fun Collection<IrSymbol>.dumpFakeOverrideSymbols() {
    if (isEmpty()) return
    elementRenderer.withHiddenParameterNames {
      indented("overridden") {
        map(elementRenderer::renderSymbolReference)
          .applyIf(options.stableOrderOfOverriddenSymbols) { sorted() }
          .forEach { printer.println(it) }
      }
    }
  }

  private fun IrSymbol.dumpAsDeclaration(label: String) {
    if (isBound)
      printer.println("$label: ", owner.accept(elementRenderer, null))
    else
      printer.println("$label: UNBOUND ${javaClass.simpleName}")
  }

  private fun IrSymbol.dumpAsReference(label: String? = null) =
    printer.println(
      elementRenderer.renderSymbolReference(this).let {
        if (label != null) "$label: $it" else it
      }
    )

  private fun IrFileEntry.dumpInternal(label: String? = null) {
    val prefix = if (label != null) "$label: " else ""
    val renderedText = elementRenderer.renderFileEntry(this)
    printer.println(prefix + renderedText)
  }

  private inline fun indented(label: String, body: () -> Unit) {
    printer.println("$label:")
    indented(body)
  }

  private inline fun indented(body: () -> Unit) {
    printer.pushIndent()
    body()
    printer.popIndent()
  }

  private fun String.withLabel(label: String) =
    if (label.isEmpty()) this else "$label: $this"
}

class DumpTreeFromSourceLineVisitor(
  val color: Boolean,
  val fileEntry: IrFileEntry,
  private val lineNumber: Int,
  out: Appendable,
  options: DumpIrTreeOptions,
) : IrVisitorVoid() {
  private val dumper = DumpIrTreeVisitor(color, out, options)

  override fun visitElement(element: IrElement) {
    if (fileEntry.getLineNumber(element.startOffset) == lineNumber) {
      element.accept(dumper, "")
      return
    }

    element.acceptChildrenVoid(this)
  }
}

internal fun getValueParameterNamesForDebug(function: IrFunction?, amount: Int, options: DumpIrTreeOptions): List<String> {
  return (0..<amount).map { index ->
    val param = function?.parameters?.getOrNull(index)
    param?.renderValueParameterName(options, disambiguate = true) ?: "${index + 1}"
  }
}

internal fun getPlaceholderParameterNames(expectedCount: Int) =
  (1..expectedCount).map { "$it" }
