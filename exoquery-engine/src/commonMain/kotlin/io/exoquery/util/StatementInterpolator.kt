package io.exoquery.util

import io.exoquery.lang.Statement
import io.exoquery.lang.StringToken
import io.exoquery.lang.Token
import io.exoquery.lang.token
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.InterpolatorBackend
import io.exoquery.terpal.InterpolatorFunction
import io.exoquery.terpal.InterpolatorWithWrapper

@InterpolatorFunction<stmt>(stmt::class)
operator fun String.unaryPlus(): Statement = error("THe unaryPlus interpolator was not inlined by the compiler plugin. Is Terpal on the plugin path?")

val emptyStatement: Statement = +""
val externalStatement: Statement = +"?"

object stmt : InterpolatorWithWrapper<Token, Statement> {
  public operator fun invoke(string: kotlin.String): Statement = error("The invoke function was not inlined by the compiler plugin. Is Terpal on the plugin path?")

  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<Token>): Statement {
    //checkLengths(args, sc.parts)
    val partsIterator = parts().iterator()
    val argsIterator = params().iterator()
    val bldr = mutableListOf<Token>()
    bldr += StringToken(partsIterator.next())
    while (argsIterator.hasNext()) {
      bldr += argsIterator.next()
      bldr += StringToken(partsIterator.next())
    }
    val tokens = flatten(bldr)
    return Statement(tokens)
  }

  private fun flatten(tokens: List<Token>): List<Token> {
    fun unnestStatements(tokens: List<Token>): List<Token> {


      fun loop(acc: MutableList<Token>, rest: List<Token>): List<Token> = rest.run {
        when {
          none() -> acc
          head is Statement -> loop(acc, (head as Statement).tokens + tail)
          else -> loop(acc.withMore(head), tail)
        }
      }

      return loop(mutableListOf(), tokens)
    }

    fun mergeStringTokens(tokens: List<Token>): List<Token> {
      val (resultBuilder, leftTokens) =
        tokens.fold((mutableListOf<Token>() to mutableListOf<String>())) { (builder, acc), token ->
          when {
            token is StringToken -> {
              val str = token.string
              if (str.isNotEmpty()) {
                acc += token.string
              }
              (builder to acc)
            }
            acc.isEmpty() -> {
              builder += token.token
              (builder to mutableListOf())
            }
            else -> {
              builder += StringToken(acc.mkString())
              builder += token.token
              (builder to mutableListOf())
            }
          }
        }
      if (leftTokens.isNotEmpty()) {
        resultBuilder += StringToken(leftTokens.mkString())
      }
      return resultBuilder
    }

    val output =
      ::unnestStatements
        .andThen(::mergeStringTokens)
        .invoke(tokens)

    return output
  }

  override fun inlined(value: String?): Token = value?.token ?: "null".token
  override fun wrap(value: String?): Token = value?.token ?: "null".token
  override fun wrap(value: Int?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Long?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Short?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Byte?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Float?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Double?): Token = value?.toString()?.token ?: "null".token
  override fun wrap(value: Boolean?): Token = value?.toString()?.token ?: "null".token
}

inline fun <A, R, R1> ((A) -> R).andThen(crossinline f: (R) -> R1): (A) -> R1 =
  { a: A -> f(this.invoke(a)) }
