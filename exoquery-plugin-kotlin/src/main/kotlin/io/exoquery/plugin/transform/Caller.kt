package io.exoquery.plugin.transform

import io.decomat.Components1
import io.decomat.Pattern
import io.decomat.customPattern1
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import io.decomat.HasProductClass as PC
import io.decomat.productComponentsOf as productOf

sealed class ReceiverCaller(open val reciver: IrExpression) : Caller, PC<ReceiverCaller> {
  override val productComponents by lazy { productOf(this, reciver) }

  companion object {
    operator fun <AP : Pattern<A>, A : IrExpression> get(value: AP) =
      customPattern1("ReceiverCaller", value) { it: ReceiverCaller ->
        Components1(it.reciver)
      }
  }

  fun <D> transform(transformer: IrTransformer<D>, data: D) =
    when (this) {
      is Caller.Dispatch -> Caller.Dispatch(reciver.transform(transformer, data))
      is Caller.Extension -> Caller.Extension(reciver.transform(transformer, data))
    }
}

sealed interface Caller {
  data class Dispatch(override val reciver: IrExpression) : ReceiverCaller(reciver)
  data class Extension(override val reciver: IrExpression) : ReceiverCaller(reciver)
  data class TopLevelMethod(val packageName: String) : Caller

  fun toDispatch() =
    when (this) {
      is Caller.Dispatch -> this
      is Caller.Extension -> Caller.Dispatch(reciver)
      // Can't do anything if it's a top-level method so ignore
      is Caller.TopLevelMethod -> this
    }

  fun toExtension() =
    when (this) {
      is Caller.Dispatch -> Caller.Extension(reciver)
      is Caller.Extension -> this
      // Can't do anything if it's a top-level method so ignore
      is Caller.TopLevelMethod -> this
    }
}
