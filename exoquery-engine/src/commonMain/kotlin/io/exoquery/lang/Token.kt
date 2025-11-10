package io.exoquery.lang

import io.exoquery.BID
import io.exoquery.Param
import io.exoquery.ParamBatchRefiner
import io.exoquery.ParamMulti
import io.exoquery.ParamSet
import io.exoquery.ParamSingle
import io.exoquery.pprint.PPrinterConfig
import io.exoquery.printing.PrintToken
import io.exoquery.util.intersperseWith
import io.exoquery.util.mkString
import io.exoquery.xrError

sealed interface Token {
  // Builds the actual string to be used as the SQL query as opposed to just for display purposes
  fun build(): String

  /**
   * Can the token vary based on the amount of parameters, e.g. the ParamMultiToken is NOT static because
   * its rendered form depends on how many values are in the ParamMulti. However a ParamSingleToken IS static because it always
   * renders to "?" regardless of the parameter value.
   *
   * This parameter is NOT the same as a static/dynamic query because even a static query can have `IN (?, ?, ... )` clauses
   * which have a variable number of parameters based on the ParamMulti values.
   */
  fun isStatic(): Boolean

  // For cases where it is a Param actually plugin the value i.e. stringify it
  fun renderWith(renderer: Renderer): String
  fun showRaw(config: PPrinterConfig = PPrinterConfig()): String = PrintToken(config).invoke(this).toString()

  fun simplify(): Token = this

  fun mapBids(bids: Map<BID, BID>) =
    object : StatelessTokenTransformer {
      override fun invoke(token: ParamSingleToken): Token = token.withBid(bid = bids[token.bid] ?: token.bid)
      override fun invoke(token: ParamMultiToken): Token = token.withBid(bid = bids[token.bid] ?: token.bid)
      override fun invoke(token: ParamSingleTokenRealized): Token = token.withBid(bids[token.bid] ?: token.bid)
      override fun invoke(token: ParamMultiTokenRealized): Token = token.withBid(bid = bids[token.bid] ?: token.bid)
      override fun invoke(token: ParamBatchToken): Token = token.withBid(bid = bids[token.bid] ?: token.bid)
      override fun invoke(token: ParamBatchTokenRealized): Token = token.withBid(bid = bids[token.bid] ?: token.bid)
    }.invoke(this)

  fun withNonStrictEquality() =
    StatelessTokenTransformer {
      when (it) {
        is ParamBatchTokenRealized -> it.copy(param = it.param?.withNonStrictEquality())
        is ParamMultiTokenRealized -> it.copy(param = it.param?.withNonStrictEquality())
        is ParamSingleTokenRealized -> it.copy(param = it.param?.withNonStrictEquality())
        else -> null
      }
    }.invoke(this)


  fun <T> ArrayDeque<T>.addAllFirst(tokens: List<T>) {
    for (i in tokens.lastIndex downTo 0) {
      val token = tokens[i]
      this.addFirst(token)
    }
  }


  fun extractParams(dissalowUnrefinedBatchParams: Boolean = false): List<Param<*>> {
    val accum = mutableListOf<Param<*>>()
    fun errorNotFound(bid: BID): Nothing = xrError("Param not found for bid: ${bid} in tokenization:\n${this.renderWith(Renderer(true, true, null))}")
    fun errorUnrealizedFound(bid: BID): Nothing = xrError("Unrealized Param found for bid: ${bid} in tokenization:\n${this.renderWith(Renderer(true, true, null))}")

    // Depth-first search of the tree to find all the params
    // NOTE: It is VERY important to travel this in a depth-first manner otherwise the Params will come out in the wrong order
    // and the wrong things will be substituted into the query `?` placeholders. Everything that adds to the toExplore list needs to prepend
    val toExplore = ArrayDeque<Token>(listOf(this))
    while (toExplore.isNotEmpty()) {
      val token = toExplore.removeFirst()
      when (token) {
        is ParamSingleTokenRealized -> accum.add(token.param ?: errorNotFound(token.bid))
        is ParamMultiTokenRealized -> accum.add(token.param ?: errorNotFound(token.bid))
        is ParamBatchTokenRealized -> {
          if (token.param != null && dissalowUnrefinedBatchParams)
            xrError("Unrefined ParamBatchToken found for bid: ${token.bid} (and description ${token.param.description}) in tokenization:\n${this.renderWith(Renderer(true, true, null))}")
          else
            accum.add(token.param ?: errorNotFound(token.bid))
        }
        // Make SURE these prepend, children of this need to be explored first
        is Statement -> toExplore.addAll(0, token.tokens)
        is SetContainsToken -> toExplore.addAll(0, listOf(token.a, token.op, token.b))
        is TokenContext -> toExplore.add(0, token.content)
        is ParamMultiToken -> errorUnrealizedFound(token.bid)
        is ParamSingleToken -> errorUnrealizedFound(token.bid)
        is ParamBatchToken -> errorUnrealizedFound(token.bid)
        is StringToken -> {} // No params in a string token
      }
    }
    return accum
  }
}

sealed interface TagToken : Token

final data class StringToken(val string: String) : Token {
  override fun isStatic() = true
  override fun build(): String = string
  override fun renderWith(renderer: Renderer): String = string
}

final data class ParamSingleToken(val bid: BID) : Token {
  override fun isStatic() = true
  override fun build() = "<UNR?>"
  fun realize(paramSet: ParamSet) =
    ParamSingleTokenRealized(bid, paramSet.lifts.asSequence().filterIsInstance<ParamSingle<*>>().find { p -> p.id == bid })

  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, null, false)
  fun withBid(bid: BID) = ParamSingleToken(bid)
}

// Allow for the possibility of `param` being an error so that we can introspect the tree for errors
// withiout immediately failing on creation
final data class ParamSingleTokenRealized(val bid: BID, val param: ParamSingle<*>?) : Token {
  override fun isStatic() = true
  override fun build(): String = param?.let { "?" } ?: xrError("Param not found for bid: ${bid}")
  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, param, true)
  fun withBid(bid: BID) = ParamSingleTokenRealized(bid, param?.withNewBid(bid))
}

final data class ParamMultiToken(val bid: BID) : Token {
  override fun isStatic() = false
  override fun build() = "<UNRS?>"
  fun realize(paramSet: ParamSet) =
    ParamMultiTokenRealized(bid, paramSet.lifts.asSequence().filterIsInstance<ParamMulti<*>>().find { p -> p.id == bid })

  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, null, false)
  fun withBid(bid: BID) = ParamMultiToken(bid)
}

final data class ParamMultiTokenRealized(val bid: BID, val param: ParamMulti<*>?) : Token {
  override fun isStatic() = false
  // NOTE probably more efficient to just count the param values and use .repeat() to get a list of "?"s
  override fun build(): String = param?.value?.map { _ -> "?" }?.joinToString(", ") ?: xrError("Param not found for bid: ${bid}")
  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, param, true)
  fun withBid(bid: BID) = ParamMultiTokenRealized(bid, param?.withNewBid(bid))
}

final data class ParamBatchToken(val bid: BID) : Token {
  override fun isStatic() = true
  override fun build() = "<UNRB?>"
  fun realize(paramSet: ParamSet) =
    ParamBatchTokenRealized(bid, paramSet.lifts.asSequence().filterIsInstance<ParamBatchRefiner<*, *>>().find { p -> p.id == bid }, 0)

  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, null, false)
  fun withBid(bid: BID) = ParamBatchToken(bid)
}

final data class ParamBatchTokenRealized(val bid: BID, val param: ParamBatchRefiner<*, *>?, val chunkIndex: Int) : Token {
  override fun isStatic() = true
  override fun build(): String = param?.let { "?" } ?: xrError("Param not found for bid: ${bid}")
  override fun renderWith(renderer: Renderer): String = renderer.invoke(bid, param, true)
  fun withBid(bid: BID) = ParamBatchTokenRealized(bid, param?.withNewBid(bid), chunkIndex)
  fun withChunkIndex(chunkIndex: Int) = ParamBatchTokenRealized(bid, param, chunkIndex)
  fun refineAny(elem: Any?): Token = run {
    val refinedParam = param?.refineAny(elem) ?: xrError("Could not refine parameter in the ParamBatchTokenRealized: ${bid}")
    ParamSingleTokenRealized(bid, refinedParam)
  }
}

final data class TokenContext(val content: Token, val kind: Kind) : Token {
  override fun isStatic() = content.isStatic()
  sealed interface Kind {
    data object AssignmentBlock : Kind
  }

  override fun build(): String = content.build()
  override fun renderWith(renderer: Renderer): String = content.renderWith(renderer)
}

final data class Statement(val tokens: List<Token>) : Token {
  /**
   * Are all tokens must be static for the statement to be static e.g.
   * if there is even one ParamMultiToken then the statement is not static.
   */
  override fun isStatic() = tokens.all { it.isStatic() }
  override fun build(): String = tokens.map { it.build() }.mkString()
  override fun renderWith(renderer: Renderer): String = tokens.map { it.renderWith(renderer) }.mkString()

  fun prepend(token: Token): Statement = Statement(listOf(token) + tokens)
  fun append(token: Token): Statement = Statement(tokens + listOf(token))

  override fun simplify(): Token {
    val accum = mutableListOf<Token>()
    for (token in tokens) {
      if (accum.isNotEmpty() && token is StringToken && accum.last() is StringToken) {
        // Use StringBuilder here for efficiency but it is more algorithmically annoying
        accum[accum.size - 1] = StringToken((accum.last() as StringToken).string + token.string)
      } else {
        accum.add(token)
      }
    }
    if (accum.size == 1) {
      return accum[0]
    } else {
      return Statement(accum)
    }
  }

}

final data class SetContainsToken(val a: Token, val op: Token, val b: Token) : Token {
  /**
   * Similar to Statement, all parts must be static for the whole to be static
   */
  override fun isStatic() = a.isStatic() && op.isStatic() && b.isStatic()
  override fun build(): String = "${a.build()} ${op.build()} parser(${b.build()})"
  override fun renderWith(renderer: Renderer): String = "${a.renderWith(renderer)} ${op.renderWith(renderer)} (${b.renderWith(renderer)})"
}

val Token.token get(): Token = this
val String.token get() = StringToken(this)

fun <T> List<T>.token(elemTokenizer: (T) -> Token): Statement = this.map(elemTokenizer).mkStmt()
fun List<Token>.mkStmt(sep: String = ", ", before: String = "", after: String = ""): Statement = run {
  val interspersed =
    if (this.isEmpty())
      Statement(listOf())
    else {
      val sepList = List(this.size - 1) { StringToken(sep) }
      Statement(this.intersperseWith(sepList))
    }

  val withPrepend = if (before.isNotEmpty()) interspersed.prepend(before.token) else interspersed
  val withAppend = if (after.isNotEmpty()) withPrepend.append(after.token) else withPrepend
  withAppend
}
