package io.exoquery.sql

import io.exoquery.xr.XR

/**
 * Remove aliases at the top level of the AST since they are not needed
 * (quill uses select row indexes to figure out what data corresponds to what encodeable object)
 * as well as entities whose aliases are the same as their selection e.g. "select x.foo as foo"
 * since this just adds syntactic noise.
 */
class RemoveExtraAlias : StatelessQueryTransformer() {
  // Remove aliases that are the same as as the select values. Since a strategy may change the name,
  // use a heuristic where if the column naming strategy make the property name be different from the
  // alias, keep the column property name.
  // Note that in many cases e.g. tuple names _1,_2 etc... the column name will be rarely changed,
  // as a result of column capitalization, however it is possible that it will be changed as a result
  // of some other scheme (e.g. adding 'col' to every name where columns actually named _1,_2 become col_1,col_2)
  // and then unless the proper alias is there (e.g. foo.col_1 AS _1, foo.col_2 AS _2) subsequent selects will incorrectly
  // select _1.foo,_2.bar fields assuming the _1,_2 columns actually exist.
  // However, in situations where the actual column name is Fixed, these kinds of things typically
  // will not happen so we do not force the alias to happen.
  // Note that in certain situations this will happen anyway (e.g. if the user overwrites the tokenizeFixedColumn method
  // in SqlIdiom. In those kinds of situations we allow specifying the -Dquill.query.alwaysAlias
  // (Also note that if there is a @ExoField/@SerialName annotation on the field, it will be marked as having a rename
  // and in that case we *always* want to keep the alias since the original property will be quoted but aliases
  // should NEVER be quoted)

  private fun removeUnneededAlias(value: SelectValue): SelectValue =
    when {
      //case sv @ SelectValue(p: Property, alias :: Nil, _) && p.name == alias =>
      value.expr is XR.Property && value.alias.size == 1 && value.expr.name == value.alias.first() && value.expr.hasRename.hasOrNot() == false -> {
        value.copy(alias = listOf())
      }
      else -> value
    }

  override fun expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery {
    val from = q.from.map { expandContext(it) }
    val select = q.select.map { removeUnneededAlias(it) }
    return q.copy(select = select, from = from)
  }

}

//  override protected def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery = {
//    val from = q.from.map(expandContext(_))
//    val select = q.select.map(removeUnneededAlias(_))
//    q.copy(select = select, from = from)(q.quat)
//  }
//}
