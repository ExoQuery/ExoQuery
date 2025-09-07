package io.exoquery.annotation

import io.exoquery.serial.ParamSerializer
import io.exoquery.sql.SqlIdiom
import io.exoquery.util.TraceType
import kotlin.reflect.KClass

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class WindowFun(val name: String)

// Internal. Identifies which functions ExoQuery transformations have seen already
@ExoInternal
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class WasSterilizedAdHoc

@ExoInternal
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class Dsl

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class ExoGoldenTest

/**
 * Use it to trace query compilation like this:
 * ```
 * @file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class)
 * ```
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TracesEnabled(vararg val traceType: KClass<out TraceType>)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class ExoGoldenOverride

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoExtras

sealed interface DslFunctionCallType {
  object PureFunction : DslFunctionCallType
  object ImpureFunction : DslFunctionCallType
  object Aggregator : DslFunctionCallType
  object QueryAggregator : DslFunctionCallType
}

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class DslFunctionCall(val type: KClass<out DslFunctionCallType>, val name: String = "")

@ExoInternal
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class DslNestingIgnore

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class DslBooleanExpression

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ParamStatic(val type: KClass<out ParamSerializer<out Any>>)

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ParamCtx

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ParamPrimitive

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ParamCustom

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ParamCustomValue

// Used internally
@ExoInternal
annotation class CapturedFunctionSketch(vararg val sketch: ParamSketch) {
  @ExoInternal
  companion object {
  }
}
@ExoInternal
annotation class ParamSketch(val paramKind: String, val stableIdent: String) {
  @ExoInternal
  companion object {
    @ExoInternal
    fun from(paramKind: ParamKind, stableIdent: String) =
      ParamSketch(paramKind.name, stableIdent)
  }
}

fun ParamSketch.paramKindParsed(): ParamKind? =
  ParamKind.fromString(paramKind)


@ExoInternal
sealed interface ParamKind {
  val name: String

  data object Dispatch : ParamKind { override val name = "Dispatch" }
  data object Context : ParamKind { override val name = "Context" }
  data object Extension : ParamKind { override val name = "Extension" }
  data object Regular : ParamKind { override val name = "Regular" }

  companion object {
    fun fromString(name: String): ParamKind? =
      when (name) {
        Dispatch.name -> Dispatch
        Context.name -> Context
        Extension.name -> Extension
        Regular.name -> Regular
        else -> null
      }

    val values = listOf(Dispatch, Context, Extension, Regular)
  }
}

// Using annotations to identify what functions capture queries/expressions/etc...
// instead of parsing them by name is a more flexible approach.
@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCapture

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCodegenFunction

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCodegenReturnFunction

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCodegenJustReturnFunction

@ExoInternal
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ExoBuildFunctionLabel

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoBuildDatabaseSpecific(val dialect: KClass<out SqlIdiom>)

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoBuildRoomSpecific

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCaptureSelect

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCaptureBatch

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoCaptureExpression

@ExoInternal
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class ExoUseExpression

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class FlatJoin

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class FlatJoinLeft

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoInsert

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoUpdate

@ExoInternal
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExoDelete

@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message = "This API is internal to ExoQuery."
)
@Retention(AnnotationRetention.BINARY)
annotation class ExoInternal
