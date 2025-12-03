package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.*
import io.exoquery.annotation.*
import io.exoquery.plugin.*
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.logging.Messages.ValueLookupComingFromExternalInExpression
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.containsBatchParam
import io.exoquery.plugin.transform.isBatchParam
import io.exoquery.xr.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


object ParseWindow {
  context(CX.Scope, CX.Parsing)
  fun parse(expr: IrExpression): XR.Window =
    on(expr).match(
      case(Ir.Call.FunctionMem1[Is(), Is("partitionBy"), Ir.Vararg[Is()]]).thenThis { head, (partitionExprs) ->
        val head = parse(head)
        val partitions = partitionExprs.map { ParseExpression.parse(it) }
        head.copy(partitionBy = partitions)
      },
      // Match the variadic one first since the others are expr and would need you to check the function-type otherwise
      case(Ir.Call.FunctionMem1[Is(), Is.of("sortBy", "orderBy"), Ir.Vararg[Is()]]).thenThis { head, (orderXbyY) ->
        val head = parse(head)
        val orders = orderXbyY.map { ParseOrder.parseOrdTuple(it) }.map { (expr, ord) -> XR.OrderField.By(expr, ord) }
        head.copy(orderBy = orders)
      },
      case(Ir.Call.FunctionMem1[Is(), Is.of("sortBy", "orderBy"), Is()]).thenThis { head, orderExpr ->
        val head = parse(head)
        val order = ParseExpression.parse(orderExpr)
        head.copy(orderBy = listOf(XR.OrderField.Implicit(order)))
      },
      case(Ir.Call.FunctionMem1[Is(), Is.of("sortByDescending", "orderByDescending"), Is()]).thenThis { head, orderExpr ->
        val head = parse(head)
        val order = ParseExpression.parse(orderExpr)
        head.copy(orderBy = listOf(XR.OrderField.Implicit(order)))
      },
      // This is the core of the window
      case(Ir.Call.FunctionMem0[Ir.Expr.ClassOf<CapturedBlock>(), Is("over")]).thenThis { _, _ ->
        XR.Window(listOf(), listOf(), XR.Ident.Unused("windowcore"), expr.loc)
      },
    ) ?: parseError("Could not parse Window from the expression", expr)
}

/**
 * Parses the tree and collets dynamic binds as it goes. The parser should be exposed
 * as stateless to client functions so everything should go through the `Parser` object instead of this.
 */
object ParseExpression {
  internal sealed interface Seg {
    data class Const(val value: String) : Seg {
      fun mergeWith(other: Const): Const = Const(value + other.value)
    }

    data class Expr(val expr: IrExpression) : Seg
    companion object {
      fun parse(expr: IrExpression): Seg =
        when {
          expr.isClassStrict<String>() && expr is IrConst && expr.kind == IrConstKind.String -> Const(expr.value as String)
          else -> Expr(expr)
        }
    }

    context(CX.Scope, CX.Parsing)
    fun constOrFail(): Const =
      when (this) {
        is Const -> this
        is Expr -> parseError("Expected a constant segment, but found an expression segment: Seg.Expr(${expr.dumpKotlinLike()})", this.expr)
      }

    context(CX.Scope, CX.Parsing)
    fun exprOrFail(): Expr =
      when (this) {
        is Const -> parseErrorAtCurrent("Expected an expression segment, but found a constant segment: Seg.Const(${value})")
        is Expr -> this
      }
  }

  context(CX.Scope, CX.Parsing) fun parseBlockStatement(expr: IrStatement): XR.Variable =
    on(expr).match(
      case(Ir.Variable[Is(), Is()]).thenThis { name, rhs ->
        val irType = TypeParser.of(this)
        val rhsXR = parse(rhs)
        XR.Variable(XR.Ident(name.sanitizeIdentName(), irType, rhs.locationXR()), rhsXR, expr.loc)
      }
    ) ?: parseError("Could not parse Ir Variable statement from:\n${expr.dumpSimple()}", expr)

  context(CX.Scope, CX.Parsing) fun parseBranch(expr: IrBranch): XR.Branch =
    on(expr).match(
      case(Ir.Branch[Is(), Is()]).then { cond, then ->
        XR.Branch(parse(cond), parse(then), expr.loc)
      }
    ) ?: parseError("Could not parse Branch from: ${expr.dumpSimple()}", expr)

  context(CX.Scope, CX.Parsing) fun parseFunctionBlockBody(blockBody: IrBlockBody): XR.Expression =
    blockBody.match(
      case(Ir.BlockBody.ReturnOnly[Is()]).then { irReturnValue ->
        parse(irReturnValue)
      },
      case(Ir.BlockBody.StatementsWithReturn[Is(), Is()]).then { stmts, ret ->
        val vars = stmts.map { parseBlockStatement(it) }
        val retExpr = parse(ret)
        XR.Block(vars, retExpr, blockBody.locationXR())
      }
    ) ?: parseError("Could not parse IrBlockBody:\n${blockBody.dumpKotlinLike()}", blockBody)

  context(CX.Scope, CX.Parsing) fun parse(expr: IrExpression): XR.Expression =
    on(expr).match<XR.Expression>(

      case(Ir.Call[Is()]).thenIf { it.ownerHasAnnotation<DslFunctionCall>() || it.ownerHasAnnotation<DslNestingIgnore>() }.then { call ->
        CallParser.parse(call).asExpr()
      },

      // Converter functions for string e.g. toInt, toLong, etc.
      case(Ir.Call.FunctionMem0[Ir.Expr.ClassOf<String>(), Is { it.isConverterFunction() }]).thenThis { head, method ->
        val isKotlinSynthetic = this.hasSameOffsetsAs(head)
        XR.MethodCall(parse(head), method, emptyList(), XR.CallType.PureFunction, CID.kotlin_String, isKotlinSynthetic, XRType.Value, expr.loc)
      },

      // Numeric conversion functions toInt, toString, etc... on numeric types Int, Long, etc...
      case(Ir.Call.FunctionMem0[Is(), Is { it.isConverterFunction() }])
        .thenIf { head, _ -> head.type.classId()?.toXR()?.isNumeric() ?: false }
        .thenThis { head, method ->
          // Typically a kotlin synthetic e.g. `.toDouble in `(x:Int) >= (x:Double)` which becomes `(x:Int).toDouble() >= (x:Double)`
          // will have the same offset as the caller, at least for things like numeric-conversion. Use that as a guide for whether it is synthetic or not
          val isKotlinSynthetic = this.hasSameOffsetsAs(head)
          val classId = head.type.classId() ?: parseError("Cannot determine the classId of the type ${this.type.dumpKotlinLike()} of this expression.", this)
          XR.MethodCall(parse(head), method, emptyList(), XR.CallType.PureFunction, classId.toXR(), isKotlinSynthetic, XRType.Value, expr.loc)
        },

      // TODO cannot assume all whitelisted methods are pure functions. Need to introduce purity/impurity to the whitelist
      case(Ir.Call.FunctionMemN[Is { it.type.classId()?.let { MethodWhitelist.allowedHost(it) } ?: false }, Is(), Is()])
        .thenIfThis { _, _ -> MethodWhitelist.allowedMethodForHost(this.type.classId(), funName, this.allArgsCount) }
        .thenThis { head, args ->
          val classId = head.type.classId() ?: parseError("Cannot determine the classId of the type ${this.type.dumpKotlinLike()} of this expression.", this)
          val argsXR = args.map { parse(it) }
          val isKotlinSynthetic = this.hasSameOffsetsAs(head)
          XR.MethodCall(parse(head), funName, argsXR, XR.CallType.PureFunction, classId.toXR(), isKotlinSynthetic, XRType.Value, expr.loc)
        },

      case(Ir.Expr.ClassOf<SqlQuery<*>>()).then { expr ->
        // Even dynamic variables will be handled by this so don't need to do anything for dynamic SqlQuery instances here.
        XR.QueryToExpr(ParseQuery.parse(expr), expr.loc)
      },

      case(ExtractorsDomain.CaseClassConstructorCall[Is()]).thenThis { data ->
        if (this.symbol.safeName == "SqlExpression" || this.symbol.safeName == "SqlQuery") parseError("Illegal state, processing ${this.symbol.safeName} as a case class constructor", expr)

        // Can get the type here if we want it but not needed since we get more precise type information from the applied arguments (e.g. if there are generics)
        //val tpe = TypeParser.of(expr).let { it as? XRType.Product ?: parseError("Data-Class constructor result was expected to be a XRType.Product (CC) but it was a ${it.shortString()}") }

        val (productName, fieldRenames) =
          on(expr.type).match(
            case(Ir.Type.DataClass[Is(), Is()]).then { name, props ->
              name.name to props.map { it.originalName to it.name }.toMap()
            }
          ) ?: parseError("The XR-Type of the intermediate constructor ${expr.dumpKotlinLike()} was not a data-class. It was a ${expr.type.dumpKotlinLike()}", expr)

        XR.Product(productName, data.fields.map { (name, valueOpt) ->
          (fieldRenames[name] ?: name) to (valueOpt?.let { parse(it) } ?: XR.Const.Null(expr.loc)) }, expr.loc
        )
      },

      // parse lambda in a sql block
      case(Ir.FunctionExpression.withBlock[Is(), Is()]).thenThis { params, blockBody ->
        XR.FunctionN(params.map { it.makeIdent() }, parseFunctionBlockBody(blockBody), expr.loc)
      },

      case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<Function<*>>(), Is("invoke"), Is()]).thenThis { hostFunction, args ->
        XR.FunctionApply(parse(hostFunction), args.map { parse(it) }, expr.loc)
      },

      case(Ir.Call.FunctionMemN[Is(), Is.of("paramRoom"), Is()]).thenThis { _, args ->
        val paramValue = (args.first() as? IrConst)?.let { it.value.toString() } ?: parseError("Expected a constant value for the paramRoom function, but found: ${args.first().dumpKotlinLike()}", args.first())
        XR.PlaceholderParam(
          paramValue,
          this.type.toClassIdXR(),
          TypeParser.of(this),
          expr.loc
        )
      },

      case(Ir.Call.FunctionMemN[Is(), Is.of("param", "paramCtx", "paramCustom"), Is()]).thenThis { _, args ->
        val paramValue = args.first()
        // Ignore human name coming from the param for now
        //val humanName = paramCallHumanName()

        val paramBindTypeRaw =
          when {
            this.ownerHasAnnotation<io.exoquery.annotation.ParamStatic>() -> {
              val staticRef = this.symbol.owner.getAnnotationArgs<io.exoquery.annotation.ParamStatic>().firstOrNull()?.let { param ->
                param as? IrClassReference
                  ?: parseError("ParamStatic annotation must have a single argument that is a class-reference (e.g. PureFunction::class)", this)
              } ?: parseError("Could not find ParamStatic annotation", this)
              val classId = staticRef.classType.classId() ?: parseError("Could not find classId for ParamStatic annotation", this)
              ParamBind.Type.ParamStatic(classId)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCustom>() -> {
              // serializer should be the 2nd arg i.e. paramCustom(value, serializer)
              ParamBind.Type.ParamCustom(args.lastOrNull() ?: parseError("ParamCustom annotation must have a second argument that is a class-reference (e.g. PureFunction::class)", this), paramValue.type)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCustomImplicit>() -> {
              ParamBind.Type.ParamCustomImplicit(paramValue.type)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCustomValue>() -> {
              ParamBind.Type.ParamCustomValue(paramValue)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCtx>() -> {
              ParamBind.Type.ParamCtx(paramValue.type)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamStaam>() -> {
              getSingleSerializerForLeafType(expr.type, ParseError.Origin.from(expr))
                ?: parseError(
                  Messages.usedParamWrongMessage(expr.type.dumpKotlinLike()),
                  expr
                )
            }
            else -> parseError("Could not find Param annotation on the param function of the call", this)
          }

        val bid = BID.new()
        val (varsUsed, callsUsed) = IrTraversals.collectGetValuesAndCalls(paramValue)
        callsUsed.forEach {
          if (it.ownerFunction.hasAnnotation<Dsl>())
            parseError("Cannot use an ExoQuery DSL function `${it.symbol.safeName}` inside of a param(...) call. The `param` construct is meant to bring external (i.e. runtime) varaibles into the sql expression", it)
        }

        varsUsed.forEach { varUsed ->
          validateCanUseInParam(varUsed)
        }

        val (paramBind, paramType) =
          if (batchAlias != null && varsUsed.any { it.isCurrentlyActiveBatchParam() }) {
            ParamBind.Type.ParamUsingBatchAlias(batchAlias, paramBindTypeRaw) to XR.ParamType.Batch
          } else {
            paramBindTypeRaw to XR.ParamType.Single
          }

        binds.addParam(bid, paramValue, paramBind)
        XR.TagForParam(bid, paramType, null, this.type.toClassIdXR(), TypeParser.of(this), paramValue.loc)
      },

      // x.let { stuff(...it...) } -> Apply(stuff(...it...), x)

      case(Ir.Call.FunctionMem1[Is(), Is("let"), Is()]).thenThis { head, lambda ->
        val reciever = parse(head)
        val lambda: XR.FunctionN = parse(lambda).let { it as? XR.FunctionN ?: parseError("Expected a lambda function to be parsed from the let call but was:\n${it.showRaw()}", lambda) }
        XR.FunctionApply(lambda, listOf(reciever), expr.loc)
      },

      case(Ir.CastingTypeOperator[Is(), Is()]).thenThis { target, newType ->
        val callType: XR.CallType = XR.CallType.PureFunction
        val targetProperty = parse(target)
        val thisType by lazy { TypeParser.of(this) }

        // If we're casting to the same class-id (e.g. frequently kotlin casts some type T? to T) then just ignore. Even if it's a SqlQuery<A> cast to SqlQuery<B> we still don't
        // care because the only reason we would ever want to know about a Kotlin cast is to do an SQL-cast and SQL-casts are only done on columns.
        if (target.type.classId() != null && target.type.classId() == newType.classId()) {
          targetProperty
        }
        // SQl does not have a notion of products (e.g. table-types) being something that is castable e.g. you can do `SELECT castToSomething(p).field FROM Person p`
        // That means that in any situation where kotlin casts a product type we need to cast the individual field instead of the entire product
        // Currently this is only happening in situations with ? operators e.g. something like this:
        //    classes: Name(first, last), Person(Name?, Int), Robot(String, String)
        //    sql.select {
        //      val p = from(Table<Person>())
        //      val r = join(Table<Robot>()) { r -> p.name?.first == r.ownerFirstName }
        //      p to r
        //    }
        // Parsers to an IR like this:
        //   select { val p = from(Table(Person)); val r = join(Table(Robot)) { { val tmp0_safe_receiver = p.name; if (tmp0_safe_receiver == null) null else kotlinCast_GC(tmp0_safe_receiver).first } == r.ownerFirstName }; Tuple(first = p, second = r) }
        // Instead of this:
        //   select { val p = from(Table(Person)); val r = join(Table(Robot)) { { val tmp0_safe_receiver = p.name; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.first } == r.ownerFirstName }; Tuple(first = p, second = r) }
        // That means we just need to remove the kotlin-cast from the tmp0_safe_receiver which is just a cast on a product type
        else if (targetProperty.type.isProduct())
          targetProperty
        // If we're casting a Boolean to a Boolean ignore the cast because it does not matter in SQL
        else if (thisType.isBooleanValue() && targetProperty.type.isBooleanValue())
          targetProperty
        else
          XR.GlobalCall(XR.FqName.Cast, listOf(targetProperty), callType, false, thisType, this.loc) //, TypeParser.of(this), loc)
      },

      // I.e. the nullableColumn!! or nullableRow!! operator, just ignore the !! part since all SQL expressions are trinary-value
      case(Ir.DenullingTypeOperator[Is()]).thenThis { target ->
        parse(target)
      },

      case(Ir.Call.FunctionMemN[Is(), Is.of("params", "paramsCtx", "paramsCustom"), Is()]).thenThis { _, args ->
        val paramValue = args.first()

        // TODO if a batch param is used then fail here since you cannot use batch queries with multi-params
        val paramBindType =
          when {
            // currently not used because the specific ones have been commented out. Waiting for @SignatureName in KMP
            this.ownerHasAnnotation<io.exoquery.annotation.ParamStatic>() -> {
              val staticRef = this.symbol.owner.getAnnotationArgs<io.exoquery.annotation.ParamStatic>().firstOrNull()?.let { param ->
                param as? IrClassReference
                  ?: parseError("ParamStatic annotation must have a single argument that is a class-reference (e.g. PureFunction::class)", this)
              } ?: parseError("Could not find ParamStatic annotation", this)
              val classId = staticRef.classType.classId() ?: parseError("Could not find classId for ParamStatic annotation", this)
              ParamBind.Type.ParamListStatic(classId)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamStaam>() -> {
              val irType = this.typeArguments.firstOrNull() ?: parseError("params-call must have a single type argument", this)
              getMultiSerializerForLeafType(irType, ParseError.Origin.from(expr))
                ?: parseError(
                  Messages.usedParamWrongMessage(irType.dumpKotlinLike()),
                  this
                )
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCustom>() -> {
              // serializer should be the 2nd arg i.e. paramCustom(value, serializer)
              val irType = this.typeArguments.firstOrNull() ?: parseError("params-call must have a single type argument", this)
              ParamBind.Type.ParamListCustom(args.lastOrNull() ?: parseError("ParamCustom annotation must have a second argument that is a class-reference (e.g. PureFunction::class)", this), irType)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCustomValue>() -> {
              ParamBind.Type.ParamListCustomValue(paramValue)
            }
            this.ownerHasAnnotation<io.exoquery.annotation.ParamCtx>() -> {
              val irType = this.typeArguments.firstOrNull() ?: parseError("params-call must have a single type argument", this)
              ParamBind.Type.ParamListCtx(irType)
            }
            else -> parseError("Could not find Param annotation on the params function of the call", this)
          }

        val varsUsed = IrTraversals.collectGetValue(paramValue)
        varsUsed.forEach { varUsed ->
          validateCanUseInParam(varUsed)
        }

        val bid = BID.Companion.new()
        binds.addParam(bid, paramValue, paramBindType)
        XR.TagForParam(bid, XR.ParamType.Multi, null, this.type.toClassIdXR(), TypeParser.ofFirstArgOfReturnTypeOf(this), paramValue.loc)
      },

      case(Ir.Call.FunctionMem1[Ir.Expr.ClassOf<Params<*>>(), Is("contains"), Is()]).thenThis { head, params ->
        val cid = head.type.classId()?.toXR() ?: parseError("Could not find classId for the head of the contains call", head)
        XR.MethodCall(parse(head), "contains", listOf(parse(params)), XR.CallType.PureFunction, cid, false, XRType.Value, expr.loc)
      },

      case(Ir.Call.FunctionMem1[Ir.Expr.ClassOf<SqlQuery<*>>(), Is("contains"), Is()]).thenThis { head, params ->
        val cid = head.type.classId()?.toXR() ?: parseError("Could not find classId for the head of the contains call", head)
        XR.MethodCall(parse(head), "contains", listOf(parse(params)), XR.CallType.PureFunction, cid, false, XRType.Value, expr.loc)
      },

      // Unlike a ParseFree, this is just a plain string-interpolation that lives inside the expression block (i.e. it's not preceeded by a "free" block)
      case(Ir.StringConcatenation[Is()]).then { components ->
        val componentExprs = components.map { ParseExpression.parse(it) }
        // Not sure if it's possible for components to be empty but cover that case with an empty string
        if (componentExprs.size == 0) XR.Const.String("", expr.loc)
        else if (componentExprs.size == 1) componentExprs.first()
        else componentExprs.reduce { a, b -> a _StrPlus_ b }
      },

      case(ParseFree.match()).thenThis { (components), _ ->
        ParseFree.parse(expr, components, funName)
      },

      case(Ir.Call.FunctionMem0[Is(), Is("value")]).thenIf { useExpr, _ -> useExpr.type.isClass<SqlQuery<*>>() }.then { sqlQueryIr, _ ->
        XR.QueryToExpr(ParseQuery.parse(sqlQueryIr), sqlQueryIr.loc)
      },

      case(ExtractorsDomain.Call.UseExpression.Receiver[Ir.Call.FunctionUntethered2[Is(PT.io_exoquery_util_scaffoldCapFunctionQuery), Is(), Is()]]).thenThis { (sqlExprArg, irVararg) ->
        processScaffolded(sqlExprArg, irVararg, expr)
      },

      // In certain odd situations (e.g. using a `@CatpuredFunction fun foo(p: Person) = sql.expression { flatJoin(Table<Address>, ...) }` inside of a other query
      // like so sql.select { val p = from(Person); val a = from(joinAddress(...)) }. We can have a scaffold without a proceeding use-function
      // need to handle that case.
      case(Ir.Call.FunctionUntethered2[Is(PT.io_exoquery_util_scaffoldCapFunctionQuery), Is(), Is()]).thenThis { sqlExprArg, irVararg ->
        processScaffolded(sqlExprArg, irVararg, expr)
      },

      // TODO check that the extension reciever is Ir.Expr.ClassOf<SqlExpression<*>> (and the dispatch method is CapturedBlock)
      // TODO make this into an annotated function similar to Param and move the matching into ExtractorsDomain
      case(ExtractorsDomain.Call.UseExpression.Receiver[Is()]).thenIf { useExpr -> useExpr.type.isClass<SqlExpression<*>>() }.then { sqlExprIr ->
        sqlExprIr.match(
          case(SqlExpressionExpr.Uprootable[Is()]).then { uprootable ->
            // Add all binds from the found SqlExpression instance, this will be truned into something like `currLifts + SqlExpression.lifts` late
            binds.addInheritedParams(sqlExprIr)
            // Then unpack and return the XR
            uprootable.unpackOrErrorXR().successOrParseError(sqlExprIr)
          },
          case(ExtractorsDomain.DynamicExprCall[Is()]).then { call ->
            if (call is IrCall && call.ownerHasAnnotation<SqlDynamic>())
              call.regularArgsOrExtension.forEach { validateDynamicArg(it) }
            val bid = BID.Companion.new()
            binds.addRuntime(bid, sqlExprIr)
            XR.TagForSqlExpression(bid, TypeParser.of(sqlExprIr), sqlExprIr.loc)
          },
        ) ?: parseError(Messages.CannotCallUseOnAnArbitraryDynamic(), sqlExprIr)
      },

      case(ExtractorsDomain.Call.`x compareableOp y`[Is()]).thenThis { opCall ->
        val (xExpr, op, yExpr) = opCall
        val x = parse(xExpr)
        val y = parse(yExpr)
        val output = XR.BinaryOp(x, op, y, expr.loc)
        ensureIsValidOp(expr, xExpr, yExpr, x, y, output)
        output
      },

      // Binary Operators
      case(ExtractorsDomain.Call.`x op y`[Is()]).thenThis { opCall ->
        val (xExpr, op, yExpr) = opCall
        val x = parse(xExpr)
        val y = parse(yExpr)
        val output = XR.BinaryOp(x, op, y, expr.loc)
        ensureIsValidOp(expr, xExpr, yExpr, x, y, output)
        output
      },

      // This is the thing that needs to have @WindowFun annotation
      case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<WindowDsl>(), Is(), Is()])
        .thenIfThis { _, _ -> ownerHasAnnotation<io.exoquery.annotation.WindowFun>() }
        .thenThis { windowFunctionDslExpr, windowArgsExprs ->

        val windowArgs = windowArgsExprs.map { ParseExpression.parse(it) }

        val windowFunctionName = this.symbol.owner.getAnnotationArgs<WindowFun>().firstOrNull()?.let { param ->
          (param as? IrConst)?.value as? String ?: parseError("Bad window function argument (${(param as? IrConst)?.value?.let { "$it:${it::class}" }})", this)
        } ?: parseError("WindowFun annotation did not have a string-argument", this)

        val call =
          if (windowFunctionName != "CUSTOM")
            XR.GlobalCall(
              XR.FqName(windowFunctionName),
              windowArgs,
              XR.CallType.PureFunction,
              false,
              TypeParser.of(this),
              this.locationXR()
            )
          else
            windowArgs.first()

        // Parse the entire window function call down to the window() part and return it here
        val window = ParseWindow.parse(windowFunctionDslExpr)
        // Set the `over` component since that is what is coming from the over() call which then returns
        // control back to the regular expression DSL.
        window.copy(over = call)
      },

      // Unary Operators
      case(ExtractorsDomain.Call.`(op)x`[Is()]).thenThis { opCall ->
        val (x, op) = opCall
        XR.UnaryOp(op, parse(x), expr.loc)
      },

      case(ExtractorsDomain.Call.`x to y`[Is(), Is()]).thenThis { x, y ->
        XR.Product.Tuple(parse(x), parse(y), expr.loc)
      },

      // Other situations where you might have an identifier which is not an SqlVar e.g. with variable bindings in a Block (inside an expression)
      case(Ir.GetValue.Symbol[Is()]).thenIfThis { this.isCapturedVariable() }.thenThis { sym ->
        if (this.isBatchParam()) parseError(Messages.batchParamError(), expr)
        XR.Ident(sym.sanitizedSymbolName(), TypeParser.of(this), this.locationXR()) // this.symbol.owner.type
      },
      case(Ir.Const[Is()]).thenThis {
        parseConst(this)
      },

      // TODO need to check for @SerialName("name_override") annotations from the Kotlin seriazation API and override the name
      //      (the parser also needs to be able to generated these based on a mapping)
      case(Ir.Call.Property[Is(), Is()]).thenThis { expr, propKind ->
        // If a batch-alias is being dereferenced should we potentially search inside of it? Might have performance implications
        if (expr.containsBatchParam()) parseError(Messages.batchParamError(), this)

        val core = parse(expr)
        when (propKind) {
          is Ir.Call.Property.PropertyKind.Named ->
            XR.Property(core, propKind.name, XR.HasRename.hasOrNot(propKind.isRenamed), XR.Visibility.Visible, expr.loc)
          is Ir.Call.Property.PropertyKind.Component -> {
            (core.type as? XRType.Product)?.let { productType ->
              val field = productType.fields[propKind.index]?.first
                ?: parseError("Could not find field at index ${propKind.index} in product type ${productType.name}. The fields were: ${productType.fields.map { (fieldName, _) -> fieldName }.withIndex()}", expr)
              XR.Property(core, field, XR.HasRename.NotHas, XR.Visibility.Visible, expr.loc)
            } ?: parseError("Component property can only be used on a product type but the IRType of the expression was: ${core.type}.\nThe expression was parsed as:\n${core.showRaw(false)}", expr)
          }
        }
      },

      // case(Ir.Call.Function[Is()]).thenIf { (list) -> list.size == 2 }.thenThis { list ->
      //   val a = list.get(0)
      //   val b = list.get(1)
      //   // TODO need to check that's its actually a binary operator!!!
      //   XR.BinaryOp(parse(a), parseSymbol(this.symbol), parse(b))
      // }
      // ,
      case(Ir.Block[Is(), Is()]).thenThis { stmts, ret ->
        val tpe = TypeParser.of(this)
        if (this.origin == IrStatementOrigin.ELVIS && tpe.isProduct()) {
          parseError("Elvis operator cannot be called on a composite type (i.e. rows-types and anything representing a group of columns) because this cannot be done in SQL", this)
        }
        XR.Block(stmts.map { parseBlockStatement(it) }, parse(ret), expr.loc)
      },
      case(Ir.When[Is()]).thenThis { cases ->
        val elseBranch = cases.find { it is IrElseBranch }?.let { parseBranch(it) }
        val casesAst = cases.filterNot { it is IrElseBranch }.map { parseBranch(it) }
        val allReturnsAreBoolean = cases.all { it.result.type.isClass<Boolean>() }
        // Kotlin converts (A && B) to `if(A) B else false`. This undoes that
        if (
          allReturnsAreBoolean &&
          elseBranch != null && casesAst.size == 1
          && casesAst.first().then.type is XRType.Boolean
          // Implicitly the else-clause in this case cannot have additional conditions
          && elseBranch.cond == XR.Const.Boolean(true) && elseBranch.then == XR.Const.Boolean(false)
        ) {
          val firstClause = casesAst.first()
          firstClause.cond _And_ firstClause.then
        }
        // Kotlin converts (A || B) to `if(A) true else B`. This undoes that
        else if (
          allReturnsAreBoolean &&
          elseBranch != null && casesAst.size == 1
          && casesAst.first().then == XR.Const.Boolean(true)
          // Implicitly the else-clause in this case cannot have additional conditions
          && elseBranch.cond == XR.Const.Boolean(true)
        ) {
          val firstClause = casesAst.first()
          firstClause.cond _Or_ elseBranch.then
        } else {
          val elseBranchOrLast = elseBranch ?: casesAst.lastOrNull() ?: parseError("Empty when expression not allowed:\n${this.dumpKotlinLike()}", expr)
          XR.When(casesAst, elseBranchOrLast.then, expr.loc)
        }
      },
    ) ?: run {
      val additionalHelp =
        when {
          // all arguments to the call must be valid dynamic arguments (i.e. constants, captured queries, captured expressions)
          expr is IrGetValue && expr.isExternal() -> ValueLookupComingFromExternalInExpression(expr)
          expr is IrCall && expr.isExternal() && expr.symbol.owner is IrSimpleFunction ->
            """|It looks like you are attempting to call the external function `${expr.symbol.safeName}` in a captured block
               |only functions specifically made to be interpreted by the ExoQuery system are allowed inside
               |of captured blocks. If you are trying to use a runtime-value of a primitive, you need to bring
               |it into the captured block by using `param(myCall(...))`. 
               |
               |For example:
               |fun myCall(value: String): Int = runtimeComputation(value)
               |val myQuery = sql { Table<Person>().filter { p -> p.id == param(myCall("someValue")) }
               |
               |If this function is supposed to do something that becomes part of the generated SQL you need to 
               |annotate it as @SqlFragment and make it return an SqlExpression<T> (or SqlQuery<T>) value. 
               |The use the use the `use` function to splice the value.
               |
               |For example:
               |@SqlFragment
               |fun myCall(value: String): SqlExpression<String> = sql.expression { value + "_suffix" }
               |val myQuery = sql { Table<Person>().filter { p -> p.name == myCall("someValue").use }
            """.trimMargin()

          else -> ""
        }

      parseError("Could not parse the expression" + (if (additionalHelp.isNotEmpty()) "\n${additionalHelp}" else ""), expr)
    }

  context(CX.Scope, CX.Parsing)
  fun processScaffolded(sqlExprArg: IrExpression, irVararg: IrExpression, currentExpr: IrExpression) = run {
    val loc = currentExpr.loc
    //if (this.dumpKotlinLikePretty().contains("Table(Address).join { a -> p.id == a.ownerId }.toExpr")) {
    //  throw IllegalArgumentException("--------------------- HERE (${sqlExprArg.dumpKotlinLikePretty()}) --------------------")
    //}

    val wrappedExprCall =
      sqlExprArg.match(
        // It is possible to sql a SqlQuery<*> value inside an sql.expression. Handle that case.
        // The actual type of the expression in this case will be SqlExpression<SqlQuery<T>> so that's what we need to check for
        case(Ir.Expr.ClassOf<SqlExpression<*>>()).thenIf { sqlExprArg.type.simpleTypeArgs.firstOrNull()?.isClass<SqlQuery<*>>() ?: false }.then {
          ParseQuery.parse(sqlExprArg)
        },
        case(SqlExpressionExpr.Uprootable[Is()]).then { uprootable ->
          // Add all binds from the found SqlExpression instance, this will be truned into something like `currLifts + SqlExpression.lifts` late
          binds.addInheritedParams(sqlExprArg)
          // Then unpack and return the XR
          uprootable.unpackOrErrorXR().successOrParseError(sqlExprArg)
        },
        // TODO need to explore in which situation this happens
        case(ExtractorsDomain.DynamicExprCall[Is()]).then { call ->
          val bid = BID.Companion.new()
          binds.addRuntime(bid, sqlExprArg)
          XR.TagForSqlExpression(bid, TypeParser.of(sqlExprArg), sqlExprArg.loc)
        },
      ) ?: parseError(Messages.CannotCallUseOnAnArbitraryDynamic(), sqlExprArg)
    val args = irVararg.varargValues()
    val parsedArgs = args.map { arg -> arg?.let { Parser.parseArg(it) } ?: XR.Const.Null(loc) }
    XR.FunctionApply(wrappedExprCall, parsedArgs, loc)
  }

  context(CX.Scope) fun parseConst(irConst: IrConst): XR.Expression =
    if (irConst.value == null) XR.Const.Null(irConst.loc)
    else when (irConst.kind) {
      IrConstKind.Null -> XR.Const.Null(irConst.loc)
      IrConstKind.Boolean -> XR.Const.Boolean(irConst.value as Boolean, irConst.loc)
      IrConstKind.Char -> XR.Const.Char(irConst.value as Char, irConst.loc)
      IrConstKind.Byte -> XR.Const.Byte(irConst.value as Int, irConst.loc)
      IrConstKind.Short -> XR.Const.Short(irConst.value as Short, irConst.loc)
      IrConstKind.Int -> XR.Const.Int(irConst.value as Int, irConst.loc)
      IrConstKind.Long -> XR.Const.Long(irConst.value as Long, irConst.loc)
      IrConstKind.String -> XR.Const.String(irConst.value as String, irConst.loc)
      IrConstKind.Float -> XR.Const.Float(irConst.value as Float, irConst.loc)
      IrConstKind.Double -> XR.Const.Double(irConst.value as Double, irConst.loc)
      else -> parseError("Unknown IrConstKind: ${irConst.kind}", irConst)
    }
}

context(CX.Scope)
fun validateDynamicArg(arg: IrExpression?) {
  if (arg == null) parseErrorAtCurrent("Argument of a @CapturedDynamic function cannot be null (i.e. no default values allowed)")
  if (arg.isClass<SqlExpression<*>>() || arg.isClass<SqlQuery<*>>()) {
    Unit
  } else {
    parseError(Messages.InvalidCapturedDynamicArgument(arg), arg)
  }
}

context(CX.Scope, CX.Parsing)
fun validateCanUseInParam(varUsed: IrGetValue) {
  fun throwValidationError(errorReason: String): Nothing =
    parseError(
      """${errorReason} 
         |The `param` function is only used to bring external variables into the sql (i.e. runtime-variables that are defined outside of it). 
         |If you want to use the `${varUsed.symbol.safeName}` symbol inside this captured block, you should be able to use it directly.
         |""".trimMargin(),
      varUsed
    )

  when (val realOwner = varUsed.realOwner()) {
    // Local variables of captured-dynamic functions can be used inside of param(...) since they are runtime values
    // technically the variables of a captured-dynamic function could be as well (in some circumstances) but this would
    // create a massive footgun so disallow it.
    is RealOwner.CapturedDynamicFunctionVariable if !varUsed.isAllowedInParam() && realOwner.varType == RealOwner.VarType.ParamVar ->
      throwValidationError("Cannot use the variable `${varUsed.symbol.safeName}` inside of a param(...) function because it is an argument of the dynamic-function `${realOwner.ownerFunction.symbol.safeName}`.")
    is RealOwner.CapturedFunctionVariable if !varUsed.isAllowedInParam()  && realOwner.varType == RealOwner.VarType.ParamVar ->
      throwValidationError("Cannot use the variable `${varUsed.symbol.safeName}` inside of a param(...) function because it is an argument of the captured-function `${realOwner.ownerFunction.symbol.safeName}`.")
    is RealOwner.CapturedFunctionVariable if !varUsed.isAllowedInParam()  && realOwner.varType == RealOwner.VarType.LocalVar ->
      throwValidationError("Cannot use the variable `${varUsed.symbol.safeName}` inside of a param(...) function because it is a varaible declared inside of the captured-function `${realOwner.ownerFunction.symbol.safeName}`.")
    is RealOwner.CapturedBlock if !varUsed.isAllowedInParam() ->
      throwValidationError("Cannot use the variable `${varUsed.symbol.safeName}` inside of a param(...) function because it originates inside of the sql-block.")
    else ->
      null // the InvalidReason.None case is covered here (and batch params)
  }
}

context(CX.Scope, CX.Parsing)
private fun IrGetValue.isAllowedInParam() =
  this.isCurrentlyActiveBatchParam() || this.symbol.safeName == "<this>"


context(CX.Scope, CX.Parsing)
private fun IrGetValue.isCurrentlyActiveBatchParam() =
  batchAlias != null && this.isBatchParam()
