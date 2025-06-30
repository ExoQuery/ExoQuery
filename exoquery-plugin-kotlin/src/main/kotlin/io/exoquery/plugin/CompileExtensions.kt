package io.exoquery.plugin

import io.decomat.Is
import io.decomat.case
import io.decomat.match
import io.exoquery.*
import io.exoquery.annotation.ChangeReciever
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.Caller
import io.exoquery.plugin.transform.createLambda0
import io.exoquery.plugin.trees.Ir
import io.exoquery.plugin.trees.ParamKind
import io.exoquery.plugin.trees.fullPathOfBasic
import io.exoquery.plugin.trees.simpleTypeArgs
import io.exoquery.xr.XR
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.eraseTypeParameters
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

fun IrCall.filterParamsByKind(f: (IrParameterKind) -> Boolean): List<IrExpression?> = run {
  this.symbol.owner.parameters.filter { f(it.kind) }.map { this.arguments.get(it) }
}

/**
 * All value parameters.
 *
 * Parameters must follow this order:
 *
 * [[dispatch receiver, context parameters, extension receiver, regular parameters]].
 */
val IrCall.extensionArg get(): IrExpression? = run {
  for (param in this.symbol.owner.parameters) {
    if (param.kind == IrParameterKind.ExtensionReceiver) {
      return@run this.arguments[param]
    } else if (param.kind == IrParameterKind.Regular) {
      // If we find regular parameter, we can stop looking for extension receivers
      // because there can only be regular parameters after the dispatch receiver.
      return null
    }
  }
  return null
}

val IrCall.dispatchArg get() =  this.dispatchReceiver

val IrFunction.extensionParam get(): IrValueParameter? = run {
  for (param in this.symbol.owner.parameters) {
    if (param.kind == IrParameterKind.ExtensionReceiver) {
      return@run param
    } else if (param.kind == IrParameterKind.Regular) {
      // If we find regular parameter, we can stop looking for extension receivers
      // because there can only be regular parameters after the dispatch receiver.
      return null
    }
  }
  return null
}

val IrFunction.regularParams get() =
  this.parameters.filter { it.kind == IrParameterKind.Regular }

context(CX.Scope)
val IrCall.argumentsWithParameters get() = run {
  // TODO optimize to return a iterator
  val params = this.symbol.owner.parameters
  val args = this.arguments
  if (params.size != args.size)
    parseError(
      "Mismatched params (${params.size}) and args (${args.size}) in function:\nParams: ${params.map { it.dumpKotlinLike() }}\nArgs: ${args.map { it?.dumpKotlinLike() }}",
      this
    )
  args zip params
}

fun IrCall.regularArgsWithParamKinds(paramKinds: List<ParamKind>) =
  (arguments zip paramKinds).filter { (arg, kind) -> kind == ParamKind.Regular }.map { (arg, _) -> arg }
fun IrCall.extensionArgWithParamKinds(paramKinds: List<ParamKind>): IrExpression? =
  (arguments zip paramKinds).find { (arg, kind) -> kind == ParamKind.Extension }?.first


val IrCall.regularArgs get() = run {
  // TODO optimize to return a iterator
  val params = this.symbol.owner.parameters
  val args = this.arguments
  params.filter { param -> param.kind == IrParameterKind.Regular }.map { args[it] }
}
val IrCall.regularArgsCount get() = run {
  val params = this.symbol.owner.parameters
  params.count { param -> param.kind == IrParameterKind.Regular }
}

val IrConstructorCall.regularArgs get() = run {
  val params = this.symbol.owner.parameters
  val args = this.arguments
  params.filter { param -> param.kind == IrParameterKind.Regular }.map { args[it] }
}
val IrConstantObject.regularArgs get() = run {
  // TODO optimize to return a iterator
  val params = this.constructor.owner.parameters
  val args = this.valueArguments
  params.filter { param -> param.kind == IrParameterKind.Regular }.map { args[it.indexInParameters] }
}


fun IrType.toClassIdXR(): XR.ClassId = run {
  val classXR =
    this.classId()?.toXR() ?: when (this) {
      is IrSimpleType -> XR.ClassId(XR.FqName(this.classifier.safeName), XR.FqName.Empty)
      is IrDynamicType -> XR.ClassId(XR.FqName("dynamic"), XR.FqName.Empty)
      is IrErrorType -> XR.ClassId(XR.FqName("ErrorType"), XR.FqName.Empty)
    }

  val args = this.simpleTypeArgs.map { it.toClassIdXR() }
  classXR.copy(nullable = this.isMarkedNullable(), typeArgs = args)
}

fun KClass<*>.classId(): ClassId? = run {
  val cls = this
  cls.qualifiedName?.let { fullPath ->
    // a bunch of things like the kotlin classpath e.g. kotlin.Boolean etc... actuall resolve to java.lang.Boolean
    // on the JVM so the JVM package path is diefferent that the kotlin package path. Need to do an early-return in such cases
    if (fullPath.startsWith("kotlin."))
      ClassId.topLevel(FqName(fullPath))
    else {

      // foo.bar in foo.bar.Baz.Blin
      val packageName = cls.java.packageName
      // the full path foo.bar.Baz.Blin

      if (!fullPath.startsWith(packageName))
        liftingError("Qualified name of class $fullPath did not start with package name $packageName")

      // the Baz.Blin part
      val className = fullPath.replace(packageName, "").dropWhile { it == '.' } // after we replaced foo.bar with "" there's still a leading "." that wee need to remove
      ClassId(FqName(packageName), FqName(className), false)
    }
  }
}

fun KClass<*>.classIdOrEmpty(): ClassId =
  this.classId() ?: ClassId.topLevel(FqName.topLevel(Name.identifier("Empty")))

fun KClass<*>.classIdOrDefault(default: ClassId): ClassId =
  this.classId() ?: default

sealed interface MethodType {
  data class Getter(val sym: IrSimpleFunctionSymbol) : MethodType
  data class Method(val sym: IrSimpleFunctionSymbol) : MethodType
}

fun IrType.findMethodOrFail(methodName: String, valueParamsSize: Int): MethodType = run {
  val cls =
    (this.classOrNull ?: error("Cannot locate the method ${methodName} from the type: ${this.dumpKotlinLike()} type is not a class."))

  fun printError(addition: String = ""): Nothing =
    error(
      """|
        |Cannot locate the method `${methodName}` from the type: `${this.dumpKotlinLike()}` because the method does not exist.${addition}
        |-------------- Available methods --------------
        |${cls.functions.joinToString("\n") { it.safeName }}
        |-------------- Available properties --------------
        |${cls.dataClassProperties().mapNotNull { cls.getPropertyGetter(it.first)?.safeName }.joinToString("\n")}
        |""".trimMargin()
    )

  cls.functions.find { it.safeName == methodName && it.owner.regularParams.size == valueParamsSize }?.let { MethodType.Method(it) }
    ?: run {
      try {
        cls.getPropertyGetter(methodName)?.let { MethodType.Getter(it) }
      } catch (e: AssertionError) {
        printError(" Also, attempting getPropertyGetter caused an assert-error.")
      }
    }
    ?: printError()
}

// WARNING assuming (for now) that the extension methods are in the same package as the Class they're being called from.
// can relax this assumption later by adding an optional package-field to ReplacementMethodToCall and propagating it here
// TODO Need to filter by reciever type i.e. what if there are multiple extension functions named the same thing
context(CX.Scope) fun IrType.findExtensionMethodOrFail(methodName: String, valueParamsSize: Int) = run {
  (this
    .classOrNull ?: error("Cannot locate the method ${methodName} from the type: ${this.dumpKotlinLike()} type is not a class."))
    .let { classSym ->
      pluginCtx.referenceFunctions(CallableId(FqName(classSym.owner.packageFqName.toString()), Name.identifier(methodName))).firstOrNull()?.let { MethodType.Method(it) }
        ?: error("Cannot locate the extension method ${classSym.owner.packageFqName.toString()}.${methodName} from the type: ${this.dumpKotlinLike()} because the method does not exist.")
    }
}

fun IrClassSymbol.isDataClass() = this.owner.isData

//context(LocateableContext)
//fun IrClassSymbol.dataClassMethods() {
//  val params =
//    (this.constructors.firstOrNull() ?: parseError("No constructors found for data-class: ${this.owner.name}"))
//      .owner.valueParameters
//
//  params.map { param ->
//    // check if the param is public or private
//    //val isPublic = param.symbol.isPublicApi
//
//  }
//
//  TODO()
//}

/**
 * Useful for getting things like `@ExoField("first_name")` or `@SerialName("first_name")`
 * after expressions like `irCall.getPropertyAnnotationArgs<ExoField>()` or `irProperty.getAnnotation<ExoField>()`
 */
fun List<IrExpression>.firstConstStringOrNull(): String? =
  this.firstOrNull()?.let { it as? IrConst }?.value?.toString()

fun IrClassSymbol.dataClassProperties() =
  if (this.isDataClass()) {
    // NOTE: Does not support data-classes with multiple constructors.
    // Constructor params are in the right order. The properties of the class are not.
    val constructorParams = this.constructors.firstOrNull()?.owner?.regularParams ?: setOf()
    //this.owner.properties
    //  .filter { constructorParams.contains(it.name) && it.getter != null }
    //  .map { it.name.toString() to it.getter!!.returnType }
    constructorParams.map { param -> param.name.asString() to param.type }
  } else listOf()


val IrSimpleFunctionSymbol.fullName get() = this.owner.kotlinFqName.asString()

val IrCall.symName get() = this.symbol.safeName

fun IrGetValue.isFunctionParam() =
  this.symbol.owner is IrValueParameter && this.symbol.owner.parent is IrSimpleFunction

fun IrGetValue.ownerFunction() =
  (this.symbol.owner as? IrValueParameter)?.let { it.parent as? IrSimpleFunction }

val IrSymbol.safeName
  get() =
    (if (owner is IrFunction && (owner as IrFunction).isPropertyAccessor) {
      (owner as IrFunction).name.asStringStripSpecialMarkers().removePrefix("get-")
    } else if (isBound) {
      (owner as? IrDeclarationWithName)?.name?.asString() ?: "<???>"
    } else {
      "<???>"
    }).replace("$", "")

fun IrElement.location(fileEntry: IrFileEntry): CompilerMessageSourceLocation {
  val irElement = this
  val sourceRangeInfo = fileEntry.getSourceRangeInfo(
    beginOffset = irElement.startOffset ?: UNDEFINED_OFFSET,
    endOffset = irElement.endOffset ?: UNDEFINED_OFFSET
  )
  val messageWithRange = CompilerMessageLocationWithRange.create(
    path = sourceRangeInfo.filePath,
    lineStart = sourceRangeInfo.startLineNumber + 1,
    columnStart = sourceRangeInfo.startColumnNumber + 1,
    lineEnd = sourceRangeInfo.endLineNumber + 1,
    columnEnd = sourceRangeInfo.endColumnNumber + 1,
    lineContent = null
  )!!
  return messageWithRange
}

fun IrType.isDataClass() =
  this.classOrNull?.isDataClass() ?: false

fun CompilerMessageSourceLocation.show() =
  "${path}:${line}:${column}"

val IrCall.funName get() = this.symbol.safeName

val IrCall.ownerFunction
  get() =
    this.symbol.owner

context(CX.Scope)
fun IrCall.zipArgsWithParamsOrFail() =
  ownerFunction.regularParams.zipIfSizesAreEqual(regularArgs)
    ?: parseError(
      "Mismatched parts (${ownerFunction.regularParams.size})  and params (${regularArgs.size}) in function:\nParts: ${ownerFunction.regularParams.map { it.source() }}\nParams: ${regularArgs.map { it?.source() }}",
      this
    )

val IrGetValue.ownerFunName
  get() =
    (this.symbol.owner as? IrFunction)?.let { it.symbol.safeName }

// TODO change to LocationContainingContext
context(CX.Scope) fun IrElement.location(): CompilerMessageSourceLocation =
  this.location(currentFile.fileEntry)

fun IrFile.location(): CompilerMessageSourceLocation =
  this.location(this.fileEntry)

context(CX.Scope) fun IrElement.locationXR(): XR.Location =
  this.location(currentFile.fileEntry).toLocationXR()

context(CX.Scope) fun IrElement.buildLocation(): CompilerMessageSourceLocation =
  this.location(currentFile.fileEntry)

context(CX.Scope) fun IrElement.buildLocationXR(): XR.Location =
  this.location(currentFile.fileEntry).toLocationXR()

fun CompilerMessageSourceLocation.toLocationXR(): XR.Location =
  XR.Location.File(path, line, column)

inline fun <reified T> IrExpression.isClassStrict(): Boolean {
  val className = T::class.classId()
  return className == this.type.classId()
}

inline fun <reified T> IrExpression.isClass(): Boolean {
  val className = T::class.classId()
  return className == this.type.classId() || type.superTypes().any { it.classId() == className }
}

inline fun <reified T> classIdOf(): ClassId? = T::class.classId()

//inline fun <reified T> fqNameOf(): FqName {
//  val className = T::class.qualifiedNameForce
//  return FqName(className)
//}

/**
 * Check annotations on the function of a call.
 * Note that this will not to up to a field-property symbol for example
 * if we have:
 * ```
 * @Foo val x @Bar get() = stuff
 * ```
 * When we call ownerHasAnnotation on the getter symbol, it will find the @Bar annotation but not @Foo
 * in order to get that we need to go up to `reciever.symbol.owner.correspondingPropertySymbol?.owner?.annotations`
 */
inline fun <reified T> IrCall.ownerHasAnnotation() =
  this.symbol.owner.hasAnnotation<T>()

fun IrCall.allOwnerAnnotations() =
  this.symbol.owner.annotations + (this.symbol.owner.correspondingPropertySymbol?.owner?.annotations ?: listOf())

fun IrCall.printAnnotationData() =
  """
    annotations: ${this.symbol.owner.annotations.map { it.dumpKotlinLike() }}
    property annotations: ${(this.symbol.owner.correspondingPropertySymbol?.owner?.annotations ?: listOf()).map { it.dumpKotlinLike() }}
    backing field annotations: ${(this.symbol.owner.correspondingPropertySymbol?.owner?.backingField?.annotations ?: listOf()).map { it.dumpKotlinLike() }}
  """.trimIndent()


inline fun <reified T> IrCall.someOwnerHasAnnotation() =
  this.symbol.owner.hasAnnotation<T>() || this.symbol.owner.correspondingPropertySymbol?.owner?.hasAnnotation<T>() ?: false

inline fun <reified T> IrElement.hasAnnotation() =
  when (this) {
    is IrAnnotationContainer ->
      this.annotations.any { it.type.isClassStrict<T>() }
    is IrSimpleFunction ->
      this.annotations.any { it.type.isClassStrict<T>() }
    else -> false
  }

inline fun IrElement.hasAnnotation(fqName: FqName) =
  when (this) {
    is IrAnnotationContainer ->
      this.annotations.any { it.type.classFqName == fqName }
    is IrSimpleFunction ->
      this.annotations.any { it.type.classFqName == fqName }
    else -> false
  }

inline fun <reified T> IrType.hasAnnotation() =
  this.annotations.any { it.type.isClassStrict<T>() }

// IrFile is both a IrSymbolOwner and a IrAnnotationContainer so
// have a override specifically for. Otherwise would need to use a @this for it
inline fun <reified T> IrFile.fileHasAnnotation() =
  this.annotations.any { it.type.isClassStrict<T>() }

inline fun <reified T> IrAnnotationContainer.getAnnotation() =
  this.let { it.annotations.find { ctor -> ctor.type.isClassStrict<T>() } }


//inline fun <reified T> IrAnnotationContainer.getAnnotation() =
//  annotations.find { it.type.isClassStrict<T>() }

// Cheaper that isClass because doesn't check subclasses
inline fun <reified T> IrType.isClassStrict(): Boolean {
  // NOTE memoize these things for performance?
  val className = T::class.classId()
  return className == this.classOrNull?.owner?.classId
}

// TODO use builderCtx.pluginCtx.referenceClass(classId) instead of this, this means pluginCtx needs to be passed into here
context(CX.Scope)
inline fun <reified T> IrType.isClass(): Boolean {
  val clsOpt = T::class.classId()?.let { pluginCtx.referenceClass(it) }
  return clsOpt?.let { cls: IrClassSymbol -> this.eraseTypeParameters().isSubtypeOfClass(cls) } ?: false
}

fun IrType.classId(): ClassId? = this.classOrNull?.owner?.classId
fun ClassId.toXR(): XR.ClassId = XR.ClassId(this.packageFqName.toXR(), this.relativeClassName.toXR())
fun FqName.toXR(): XR.FqName = XR.FqName(this.pathSegments().map { it.identifier })

inline fun <reified T> IrAnnotationContainer.getAnnotationArgs(): List<IrExpression> {
  val annotation = annotations.find { it.type.isClassStrict<T>() }
  return annotation?.regularArgs?.filterNotNull() ?: emptyList()
}

inline fun <reified T> IrCall.getPropertyAnnotationArgs(): List<IrExpression> {
  val annotation = (this.symbol.owner.correspondingPropertySymbol?.owner?.annotations ?: listOf()).find { it.type.isClassStrict<T>() }
  return annotation?.regularArgs?.filterNotNull() ?: emptyList()
}

context(CX.Scope) fun IrExpression.varargValues(): List<IrExpression> =
  (this as? IrVararg ?: run {
    logger.warn("[ExoQuery-WARN] Expected the argument to be an IrVararg but it was not: ${this.dumpKotlinLike()}"); null
    null
  })?.elements?.mapNotNull {
    it as? IrExpression ?: run {
      logger.warn("[ExoQuery-WARN] Expected the argument to be an IrExpression but it was not: ${it.dumpKotlinLike()}"); null
    }
  } ?: emptyList()

inline fun <reified T> IrCall.reciverIs() =
  (this.extensionArg ?: this.dispatchArg)?.isClass<T>() ?: false

inline fun <reified T> IrCall.reciverIs(methodName: String) =
  (this.extensionArg ?: this.dispatchArg)?.isClass<T>() ?: false && this.symbol.safeName == methodName

context(CX.Scope) val IrElement.loc get() = this.locationXR()

data class ReplacementMethodToCall(val methodToCall: String, val callerType: ChangeReciever = ChangeReciever.DoNothing) {
  companion object {
    fun from(call: IrConstructorCall) =
      call.regularArgs[0]?.let { firstArg ->
        if (firstArg is IrConst && firstArg.kind == IrConstKind.String) {
          val secondArg: ChangeReciever =
            call.regularArgs[1]?.let { secondArg ->
              secondArg.match(
                case(Ir.GetEnumValue[Is()]).then { it.safeName }
              )
            }?.let { secondArgValue ->
              ChangeReciever.valueOf(secondArgValue)
            }
              ?: ChangeReciever.DoNothing

          ReplacementMethodToCall(firstArg.value as String, secondArg)
        } else
          null
      }
  }
}


fun IrCall.caller() =
  this.extensionArg?.let {
    Caller.Extension(it)
  } ?: this.dispatchReceiver?.let {
    Caller.Dispatch(it)
  }

// Best-effort to get the source of the file
context(CX.Scope) fun IrElement.source(): String? = run {
  val range = TextRange(this.startOffset, this.endOffset)

  fun getFromFirSource() =
    (currentFile.metadata as? FirMetadataSource.File)
      ?.fir
      ?.source
      ?.getElementTextInContextForDebug()
      ?.let { range.substring(it) }

  fun getFromKtFile() =
    currentFile.getKtFile()?.let { ktFile ->
      ktFile.textRange.cutOut(range).let { cutOut ->
        ktFile.text.let { textValue ->
          cutOut.substring(textValue)
        }
      }
    }

  getFromFirSource() ?: getFromKtFile()
}

context(CX.Scope)
fun IrExpression.isSqlQuery() =
  this.type.isClass<SqlQuery<*>>()

context(CX.Scope)
fun IrExpression.isSqlExpression() =
  this.type.isClass<SqlExpression<*>>()

context(CX.Scope)
fun IrExpression.isSqlAction() =
  this.type.isClass<SqlAction<*, *>>()

/**
 * In situations where a SqlAction is created and executed in the same block errors can result
 * that seem to indicate that the .params call coming out of the initial expression is empty.
 * Still in certain situations it works but in others it does not. This was first descovered in TransactionSpec
 * where the following would not work:
 * ```
 * val joe = Person(1, "Joe", "Bloggs", 111)
 * "transaction support" - {
 *   "success" {
 *       capture { insert<Person> { setParams(joe) } }.build<PostgresDialect>()
 *    }
 * }
 * //> Compile Error:
 * //> java.lang.IllegalStateException: IrFieldSymbolImpl is unbound. Signature: null
 * ```
 * While the following would:
 * ```
 * "transaction support" - {
 *   "success" {
 *       val joe = Person(1, "Joe", "Bloggs", 111)
 *       capture { insert<Person> { setParams(joe) } }.build<PostgresDialect>()
 *    }
 * }
 * ```
 * It was then noted that the above consturct would trivially work if the capture block was writtein to a new variable
 * ```
 * "transaction support" - {
 *   "success" {
 *       val joe = Person(1, "Joe", "Bloggs", 111)
 *       val cap = capture { insert<Person> { setParams(joe) } }
 *       cap.build<PostgresDialect>()
 *    }
 * }
 * ```
 * This leads me to belive that specifically in the situation where the variable is not created,
 * the way the `capture { ... }` is munged around inside of the TransformCompileQuery, the original
 * expression becomes somehow invalid. Writing it to a variable seems to introduce some kind of stability
 * that allows it to be resused. As of yet I have not seen similar issues with constructs like `param`
 * but if they do arise, a similar solution (i.e. creating a run-function with a internal-variable
 * pointing to the original expression) can be applied.
 */
context(CX.Scope, CX.Builder)
fun makeRunFunction(statements: List<IrStatement>, returnExpr: IrExpression): IrExpression {
  val runFunction = pluginCtx.referenceFunctions(CallableId(FqName("kotlin"), Name.identifier("run")))
    .first { it.owner.regularParams.singleOrNull()?.type is IrSimpleType } // get the generic run<T> function

  val lambdaType = runFunction.owner.regularParams[0].type
  val returnType = runFunction.owner.returnType

  val decl = currentDeclarationParent ?: parseError("Cannot get parent of the current declaration", returnExpr)
  val lambda = createLambda0(returnExpr, decl, statements)

  return builder.irCall(runFunction).apply {
    typeArguments[0] = returnExpr.type // for example, T = String
    arguments[0] = lambda
  }
}

fun IrElement.hasSameOffsetsAs(other: IrElement): Boolean =
  this.startOffset == other.startOffset && this.endOffset == other.endOffset

context(CX.Scope)
inline fun <reified T> typeOfClass(): IrType? = run {
  val classId = typeOf<T>().fullPathOfBasic()
  pluginCtx.referenceClass(classId)?.owner?.defaultType
}
