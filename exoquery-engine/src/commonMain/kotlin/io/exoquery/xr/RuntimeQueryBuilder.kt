package io.exoquery.xr

import io.exoquery.ContainerOfFunXR
import io.exoquery.ContainerOfXR
import io.exoquery.Param
import io.exoquery.ParamSet
import io.exoquery.SqlAction
import io.exoquery.SqlBatchAction
import io.exoquery.TransformXrError
import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.ParamBatchToken
import io.exoquery.lang.ParamMultiToken
import io.exoquery.lang.ParamSingleToken
import io.exoquery.lang.SqlIdiom
import io.exoquery.lang.SqlQueryModel
import io.exoquery.lang.StatelessTokenTransformer
import io.exoquery.lang.Token
import io.exoquery.util.formatQuery

@ExoInternal
class RuntimeBuilder(val dialect: SqlIdiom, val pretty: Boolean) {
  data class ContainerBuildQuery(val queryString: String, val queryTokenized: Token, val queryModel: SqlQueryModel)
  data class ContainerBuildAction(val queryString: String, val queryTokenized: Token)


  private fun <Other> processContainer(container: ContainerOfXR, tokenize: (XR) -> Pair<Token, Other>): Triple<String, Token, Other> {
    // First thing we need to do is dedupe any runtime binds. We need to do this while the whole AST is still hierarchically
    // organized i.e. before flattening. This happens because in dynamic situations that collect and then combine multiple queries e.g:
    // List("a", "b").map(a => quote { query[Person].filter(p => p.name == lift(a)) }).reduce(_ ++ _)
    // since the lift(a) UID is determined at compile-time the trees of both "a", and "b" variants will have the same value for it
    // resulting in a tree that looks like this (before being flattened into a single filter which happens during query compilation):
    // UnionAll(
    //   Filter(Entity("Person"), BinaryOperation(p.name, ==, ScalarTag("SOME_UUID"))
    //   Filter(Entity("Person"), BinaryOperation(p.name, ==, ScalarTag("SOME_UUID"))
    // )
    // This will result in incorrect queries. Therefore we need to dedupe the runtime the "SOME_UUID" binds here.
    val quoted = container.rekeyRuntimeBinds()

    val (splicedAst, allParams) = quoted.spliceQuotations()

    val (queryTokenizedRaw, other) = tokenize(splicedAst)

    // "realize" the tokens converting params clauses to actual list-values
    val queryTokenized = TokenRealizer(ParamSet(allParams)).invoke(queryTokenizedRaw)

    val queryRaw =
      try {
        queryTokenized.build()
      } catch (e: TransformXrError) {
        throw TransformXrError("Failed to build query from tokenized AST:\n${splicedAst.showRaw()}\n--------- With Params ---------\n${allParams}", e)
      }

    val queryString = if (pretty) formatQuery(queryRaw) else queryRaw
    return Triple(queryString, queryTokenized, other)
  }

  fun forQuery(container: ContainerOfFunXR): ContainerBuildQuery {
    val (queryString, queryToken, queryModel) =
      processContainer(container) { splicedAst ->
        when (splicedAst) {
          // If it is an expression attempt to convert into a query-type and run it. There are situations in which toExpr is introduced around a query etc... and we can handle that in SqlQuery processing
          is XR.Expression -> dialect.processQuery(splicedAst.asQuery())
          is XR.Query -> dialect.processQuery(splicedAst)
          else -> throw IllegalArgumentException("Unsupported XR type. Can only be a XR.Query: ${splicedAst::class}\n${splicedAst.showRaw()}")
        }
      }

    return ContainerBuildQuery(queryString, queryToken, queryModel)
  }

  fun forAction(container: SqlAction<*, *>): ContainerBuildAction {
    val (queryString, queryToken, _) =
      processContainer(container) { splicedAst ->
        when (splicedAst) {
          is XR.Action -> dialect.processAction(splicedAst) to null
          else -> throw IllegalArgumentException("Unsupported XR type. Can only be a XR.Action: ${splicedAst::class}\n${splicedAst.showRaw()}")
        }
      }

    return ContainerBuildAction(queryString, queryToken)
  }

  fun forBatching(container: SqlBatchAction<*, *, *>): ContainerBuildAction {
    val (queryString, queryToken, _) =
      processContainer(container) { splicedAst ->
        when (splicedAst) {
          is XR.Batching -> dialect.processAction(splicedAst.action) to null
          else -> throw IllegalArgumentException("Unsupported XR type. Can only be a XR.Batching: ${splicedAst::class}\n${splicedAst.showRaw()}")
        }
      }

    return ContainerBuildAction(queryString, queryToken)
  }

  class TokenRealizer(val paramSet: ParamSet) : StatelessTokenTransformer {
    override fun invoke(token: ParamMultiToken): Token = token.realize(paramSet)
    override fun invoke(token: ParamSingleToken): Token = token.realize(paramSet)
    override fun invoke(token: ParamBatchToken): Token = token.realize(paramSet)
  }
}

fun ContainerOfXR.spliceQuotations(): Pair<XR, List<Param<*>>> {
  // While we're recursing through the nested containers and splicing them, collect all the parameters
  val collectedParams = mutableListOf<Param<*>>()

  fun spliceQuotationsRecurse(quoted: ContainerOfXR): XR {
    val quotationVases = quoted.runtimes.runtimes
    collectedParams.addAll(quoted.params.lifts)

    val ast = quoted.xr
    // Get all the quotation tags
    return TransformXR.build()
      .withQueryOf<XR.TagForSqlQuery> { tag ->
        quotationVases.find { it.first == tag.id }?.let { (id, vase) -> spliceQuotationsRecurse(vase) as XR.Query }
          ?: throw IllegalArgumentException("Query-Based vase with UID ${tag.id} could not be found!")
      }.withExpressionOf<XR.TagForSqlExpression> { tag ->
        quotationVases.find { it.first == tag.id }?.let { (id, vase) -> spliceQuotationsRecurse(vase) as XR.Expression }
          ?: throw IllegalArgumentException("Expression-Based vase with UID ${tag.id} could not be found!")
      }.withActionOf<XR.TagForSqlAction> { tag ->
        quotationVases.find { it.first == tag.id }?.let { (id, vase) -> spliceQuotationsRecurse(vase) as XR.Action }
          ?: throw IllegalArgumentException("Action-Based vase with UID ${tag.id} could not be found!")
      }
      // Eventually need to have a TagForBatching for batching to be able to transfer across dynamic queries
      .invoke(ast)
  }
  return BetaReduction.ofXR(spliceQuotationsRecurse(this)) to collectedParams.toList()
}
