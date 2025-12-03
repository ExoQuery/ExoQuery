package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.*
import io.exoquery.plugin.classId
import io.exoquery.plugin.classIdOf
import io.exoquery.plugin.transform.*
import io.exoquery.xr.EncodingXR
import io.exoquery.xr.XR
import io.exoquery.xr.encode
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

    sealed interface Multi : Type {
      context (CX.Scope, CX.Builder) fun makeSerializer(): IrExpression
      context (CX.Scope, CX.Builder) fun makeValue(originalValue: IrExpression): IrExpression = originalValue
    }

    companion object {
      context(CX.Scope)
      fun auto(expr: IrExpression) =
        getSingleSerializerForLeafType(expr.type, ParseError.Origin.from(expr)) ?: ParamCtx(expr.type)
    }

    context (CX.Scope, CX.Builder) fun build(bid: BID, originalValue: IrExpression, lifter: Lifter): IrExpression

    data class ParamUsingBatchAlias(val batchVariable: IrValueParameter, val param: Type.Single, val suffix: String? = null) : Type {
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          // Remember that the transformer (called below) that replaces the batch_varaible mutates the Ir so we need to make
          // copies of it in case the actual batch-variable is being reused for multiple expression (as it happens when we use sql.batch(people) { p -> ... setParams(p)... })
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
            // batch variable is the `p` in sql.batch(people) { p -> ... } it always a Regular parameter
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
            }

          val newMadeValue = transformer.visitElement(madeValue) as IrExpression
          val refinerLambda = createLambda1(newMadeValue, batchVariableCopy, currentDeclarationParentOrFail())
          val description = if (perFileDebugConfig.addParamDescriptions) refinerLambda.dumpKotlinLike().lift() else builder.irNull()
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
          val description = if (perFileDebugConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
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
          val description = if (perFileDebugConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
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
          val description = if (perFileDebugConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
          make<ParamSingle<*>>(bid.lift(), value, makeSerializer())
        }
    }

    data class ParamCustomImplicit(val type: IrType) : Type, Single {
      context (CX.Scope, CX.Builder) override fun makeSerializer() = run {
        val constructSerializer = callWithParams("kotlinx.serialization", "serializer", listOf(type))()
        callWithParams("io.exoquery.serial", "customSerializer", listOf(type)).invoke(constructSerializer)
      }
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) =
        with(lifter) {
          val value = makeValue(originalValue)
          val description = if (perFileDebugConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
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
          val description = if (perFileDebugConfig.addParamDescriptions) value.dumpKotlinLike().lift() else builder.irNull()
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
    data class ParamListStatic(val classId: ClassId) : Multi, Type {
      context(CX.Scope, CX.Builder) override fun makeSerializer() = makeObjectFromId(classId)
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the static-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, makeSerializer())
        }
    }

    data class ParamListCtx(val type: IrType) : Multi, Type {
      context(CX.Scope, CX.Builder) override fun makeSerializer() = callWithParams("io.exoquery.serial", "contextualSerializer", listOf(type)).invoke()
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the context-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, makeSerializer())
        }
    }

    /**
     * Comes out of paramsCustom(values: List<T>, serializer: SerializationStrategy<T>)
     * ktSerializer is the SerializationStrategy (serializer arg) instance to be passed into ParamSerializer.Custom,
     * which is created via `io.exoquery.serial.customSerializer` below. The type is the type to use when calling it.
     * originalValues is the `values` arg of the params call above.
     */
    data class ParamListCustom(val ktSerializer: IrExpression, val type: IrType) : Multi, Type {
      context(CX.Scope, CX.Builder) override fun makeSerializer() = callWithParams("io.exoquery.serial", "customSerializer", listOf(type)).invoke(ktSerializer)
        context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the custom-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, makeSerializer())
        }
    }

    data class ParamListCustomImplicit(val type: IrType) : Multi, Type {
      context(CX.Scope, CX.Builder) override fun makeSerializer() = run {
        val constructSerializer = callWithParams("kotlinx.serialization", "serializer", listOf(type))()
        callWithParams("io.exoquery.serial", "customSerializer", listOf(type)).invoke(constructSerializer)
      }
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValues: IrExpression, lifter: Lifter) =
        with(lifter) {
          val valueType = originalValues.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the custom-param list type: ${originalValues.dumpKotlinLike()}", originalValues)
          make<ParamMulti<*>>(bid.lift(), originalValues, makeSerializer())
        }
    }

    data class ParamListCustomValue(val valueWithSerializer: IrExpression) : Multi, Type {
      context(CX.Scope, CX.Builder) override fun makeSerializer() = constructValueListSerializer(valueWithSerializer)
      context (CX.Scope, CX.Builder) override fun build(bid: BID, originalValue: IrExpression, lifter: Lifter) = run {
        val value = originalValue.callDispatch("values")()
        with(lifter) {
          val valueType = value.type.simpleTypeArgs.firstOrNull() ?: parseError("Could not get the first type-argument of the custom-param-value list type: ${value.dumpKotlinLike()}", value)
          make<ParamMulti<*>>(bid.lift(), value, makeSerializer())
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
          case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlQuery.Companion>(), Is("fromPackedXR"), Is()]).thenThis { _, _ ->
            Components1(this)
          },
          case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlAction.Companion>(), Is("fromPackedXR"), Is()]).thenThis { _, _ ->
            Components1(this)
          },
          case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlExpression.Companion>(), Is("fromPackedXR"), Is()]).thenThis { _, _ ->
            Components1(this)
          },
          case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlBatchAction.Companion>(), Is("fromPackedXR"), Is()]).thenThis { _, _ ->
            Components1(this)
          }
        )
      }
  }
}

object SqlExpressionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    // This is an expensive operation so put it behind a lazy value that the user will invoke only if needed
    override fun unpackOrErrorXR(): UnpackResult<XR.Expression> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Expression>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    // re-create the SqlExpression instance. Note that we need a varaible from which to take params
    // So for example say we have something like:
    // val x = sql { 123 } // i.e. SqlExpression(unpack(xyz), params=...)
    // val y = x
    // we want to change `val y` to:
    // val y = SqlExpression(unpack(xyz), lifts=x.lifts)
    // That is because as we propagate the SqlExpression instance we need to keep the params the same
    // but the varaibles referenced in the params might refer to local things that are no longer
    // avaiable as well keep inlining the SqlExpression instances
    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val callParams = paramsFrom.callDispatch("params").invoke()
      val make = makeObject<SqlExpression.Companion>()
        .callDispatch("fromPackedXR")
        .invoke(builder.irString(packedXR), RuntimeEmpty(), callParams)
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlExpressionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlExpression.Companion>(), Is("fromPackedXR"), Is()])
              .thenIf { _, args ->
                // Ensure runtimes arg is Runtimes.Empty
                args[1].isEmptyRuntimes()
              }
              .then { _, args ->
                val constPackedXR = args[0] as? IrConst ?: throw IllegalArgumentException("value passed to unpackExpr was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootableWithPacked(xr: XR.Expression, params: ParamsExpr): Pair<IrExpression, SqlExpressionExpr.Uprootable> {
        val packedXR = xr.encode()
        val make = makeObject<SqlExpression.Companion>()
          .callDispatch("fromPackedXR")
          .invoke(builder.irString(packedXR), RuntimeEmpty(), params.lift())
        return make to SqlExpressionExpr.Uprootable(packedXR)
      }
      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Expression, params: ParamsExpr): IrExpression =
        plantNewUprootableWithPacked(xr, params).first

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Expression, runtimes: RuntimesExpr, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        val make = makeObject<SqlExpression.Companion>()
          .callDispatch("fromPackedXR")
          .invoke(builder.irString(packedXR), runtimes.lift(), params.lift())
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

  fun unpackOrErrorXR(): UnpackResult<XR>

  context(CX.Scope, CX.Builder)
  fun replant(paramsFrom: IrExpression): IrExpression
}

// This is effectively a carbon-copy of the SqlExpressionExpr with some different names. We can abstract out many
// common pieces of this code into a shared Base-class or shared functions but for only two ContainerOfXR types it is
// not worth it. Need to keep an eye on this as we add more ContainerOfXR types
object SqlQueryExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {

    override fun unpackOrErrorXR(): UnpackResult<XR.Query> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Query>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val callParams = paramsFrom.callDispatch("params").invoke()
      val make = makeObject<SqlQuery.Companion>().callDispatch("fromPackedXR").invoke(builder.irString(packedXR), RuntimeEmpty(), callParams)
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlQueryExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            //  [Is(PT.io_exoquery_SqlQuery), Ir.Call.FunctionUntethered1[Is(PT.io_exoquery_unpackQuery), Is()]]
            case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlQuery.Companion>(), Is("fromPackedXR"), Is()])
              .thenIf { _, args ->
                args[1].isEmptyRuntimes()
              }
              .then { _, args ->
                val constPackedXR = args[0] as? IrConst ?: throw IllegalArgumentException("value passed to unpackQuery was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootableWithPacked(xr: XR.Query, params: ParamsExpr): Pair<IrExpression, SqlQueryExpr.Uprootable> {
        val packedXR = xr.encode()
        // TODO cache the class types using the pattern in Types.kt so the class-id doesn't need to be looked up over and over again
        val make = makeObject<SqlQuery.Companion>().callDispatch("fromPackedXR").invoke(builder.irString(packedXR), RuntimeEmpty(), params.lift())
        return make to SqlQueryExpr.Uprootable(packedXR)
      }
      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Query, params: ParamsExpr): IrExpression =
        plantNewUprootableWithPacked(xr, params).first


      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Query, runtimes: RuntimesExpr, params: ParamsExpr): IrExpression {
        val packedXR = xr.encode()
        // TODO cache the class types using the pattern in Types.kt so the class-id doesn't need to be looked up over and over again
        val make = makeObject<SqlQuery.Companion>().callDispatch("fromPackedXR").invoke(builder.irString(packedXR), runtimes.lift(), params.lift())
        return make
      }
    }
  }
}


object SqlActionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    override fun unpackOrErrorXR(): UnpackResult<XR.Action> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Action>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val callParams = paramsFrom.callDispatch("params").invoke()
      val inputType = paramsFrom.type.simpleTypeArgs[0]
      val outputType = paramsFrom.type.simpleTypeArgs[1]
      val make = makeObject<SqlAction.Companion>()
        .callDispatchWithParams("fromPackedXR", listOf(inputType, outputType))
        .invoke(builder.irString(packedXR), RuntimeEmpty(), callParams)
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlActionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlAction.Companion>(), Is("fromPackedXR"), Is()])
              .thenIf { _, args ->
                args[1].isEmptyRuntimes()
              }
              .then { _, args ->
                val constPackedXR = args[0] as? IrConst ?: throw IllegalArgumentException("value passed to unpackAction was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          )
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Action, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val inputType = originalType.simpleTypeArgs[0]
        val outputType = originalType.simpleTypeArgs[1]
        val make = makeObject<SqlAction.Companion>()
          .callDispatchWithParams("fromPackedXR", listOf(inputType, outputType))
          .invoke(builder.irString(packedXR), RuntimeEmpty(), params.lift())
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Action, runtimes: RuntimesExpr, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val inputType = originalType.simpleTypeArgs[0]
        val outputType = originalType.simpleTypeArgs[1]
        val make = makeObject<SqlAction.Companion>()
          .callDispatchWithParams("fromPackedXR", listOf(inputType, outputType))
          .invoke(builder.irString(packedXR), runtimes.lift(), params.lift())
        return make
      }
    }
  }
}

object SqlBatchActionExpr {
  data class Uprootable(override val packedXR: String): UprootableExpr {
    override fun unpackOrErrorXR(): UnpackResult<XR.Batching> =
      try {
        UnpackResult.Success(EncodingXR.protoBuf.decodeFromHexString<XR.Batching>(packedXR))
      } catch (e: Throwable) {
        UnpackResult.Failure(e)
      }

    context(CX.Scope, CX.Builder)
    override fun replant(paramsFrom: IrExpression): IrExpression {
      val callParams = paramsFrom.callDispatch("params").invoke()
      val batchParam = paramsFrom.callDispatch("batchParam").invoke()
      val batchInputType = paramsFrom.type.simpleTypeArgs[0]
      val inputType = paramsFrom.type.simpleTypeArgs[1]
      val outputType = paramsFrom.type.simpleTypeArgs[2]
      val make = makeObject<SqlBatchAction.Companion>()
        .callDispatchWithParams("fromPackedXR", listOf(batchInputType, inputType, outputType))
        .invoke(builder.irString(packedXR), batchParam, RuntimeEmpty(), callParams)
      return make
    }

    companion object {
      context (CX.Scope) operator fun <AP : Pattern<Uprootable>> get(x: AP) =
        customPattern1("SqlBatchActionExpr.Uprootable", x) { it: IrExpression ->
          it.match(
            case(Ir.Call.FunctionMemN[Ir.Expr.ClassOf<SqlBatchAction.Companion>(), Is("fromPackedXR"), Is()])
              .thenIf { _, args ->
                // Ensure runtimes arg is Runtimes.Empty
                args[2].isEmptyRuntimes()
              }
              .then { _, args ->
                val constPackedXR = args[0] as? IrConst ?: throw IllegalArgumentException("value passed to unpackBatchAction was not a constant-string in:\n${it.dumpKotlinLike()}")
                Components1(Uprootable(constPackedXR.value.toString()))
              }
          ) //?: parseError("Could not match the SqlBatchActionExpr.Uprootable pattern", it)
        }

      context(CX.Scope, CX.Builder) fun plantNewUprootable(xr: XR.Batching, batchParam: IrExpression, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val batchInputType = originalType.simpleTypeArgs[0]
        val inputType = originalType.simpleTypeArgs[1]
        val outputType = originalType.simpleTypeArgs[2]
        val make = makeObject<SqlBatchAction.Companion>()
          .callDispatchWithParams("fromPackedXR", listOf(batchInputType, inputType, outputType))
          .invoke(builder.irString(packedXR), batchParam, RuntimeEmpty(), params.lift())
        return make
      }

      context(CX.Scope, CX.Builder) fun plantNewPluckable(xr: XR.Batching, batchParam: IrExpression, runtimes: RuntimesExpr, params: ParamsExpr, originalType: IrType): IrExpression {
        val packedXR = xr.encode()
        val batchInputType = originalType.simpleTypeArgs[0]
        val inputType = originalType.simpleTypeArgs[1]
        val outputType = originalType.simpleTypeArgs[2]
        val make = makeObject<SqlBatchAction.Companion>()
          .callDispatchWithParams("fromPackedXR", listOf(batchInputType, inputType, outputType))
          .invoke(builder.irString(packedXR), batchParam, runtimes.lift(), params.lift())
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
