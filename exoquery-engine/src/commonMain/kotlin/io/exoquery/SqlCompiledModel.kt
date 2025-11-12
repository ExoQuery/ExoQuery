package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.Token
import io.exoquery.xr.XR

@ExoInternal
sealed interface Phase {
  data object CompileTime : Phase
  data object Runtime : Phase
}

@ExoInternal
sealed interface ActionKind {
  fun isUpdateOrDelete() = this == Update || this == Delete
  fun isDelete() = this == Delete

  object Insert : ActionKind
  object Update : ActionKind
  object Delete : ActionKind
  object Unknown : ActionKind
}

@ExoInternal
abstract class ExoCompiled {
  abstract val value: String
  abstract val params: List<Param<*>>
  abstract val token: Token
  abstract fun originalXR(): XR

  abstract fun determinizeDynamics(): ExoCompiled

  protected fun determinizedToken() = determinizeToken(token, params).first
}

@ExoInternal
fun determinizeToken(token: Token, params: List<Param<*>>): Pair<Token, List<Param<*>>> {
  var id = 0
  fun nextId() = "$id".also { id++ }
  val bids = params.map { param ->
    val newId = BID(nextId())
    (param.id to newId) to param.withNewBid(newId)
  }
  val (bidMap, newParams) = bids.unzip()
  return token.mapBids(bidMap.toMap()) to newParams
}
