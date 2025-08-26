package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.*
import io.exoquery.plugin.classId
import io.exoquery.plugin.classIdOf
import io.exoquery.plugin.regularArgs
import io.exoquery.plugin.transform.*
import io.exoquery.xr.EncodingXR
import io.exoquery.xr.XR
import io.exoquery.xr.encode
import io.exoquery.xr.of
import kotlinx.serialization.decodeFromHexString
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullablePrimitiveType
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class RuntimesExpr(val runtimes: List<Pair<BID, IrExpression>>, val runtimesToCompose: List<IrExpression>) {
  context (CX.Scope, CX.Builder) fun lift(): IrExpression {
    return with(makeLifter()) {
      val bindsList = runtimes.map { pair ->
        pair.lift(
          { bid -> bid.lift() },
          { it })
      }
      val newRuntimes: IrExpression = make<RuntimeSet>(bindsList.liftExpr<Pair<BID, IrExpression>>())
      runtimesToCompose
        // First take the .runtimes property from each SqlExpression instance
        .map { it.callDispatch("runtimes")() }
        // Then compose them them together with the new lifts
        .fold(newRuntimes, { acc, nextRuntimes ->
          acc.callDispatch("plus")(nextRuntimes)
        })
    }
  }
}

/** Represents the data coming from the `param` function in the DSL on the backend */
data class ParamBind(val bid: BID, val value: IrExpression, val paramSerializer: ParamBind.Type) {
  /** Constructors corresponding to needed ingredients of the annotations:
   * ```
   * annotation class ParamStatic(val type: KClass<out ParamSerializer<out Any>>)
   * annotation class ParamCtx
   * annotation class ParamCustom
   * annotation class ParamCustomValue
   * ```
   */
  sealed interface Type {
    sealed interface Single : Type {
      context (CX.Scope, CX.Builder) fun makeSerializer(): IrExpression
      context (CX.Scope, CX.Builder) fun makeValue(originalValue: IrExpression): IrExpression = originalValue
    }

    companion object {
      context(CX.Scope)
      fun auto(expr: IrExpression) = run {
        // Both isNullablePrimitiveType() and isPrimitiveType() are not needed (the 1st one takes care of both) but doing this for clarity
        if (expr.type.isNullablePrimitiveType() || expr.type.isPrimitiveType() || expr.type.isString() || expr.type.isNullableString())
          ParamStatic(getSerializerForType(expr.type) ?: parseError("Could not create a primitive-type parser of the type ${expr.type.dumpKotlinLike()} whose class-id is ${expr.type.classId()}", expr))
        //ParamStatic(expr.type.classId()!!)
        else
          ParamCtx(expr.type)
      }
    }

    context (CX.Scope, CX.Builder) fun build(bid: BID, originalValue: IrExpression, lifter: Lifter): IrExpression

    data class ParamUsingBatchAlias(val batchVariable: IrValueParameter, val param: Type.Single, val suffix: String? = null) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          // Remember that the transformer (called below) that replaces the batch_varaible mutates the Ir so we need to make
          // copies of it in case the actual batch-variable is being reused for multiple expression (as it happens when we use capture.batch(people) { p -> ... setParams(p)... })
          // I.e. since during the eleboration phase, the actual `p` parameter gets copied so once we mutate it, it will be the same underlying instance for all
          // expressions in the elaboration. Therefore when we need to actually replace the underlying symbol we need to copy all of them.
          // If you don't do that you'll get strange `No mapping for symbol: VALUE_PARAMETER name:p` errors. This is especially confusing then the `p` variable
          // is used for every single element in the batch (and is the input value to every lambda... so it SEEMS the mapping does exist, after all it's right there
          // as a parameter in the lambda!... on it's not. Is the a different parameter with the same name.) To avoid this confounding issue, I modify the identifier
          // to have some other suffix so that the symbol is unique for each batch clause and when debugging with print-source you can clearly see that the symbols are different.
          // (in most cases the 1st symbol e.g. person.id will appear in the GetValue of every other clause (i.e. of the refiners for person.name and person.age as well).
          // Therefore it is more clear what the mutability issue is.
          // (Note that in case other similar issues occur, we might want to do a deep-copy for every single clause in the elaboration as a precautionary measure and maybe even create
          // an new IrValueParameter for them. This might have performance implications so not doing for now.)
          val madeValue = param.makeValue(originalValue).deepCopyWithSymbols(currentDeclarationParent)

          val newSymbol = IrValueParameterSymbolImpl()
          val batchVariableCopy = batchVariable.factory.createValueParameter(
            startOffset = batchVariable.startOffset,
            endOffset = batchVariable.endOffset,
            origin = batchVariable.origin,
            name = Name.identifier(batchVariable.name.identifier + "_batch" + (suffix ?: "")),
            isCrossinline = false,
            type = batchVariable.type,
            isHidden = batchVariable.isHidden,
            // Not used anymore
             //index = batchVariable.index,
            varargElementType = batchVariable.varargElementType,
            isAssignable = batchVariable.isAssignable,
            symbol = newSymbol,
            isNoinline = batchVariable.isNoinline,
            // batch variable is the `p` in capture.batch(people) { p -> ... } it always a Regular parameter
            kind = IrParameterKind.Regular
          )


          val transformer =
            object : IrElementTransformerVoid() {
              // Generic calls of IrElement bubble up to here
              override fun visitElement(expression: IrElement): IrElement {
                if (expression is IrGetValue && expression.symbol.owner == batchVariable) expression.symbol = newSymbol
                return super.visitElement(expression)
              }

              // IrGetValue and IrDeclarationReference bubble up to here
              override fun visitExpression(expression: IrExpression): IrExpression {
                if (expression is IrGetValue && expression.symbol.owner == batchVariable) expression.symbol = newSymbol
                return super.visitExpression(expression)
              }

              // Technically don't need this because it will bubble up to visitExpression but here for clarity
              override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (expression.symbol.owner == batchVariable) expression.symbol = newSymbol
                return super.visitGetValue(expression)
              }
              //override fun visitValueParameter(declaration: IrValueParameter): IrStatement =
              //  when {
              //    declaration == batchVariable -> batchVariableCopy
              //    else -> super.visitValueParameter(declaration)
              //  }
            }

          val newMadeValue = transformer.visitElement(madeValue) as IrExpression

          //val symbolRemapper =
          //  object: DeepCopySymbolRemapper() {
          //    init {
          //      valueParameters.put(batchVariable.symbol, newSymbol)
          //    }
          //    //override val valueParameters = hashMapOf<IrValueParameterSymbol, IrValueParameterSymbol>(batchVariable.symbol to newSymbol)
          //  }
          //val batchVariableCopy = batchVariable.deepCopyWithSymbols(symbolRemapper)

          val refinerLambda = createLambda1(newMadeValue, batchVariableCopy, currentDeclarationParentOrFail())
          val description = if (debugDataConfig.addParamDescriptions) refinerLambda.dumpKotlinLike().lift() else builder.irNull()
          make<ParamBatchRefiner<*, *>>(bid.lift(), refinerLambda, param.makeSerializer(), description)
        }
    }

    /**
     * This comes out of `param(value: String/Char/Int/Short/...)` serializers.
     * classId is the classId of the static ParamSerializer object
     */
    data class ParamStatic(val classId: ClassId) : Type, Single {
      context (CX.Scope, CX.Builder) override fun makeSerializer() = makeObjectFromId(classId)
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          val value = makeValue(originalValue)
          val description = if (debugDataConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
          make<ParamSingle<*>>(bid.lift(), value, makeSerializer())
        }
    }

    /**
     * This comes out of paramCtx(value: T)
     * `type` is the type to use when calling io.exoquery.serial.contextualSerializer()
     * originalValue is the `value` arg
     */
    data class ParamCtx(val type: IrType) : Type, Single {
      context (CX.Scope, CX.Builder) override fun makeSerializer() =
        callWithParams("io.exoquery.serial", "contextualSerializer", listOf(type)).invoke()

      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          val value = makeValue(originalValue)
          val description = if (debugDataConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
          make<ParamSingle<*>>(bid.lift(), value, makeSerializer())
        }
    }

    /**
     * This comes out of paramCustom(value: T, serializer: SerializationStrategy<T>)
     * ktSerializer is the SerializationStrategy (serializer arg) instance to be passed into ParamSerializer.Custom,
     * which is created via `io.exoquery.serial.customSerializer` call below
     * complete with the serializer to pass in and the type to use when calling it.
     * originalValue is `value: T`.
     */
    data class ParamCustom(val ktSerializer: IrExpression, val type: IrType) : Type, Single {
      context (CX.Scope, CX.Builder) override fun makeSerializer() = callWithParams("io.exoquery.serial", "customSerializer", listOf(type)).invoke(ktSerializer)
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          val value = makeValue(originalValue)
          val description = if (debugDataConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
          make<ParamSingle<*>>(bid.lift(), value, makeSerializer())
        }
    }

    /** valueWithSet is the ValueWithSerializer instance passed to param */
    data class ParamCustomValue(val valueWithSerializer: IrExpression) : Type, Single {
      context (CX.Scope, CX.Builder) override fun makeSerializer() = constructValueSerializer(valueWithSerializer)
      context (CX.Scope, CX.Builder) override fun makeValue(originalValue: IrExpression) = originalValue.callDispatch("value")()
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) = run {
        with(lifter) {
          val value = makeValue(originalValue)
          val description = if (debugDataConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
          make<ParamSingle<*>>(bid.lift(), value, makeSerializer())
        }
      }
    }

    context (CX.Scope, CX.Builder)
    fun constructValueSerializer(valueWithSerializer: IrExpression) = run {
      // Get the type-argument of the ValueWithSerializer<T> instance which is the actual type to be serialized e.g. ValueWithSerializer<MyCustomDateType>
      val type = valueWithSerializer.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the type of the ValueWithSerializer instance", valueWithSerializer)
      val constructSerializer = callWithParams("io.exoquery.serial", "customValueSerializer", listOf(type)).invoke(valueWithSerializer)
      constructSerializer
    }


    /**
     * for params(listOf(...))
     * originalValues is the list
     */
    data class ParamListStatic(val classId: ClassId) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the static-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, makeObjectFromId(classId))
        }
    }

    data class ParamListCtx(val type: IrType) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the context-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, callWithParams("io.exoquery.serial", "contextualSerializer", listOf(type)).invoke())
        }
    }

    /**
     * Comes out of paramsCustom(values: List<T>, serializer: SerializationStrategy<T>)
     * ktSerializer is the SerializationStrategy (serializer arg) instance to be passed into ParamSerializer.Custom,
     * which is created via `io.exoquery.serial.customSerializer` below. The type is the type to use when calling it.
     * originalValues is the `values` arg of the params call above.
     */
    data class ParamListCustom(val ktSerializer: IrExpression, val type: IrType) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the custom-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, callWithParams("io.exoquery.serial", "customSerializer", listOf(type)).invoke(ktSerializer))
        }
    }

    data class ParamListCustomValue(val valueWithSerializer: IrExpression) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) = run {
        val paramSerializer = constructValueListSerializer(valueWithSerializer)
        val value = originalValue.callDispatch("values")()
        with(lifter) {
          val valueType = value.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the custom-param-value list type: ${value.dumpKotlinLike()}", value)
          make<ParamMulti<*>>(bid.lift(), value, paramSerializer)
        }
      }
    }

    context (CX.Scope, CX.Builder)
    fun constructValueListSerializer(valueListWithSerializer: IrExpression) = run {
      // Get the type-argument of the ValuesWithSerializer<T> instance which is the actual type to be serialized e.g. ValueWithSerializer<MyCustomDateType>
      val type = valueListWithSerializer.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the type of the ValuesWithSerializer instance", valueListWithSerializer)
      val constructSerializer = callWithParams("io.exoquery.serial", "customValueListSerializer", listOf(type)).invoke(valueListWithSerializer)
      constructSerializer
    }
  }
}

class ParamsExpr(val paramBinds: List<ParamBind>, val paramsToCompose: List<IrExpression>) {
  context (CX.Scope, CX.Builder) fun lift(): IrExpression {
    val lifter = makeLifter()
    return with(lifter) {
      val paramsList = paramBinds.map { (bid, value, paramType) ->
        paramType.build(bid, value, lifter)
      }
      val newParams: IrExpression = make<ParamSet>(paramsList.liftExpr<Param<*>>())
      val out =
        paramsToCompose
          .map { it.callDispatch("params")() }
          .fold(newParams, { acc, nextParams ->
            acc.callDispatch("plus")(nextParams)
          })
      out
    }
  }
}

context(CX.Scope, CX.Builder) private fun RuntimeEmpty(): IrExpression {
  //val runtimesCompanionRef = builder.irGetObjectValue(pluginCtx.referenceClass(classIdOf<io.exoquery.Runtimes.Companion>())!!)
  val cls = classIdOf<RuntimeSet>() ?: throw RuntimeException("Could not find the class-id for RuntimeSet")
  val clsSym = pluginCtx.referenceClass(cls) ?: throw RuntimeException("Could not find the reference for the class $cls in the context")
  val clsSymCompanion = clsSym.owner.companionObject() ?: throw RuntimeException("Could not find the companion object for the class $cls")
  val runtimesCompanionRef = builder.irGetObject(clsSymCompanion.symbol)
  return runtimesCompanionRef.callDispatch("Empty")()
}

object ContainerExpr {
  object Pluckable {
    context (CX.Scope) operator fun <AP : Pattern<IrExpression>> get(x: AP) =
      customPattern1("SqlExpressionExpr.Pluckable", x) { it: IrExpression ->
        it.match(
          case(ExtractorsDomain.CaseClassConstructorCall1PlusLenient[
            Is.of(PT.io_exoquery_SqlQuery, PT.io_exoquery_SqlExpression, PT.io_exoquery_SqlAction, PT.io_exoquery_SqlBatchAction),
            Is()
          ])
            .thenThis { _, _ ->
              Components1(this)
            }
        )
      }
  }
}

object SqlExpressionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    // This is an expensive operation so put it behind a lazy value that the user will invoke only if needed
    fun unpackOrErrorXR(): UnpackResult<XR.Expression> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Expression>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    // re-create the SqlExpression instance. Note that we need a varaible from which to take params
    // So for example say we have something like:
    // val x = capture { 123 } // i.e. SqlExpression(unpack(xyz), params=...)
    // val y = x
    // we want to change `val y` to:
    // val y = SqlExpression(unpack(xyz), lifts=x.lifts)
    // That is because as we propagate the SqlExpression instance we need to keep the params the same
    // but the varaibles referenced in the params might refer to local things that are no longer
    // avaiable as well keep inlining the SqlExpression instances
    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val strExpr = call(PT.io_exoquery_unpackExpr).invoke(builder.irString(packedXR))
      // TODO we know at this point Runtimes is Runtimes.Empty so need to add that when we add that variable
      //error(
      //  """|------- Calling replant -------
      //     |${Caller.Dispatch(paramsFrom).call("params")().dump()}
      //""".trimMargin())

      val callParams = paramsFrom.callDispatch("params").invoke()

      // Not sure why calling it like this makes it blow up
      //val make = makeClassFromString(Paths.SqlExpression, listOf(strExpr, Caller.Dispatch(paramsFrom).call("params")()))
      val make = makeClassFromString(PT.io_exoquery_SqlExpression, listOf(strExpr, RuntimeEmpty(), callParams))
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlExpressionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            // Match on: SqlExpression(unpackExpr(str))
            case(ExtractorsDomain.CaseClassConstructorCall1Plus[Is(PT.io_exoquery_SqlExpression), Ir.Call.FunctionUntethered1[Is(PT.io_exoquery_unpackExpr), Is()]])
              .thenIf { _, _ ->
                // Check that the 2nd argument to SqlExpression is Runtimes.Empty i.e. SqlExpression(xr=unpackExpr(str), runtimes=Runtimes.Empty, ...)
                comp.regularArgs[1].isEmptyRuntimes()
              }
              .then { _, (_, irStr) ->
                // The 1st argument to SqlExpression in the unpackExpr ie. SqlExpression(unpackExpr(str), ...)
                val constPackedXR = irStr as? IrConst ?: throw IllegalArgumentException("value passed to unpackExpr was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Expression, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackExpr).invoke(builder.irString(packedXR))
        val make = makeClassFromString(PT.io_exoquery_SqlExpression, listOf(strExpr, RuntimeEmpty(), params.lift()))
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Expression, runtimes: RuntimesExpr, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackExpr).invoke(builder.irString(packedXR))
        val make = makeClassFromString(PT.io_exoquery_SqlExpression, listOf(strExpr, runtimes.lift(), params.lift()))
        return make
      }
    }
  }
}

sealed interface UnpackResult<out T> {
  data class Success<T>(val value: T) : UnpackResult<T>
  data class Failure(val e: Throwable) : UnpackResult<Nothing>

  context(CX.Scope)
  fun successOrParseError(element: IrExpression) = when (this) {
    is Success -> value
    is Failure -> parseError("Could not unpack the XR from the packed string due to: ${e.stackTraceToString()}", element)
  }
}

sealed interface UprootableExpr {
  val packedXR: String

  context(CX.Scope, CX.Builder)
  fun replant(paramsFrom: IrExpression): IrExpression
}

// This is effectively a carbon-copy of the SqlExpressionExpr with some different names. We can abstract out many
// common pieces of this code into a shared Base-class or shared functions but for only two ContainerOfXR types it is
// not worth it. Need to keep an eye on this as we add more ContainerOfXR types
object SqlQueryExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {

    fun unpackOrErrorXR(): UnpackResult<XR.Query> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Query>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val strExpr = call(PT.io_exoquery_unpackQuery).invoke(builder.irString(packedXR))
      val callParams = paramsFrom.callDispatch("params").invoke()
      val make = makeClassFromString(PT.io_exoquery_SqlQuery, listOf(strExpr, RuntimeEmpty(), callParams))
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlQueryExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(ExtractorsDomain.CaseClassConstructorCall1Plus[Is(PT.io_exoquery_SqlQuery), Ir.Call.FunctionUntethered1[Is(PT.io_exoquery_unpackQuery), Is()]])
              .thenIf { _, _ ->
                comp.regularArgs[1].isEmptyRuntimes()
              }
              .then { _, (_, irStr) ->
                val constPackedXR = irStr as? IrConst ?: throw IllegalArgumentException("value passed to unpackQuery was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Query, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackQuery).invoke(builder.irString(packedXR))
        // TODO cache the class types using the pattern in Types.kt so the class-id doesn't need to be looked up over and over again
        val make = makeClassFromString(PT.io_exoquery_SqlQuery, listOf(strExpr, RuntimeEmpty(), params.lift()))
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Query, runtimes: RuntimesExpr, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackQuery).invoke(builder.irString(packedXR))
        // TODO cache the class types using the pattern in Types.kt so the class-id doesn't need to be looked up over and over again
        val make = makeClassFromString(PT.io_exoquery_SqlQuery, listOf(strExpr, runtimes.lift(), params.lift()))
        return make
      }
    }
  }
}


object SqlActionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    fun unpackOrErrorXR(): UnpackResult<XR.Action> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Action>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val strExpr = call(PT.io_exoquery_unpackAction).invoke(builder.irString(packedXR))
      val callParams = paramsFrom.callDispatch("params").invoke()
      val inputType = paramsFrom.type.simpleTypeArgs[0]
      val outputType = paramsFrom.type.simpleTypeArgs[1]
      val make = makeClassFromString(PT.io_exoquery_SqlAction, listOf(strExpr, RuntimeEmpty(), callParams), listOf(inputType, outputType))
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlActionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(ExtractorsDomain.CaseClassConstructorCall1Plus[Is(PT.io_exoquery_SqlAction), Ir.Call.FunctionUntethered1[Is(PT.io_exoquery_unpackAction), Is()]])
              .thenIf { _, _ -> comp.regularArgs[1].isEmptyRuntimes() }
              .then { _, (_, irStr) ->
                val constPackedXR = irStr as? IrConst ?: throw IllegalArgumentException("value passed to unpackAction was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Action, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackAction).invoke(builder.irString(packedXR))
        val inputType = originalType.simpleTypeArgs[0]
        val outputType = originalType.simpleTypeArgs[1]
        val make = makeClassFromString(PT.io_exoquery_SqlAction, listOf(strExpr, RuntimeEmpty(), params.lift()), listOf(inputType, outputType))
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Action, runtimes: RuntimesExpr, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackAction).invoke(builder.irString(packedXR))
        val inputType = originalType.simpleTypeArgs[0]
        val outputType = originalType.simpleTypeArgs[1]
        val make = makeClassFromString(PT.io_exoquery_SqlAction, listOf(strExpr, runtimes.lift(), params.lift()), listOf(inputType, outputType))
        return make
      }
    }
  }
}

object SqlBatchActionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    fun unpackOrErrorXR(): UnpackResult<XR.Batching> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Batching>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val strExpr = call(PT.io_exoquery_unpackBatchAction).invoke(builder.irString(packedXR))
      val callParams = paramsFrom.callDispatch("params").invoke()
      val batchParam = paramsFrom.callDispatch("batchParam").invoke()
      val make = makeClassFromString(PT.io_exoquery_SqlBatchAction, listOf(strExpr, batchParam, RuntimeEmpty(), callParams))
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlBatchActionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(ExtractorsDomain.CaseClassConstructorCall1Plus[Is(PT.io_exoquery_SqlBatchAction), Ir.Call.FunctionUntethered1[Is(PT.io_exoquery_unpackBatchAction), Is()]])
              .thenIf { _, _ -> comp.regularArgs[2].isEmptyRuntimes() }
              .then { _, (_, irStr) ->
                val constPackedXR = irStr as? IrConst ?: throw IllegalArgumentException("value passed to unpackBatchAction was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          ) //?: parseError("Could not match the SqlBatchActionExpr.Uprootable pattern", it)
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Batching, batchParam: IrExpression, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackBatchAction).invoke(builder.irString(packedXR))
        val make = makeClassFromString(PT.io_exoquery_SqlBatchAction, listOf(strExpr, batchParam, RuntimeEmpty(), params.lift()))
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Batching, batchParam: IrExpression, runtimes: RuntimesExpr, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val strExpr = call(PT.io_exoquery_unpackBatchAction).invoke(builder.irString(packedXR))
        val make = makeClassFromString(PT.io_exoquery_SqlBatchAction, listOf(strExpr, batchParam, runtimes.lift(), params.lift()))
        return make
      }
    }
  }
}


context(CX.Scope)
private fun IrExpression?.isEmptyRuntimes(): Boolean =
  this?.match(
    case(Ir.Call.FunctionMem0[Ir.Expr.ClassOf<RuntimeSet.Companion>(), Is(PT.EmptyRuntimes)]).then { _, _ -> true }
  ) ?: false
