package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.SqlExpression
import io.exoquery.SqlQuery
import io.exoquery.annotation.CapturedFunction
import io.exoquery.annotation.CapturedFunctionParamKinds
import io.exoquery.fansi.nullableAsList
import io.exoquery.parseError
import io.exoquery.plugin.extensionParam
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.isClass
import io.exoquery.plugin.location
import io.exoquery.plugin.locationXR
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.regularParams
import io.exoquery.plugin.safeName
import io.exoquery.plugin.source
import io.exoquery.plugin.sourceOrDump
import io.exoquery.plugin.trees.DynamicsAccum
import io.exoquery.plugin.trees.ExtractorsDomain.Call
import io.exoquery.plugin.trees.Lifter
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.SqlExpressionExpr
import io.exoquery.plugin.trees.SqlQueryExpr
import io.exoquery.plugin.trees.TypeParser
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.js.parser.parse


/**
 * A captured function in the form of:
 * ```
 * @CapturedFunction
 * fun nameIsX(people: SqlQuery<Person>, name: String): SqlQuery<Person> = capture { people.filter { p -> p.name == name } }
 *
 * // Is supposed to be transformed into a capture-instance with a lambda that looks like this:
 * fun nameIsX() =
 *   SqlQuery(XR.Function(args = Id(people), Id(name), body = XR.Filter(Id(people), BinaryOp(Prop(Id(p), "name"), "==", Id(name))))
 * ```
 * So effectively we take the parameters of the function and roll them up under a lambda that is then transformed into a XR instance.
 *
 *
 * Note however that if there are any params in this function they need to interpreted normally and the be passed in the SqlQuery instance
 * so that later in the TransformProjectCapture stage they can undergo the correct transformation.
 * For example say
 * ```
 * val name = ...
 * fun nameIsParam(people: SqlQuery<Person>): SqlQuery<Person> = capture { people.filter { p -> p.name == param(name) } }
 *
 * // This needs to be transformed into:
 * fun nameIsParam() =
 *   SqlQuery(XR.Function(
 *     args = Id(people), body = XR.Filter(Id(people), BinaryOp(Prop(Id(p), "name"), "==", Param(Tag(123))),
 *     params = listOf(Param(Tag(123), Id(name)))
 *   )
 * ```
 * The later in the TransformProjectCapture stage the caller of this function that looks roughly like this:
 * ```
 * nameIsParam(Table<Person>()).map { ...param(something)... }
 * // Needs to be transformed into the following (note that we drop the parameters being used in the call!)
 *
 * SqlQuery(XR.FunctionApply(nameIsParam(), listOf(Table<Person>()) ), nameIsParam().params ++ this.params /*i.e. param(something) */) ->
 *   SqlQuery(XR.FunctionApply(SqlQuery(XR.Function(args = Id(people), body = XR.Filter(Id(people), BinaryOp(Prop(Id(p), "name"), "==", Param(Tag(123))), params = nameIsParam().params ++ this.params)
 * ```
 *
 * If there are any runtimes in this function the function still needs to be captured in the fasion above but the runtimes need to be maintained.
 * In for both of the above instances, params and runtimes that can be accessed by the symbol of the function nameIsX may not necessairly
 * be accessible to later callers of the function (e.g. if nameIsX is a member-function which has access to internal parameters of a class
 * that the caller does not. So we cannot just grab the contents from the body of the function by callers. The symbol
 * of the function itself needs to be called in order to get params and runtimes. This is why we remove the arguments but still make the function
 * accessible.
 */
class TransformAnnotatedFunction(val superTransformer: VisitTransformExpressions) : ElementTransformer<IrFunction>() {

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun matches(expr: IrFunction): Boolean =
    expr is IrSimpleFunction && expr.hasAnnotation<CapturedFunction>() &&
        // If the function has a CapturedFunctionParamKinds annotation it has already been transformed
        !expr.hasAnnotation<CapturedFunctionParamKinds>()

  fun IrSimpleFunction.getSingleReturnExpr(): IrExpression? =
    body?.statements?.singleOrNull().let { it as? IrReturn }?.value

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun transform(capFunRaw: IrFunction): IrFunction {
    val capFun = capFunRaw as? IrSimpleFunction ?: parseError("The function annotated with @CapturedFunction must be a simple function.", capFunRaw)
    val recieiverParam: XR.Ident? =
      capFun.extensionParam?.let { recieiverExpr ->
        val tpe = TypeParser.of(recieiverExpr)
        XR.Ident("this", tpe)
      }

    if (capFun.regularParams.isEmpty() && capFun.extensionParam == null)
      parseError(
        """
          The function annotated with @CapturedFunction must have at least one parameter but none were found (and a extension-receiver parameter -that is treated as an argument- was not found either). In this case it is not necessary to mark the function with the @CapturedFunction annotation. Remove it and treat the function as a static query splice.
          -------------------- The internal representation of the function was: --------------------
          ${capFun.dumpKotlinLike()}          
        """.trimIndent(),
        capFun.location()
      )

    val errorText = "A function annotated with @CapturedFunction must return a single, single SqlQuery<T> or SqlExpression<T> instance"

    if (!(capFun.returnType.isClass<SqlQuery<*>>() || capFun.returnType.isClass<SqlExpression<*>>())) {
      parseError("The Captured Function had the wrong kind of return type. ${errorText}", capFun)
    }
    if (capFun.body == null) {
      parseError("The function body of a @CapturedFunction cannot be empty", capFun)
    }
    val funBody = capFun.body!!

    val originalRegularParams = capFun.regularParams

    val capFunReturn =
      capFun.getSingleReturnExpr() ?: parseError(Messages.CapturedFunctionFormWrong("Outer form of the captured-function was wrong."), capFun)

    // Add the function value parameters to the parsing context so that the parser treats them as identifiers (instead of dynamics)
    val newSqlContainer =
      with(CX.Symbology(symbolSet.withCapturedFunctionParameters(capFun))) {
        // If we've got some function like:
        // @CapturedFunction fun nameIsJoe(people: SqlQuery<Person>) = capture { people.filter { p -> p.name == name } }
        // we need to parse the body of the function and transform it into a XR.Function. On the site of a use-call
        // e.g. `nameIsJoe(Table<Person>())` we need to transform the use-site a XR.FunctionApply(..., args)
        // eventually during beta-reduction we'll get we'll get XR.FunctionApply(XR.Function(...), args) which is the XR representation of the function
        // and we'll apply the function during the SqlNormalization phase.
        on(capFunReturn).match(
          // It can either be a `select { ... }` or a `capture { ... }`
          case(Call.CaptureQuery[Is()]).thenThis {
            val (rawQueryXR, dynamics) = TransformCapturedQuery.parseCapturedQuery(it, superTransformer)
            processQuery(recieiverParam, rawQueryXR, dynamics, originalRegularParams, capFun.locationXR(), capFunReturn, capFunRaw)
          },
          case(Call.CaptureSelect[Is()]).thenThis {
            val (rawQueryXR, dynamics) = TransformSelectClause.parseSelectClause(it, superTransformer)
            processQuery(recieiverParam, rawQueryXR, dynamics, originalRegularParams, capFun.locationXR(), capFunReturn, capFunRaw)
          },
          case(Call.CaptureExpression[Is()]).thenThis {
            val (rawQueryXR, dynamics) = TransformCapturedExpression.parseSqlExpression(it, superTransformer)
            processExpression(recieiverParam, rawQueryXR, dynamics, originalRegularParams, capFun.locationXR(), capFunReturn, capFunRaw)
          }
        ) ?: parseError(Messages.CapturedFunctionFormWrong("Invalid capture-function body. ${errorText}"), capFunReturn)
      }

    // Create a helper annotation so we know what the original Param-Kinds of the function so that later in
    // the TransformScaffoldAnnotatedFunctionCall we can reconstruct what the arguments of the function were
    // in order to know how to interpret the parameters.
    val originalParamKindsAnnotation = Lifter(this@Builder).makeCapturedFunctionParamKinds(capFun.symbol.owner.parameters)
    capFun.annotations = capFun.annotations + listOf(originalParamKindsAnnotation)

    // The function should not have any parameters since they will be captured in the XR
    // i.e. they will be arguments of the XR.FunctionApply when the scaffold is parsed
    // the extension reciever gets dropped and used like a variable
    // Only the the dispatch-reciever to a annotated function remains so if it is there put it in,
    // otherwise put in an empty list
    capFun.parameters =
      capFun.dispatchReceiverParameter?.let { listOf(it) } ?: emptyList()

    //capFun.typeParameters = emptyList()

    capFun.body = builder.irBlockBody {
      val ret = irReturn(newSqlContainer)
      +ret
    }
    //error("---------- New CapFun: ----------\n${capFun.dumpKotlinLike()}")

    return capFun
  }

  context(CX.Scope, CX.Builder, CX.Symbology)
  private fun processQuery(recieverParam: XR.Ident?, rawQueryXR: XR, dynamics: DynamicsAccum, originalParams: List<IrValueParameter>, capFunLocation: XR.Location, capFunReturn: IrExpression, originalCall: IrFunction) = run {
    val xrLambdaParams = recieverParam.nullableAsList() + originalParams.map { Parser.scoped { Parser.parseValueParamter(it) } }
    val params = dynamics.makeParams()
    val queryXR = rawQueryXR as? XR.Query ?: parseError("The body @CapturedFunction must be a capture returning a SqlQuery<T> or SqlExpression<T> instance but it was: ${rawQueryXR.showRaw()}", capFunReturn)
    val xrLambda = XR.FunctionN(params = xrLambdaParams, body = queryXR, loc = capFunLocation)
    if (dynamics.noRuntimes()) {
      SqlQueryExpr.Uprootable.plantNewUprootable(xrLambda.asQuery(), params)
    } else {
      val runtimes = dynamics.getAllRuntimesCollect()
      parseError("""
      The query-based @CapturedFunction `${originalCall.symbol.safeName}` had (${runtimes.size}) parameters that could not be captured at compile-time. They are the following:
      ${runtimes.withIndex().map { (i, it) -> "$i) ${it.sourceOrDump()}" }.joinToString("\n")}
      """.trimIndent())
    }
  }

  context(CX.Scope, CX.Builder, CX.Symbology)
  private fun processExpression(recieverParam: XR.Ident?, rawQueryXR: XR, dynamics: DynamicsAccum, originalParams: List<IrValueParameter>, capFunLocation: XR.Location, capFunReturn: IrExpression, originalCall: IrFunction) = run {
    val xrLambdaParams = recieverParam.nullableAsList() + originalParams.map { Parser.scoped { Parser.parseValueParamter(it) } }
    val params = dynamics.makeParams()
    val queryXR = rawQueryXR as? XR.Expression ?: parseError("The return value of a @CapturedFunction must be an Expression ADT instance but it was: ${rawQueryXR.showRaw()}", capFunReturn)
    val xrLambda = XR.FunctionN(params = xrLambdaParams, body = queryXR, loc = capFunLocation)
    if (dynamics.noRuntimes()) {
      SqlExpressionExpr.Uprootable.plantNewUprootable(xrLambda.asExpr(), params)
    } else {
      val runtimes = dynamics.getAllRuntimesCollect()
      parseError("""
      The expression-based @CapturedFunction `${originalCall.symbol.safeName}` had (${runtimes.size}) parameters that could not be captured at compile-time. They are the following:
      ${runtimes.withIndex().map { (i, it) -> "$i) ${it.sourceOrDump()}" }.joinToString("\n")}
      """.trimIndent())
    }
  }

  // Diagnostic error for debugging uproot failure of a particular clause
  fun failOwnerUproot(sym: IrSymbol, output: IrExpression): IrExpression {
    error(
      """|----------- Could not uproot the expression: -----------
         |${sym.owner.dumpKotlinLike()}
         |--- With the following IR: ----
         |${sym.owner.dumpSimple()}""".trimMargin()
    )
    return output
  }
}
