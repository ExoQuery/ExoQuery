package io.exoquery.sql

import io.exoquery.xr.StatelessTransformer
import io.exoquery.xr.XR
import io.exoquery.xr.XR.Ident
import io.exoquery.xr.XRType
import io.exoquery.xr.XRType.Product.Meta

// If we run this right after SqlQuery we know that in every place with a single select-value it is a leaf clause e.g. `SELECT x FROM (SELECT p.name from Person p)) AS x`
// in that case we know that SelectValue(x) is a leaf clause that we should expand into a `x.value`.
// MAKE SURE THIS RUNS BEFORE ExpandNestedQueries otherwise it will be incorrect, it should only run for single-selects from atomic values,
// if the ExpandNestedQueries ran it could be a single field that is coming from a case class e.g. case class MySingleValue(stuff: Int) that is being selected from
// (Note, no such thing as a Naming strategy in Kotlin implementation, fields of property classes are renamed in the parsing phase)
class ValueizeSingleLeafSelects() : StatelessQueryTransformer() {
  val ValueFieldName = "value"

  protected fun productize(origType: XRType) =
    XRType.Product("<Value>", listOf(ValueFieldName to origType), Meta.Empty)

  protected fun productize(ast: Ident): Ident =
    Ident(ast.name, XRType.Product("<Value>", listOf(ValueFieldName to XRType.Value), Meta.Empty))

  protected fun valueize(ast: Ident): XR.Property =
    XR.Property(productize(ast), ValueFieldName, XR.HasRename.NotHas)

  private fun collectAliases(contexts: List<FromContext>): List<Ident> =
    contexts.flatMap {
      when (val c = it) {
        is TableContext -> listOf(c.aliasIdent())
        is QueryContext -> listOf(c.aliasIdent())
        is ExpressionContext -> listOf(c.aliasIdent())
        is FlatJoinContext -> collectAliases(listOf(c.from))
        else -> emptyList()
      }
    }

  // Turn every `SELECT primitive-x` into a `SELECT case-class-x.primitive-value`
  override protected fun expandNested(qRaw: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery {
    // get the alises before we transform (i.e. Valueize) the contexts inside turning the leaf-quat alises into product-quat alises
    val leafValuedFroms = collectAliases(qRaw.from).filterNot { it.type.isProduct() }
    // now transform the inner clauses
    val from = qRaw.from.map { expandContext(it) }
    val q = qRaw.copy(from = from) // replace the from-clauses with the new inner-queries

    // If there is one single select clause that has a primitive (i.e. Leaf) quat then we can alias it to "value" (or whatever the value of valueConstant is)
    // This is the case of `SELECT primitive FROM (SELECT p.age from Person p) AS primitive`
    // where we turn it into `SELECT p.name AS value FROM Person p`
    fun aliasSelects(selectValues: List<SelectValue>): List<SelectValue> =
      when {
        selectValues.size == 1 && selectValues.first().type.isLeaf() ->
          selectValues.first().let { sv ->
            // interesting to explore cases where it could already be a path e.g. it's a subselect like select { val a = from(...); a.b.c.atom }
            listOf(sv.copy(alias = sv.alias + ValueFieldName))
          }
        else -> selectValues
      }

    val valuizedQuery =
      if (leafValuedFroms.isNotEmpty()) {
        q.transformXR(object : StatelessTransformer {
          override fun invoke(xr: XR.Expression): XR.Expression =
          // TODO Are there situations where indiscrmimanantly replacing things like this is problematic?
          //      if we switch to beta reduction we need to collect the values first which is less efficient
          //      or perhaps the dangerous cases have already been taken care of by dealising
          //BetaReduction(xr, TypeBehavior.ReplaceWithReduction, CollectXR.byType<Ident>(xr)
          //  .filter { leafValuedFroms.contains(it) }
            //  .map { it to valueize(it) })

            when (xr) {
              is XR.Ident -> {
                if (leafValuedFroms.contains(xr)) valueize(xr) else xr
              }
              else -> super.invoke(xr)
            }
        })
      } else
        q

    // If we're in a subquery that's selecting a single value e.g. `SELECT p.name FROM Person p` change it to `SELECT p.name AS value FROM Person p` so that
    // when we get out of this recursion into the outer queries e.g. `SELECT x FROM (SELECT p.name /*here*/ as value FROM Person p) AS x` (technically this is invalid SQL but it is exactly what is going in in our ADTs)
    // we can in turn it into `SELECT x.value FROM (SELECT p.name as value FROM Person p) AS x`
    val newSelects = aliasSelects(valuizedQuery.select)
    val out = valuizedQuery.copy(select = newSelects)
    return out
  }

  // Turn every `FROM primitive-x` into a `FROM case-class(x.primitive)`
  override protected fun expandContext(s: FromContext): FromContext =
    super.expandContext(s).let { valueized ->
      when {
        valueized is QueryContext && valueized.type.isLeaf() ->
          QueryContext(valueized.query.transformType { productize(it) }, valueized.alias)
        else -> valueized
      }
    }
}
