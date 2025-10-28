package io.exoquery.lang

import io.decomat.Is
import io.decomat.Pattern
import io.decomat.Pattern2
import io.decomat.Typed
import io.exoquery.xr.CollectXR
import io.exoquery.xr._And_
import io.exoquery.xr.XR
import io.exoquery.xrError

data class LayerComponents(
  val grouping: SqlQueryApply.Layer.Grouping?,
  val sorting: SqlQueryApply.Layer.Sorting?,
  val filtering: SqlQueryApply.Layer.Filtering?,
  val having: SqlQueryApply.Layer.Having? = null
)

fun List<SqlQueryApply.Layer>.findComponentsOrNull(): LayerComponents {
  val groupings = this.mapNotNull { if (it is SqlQueryApply.Layer.Grouping) it else null }
  if (groupings.size > 1) xrError("Multiple groupings detected, this is illegal:\n" + groupings.map { it.groupBy }.joinToString("\n"))

  val sortings = this.mapNotNull { if (it is SqlQueryApply.Layer.Sorting) it else null }
  if (sortings.size > 1) xrError("Multiple sortings detected, this is illegal:\n" + sortings.map { it.criteria }.joinToString("\n"))

  // Having multiple filterings is actually fine, we can combine them with &&
  val filterings = this.mapNotNull { if (it is SqlQueryApply.Layer.Filtering) it else null }
  val filtering = if (filterings.isEmpty()) null else filterings.reduce { a, b -> a combine b }

  val havings = this.mapNotNull { if (it is SqlQueryApply.Layer.Having) it else null }
  if (havings.size > 1) xrError("Multiple havings detected, this is illegal:\n" + havings.map { it.condition }.joinToString("\n"))

  return LayerComponents(groupings.firstOrNull(), sortings.firstOrNull(), filtering, havings.firstOrNull())
}

fun XR.Query.findFlatUnits(): Triple<List<XR.FlatGroupBy>, List<XR.FlatSortBy>, List<XR.FlatFilter>> =
  Triple(
    CollectXR.byType<XR.FlatGroupBy>(this),
    CollectXR.byType<XR.FlatSortBy>(this),
    CollectXR.byType<XR.FlatFilter>(this)
  )


fun combineWhereClauses(expr: XR.Expression?, combineWith: XR.Expression) =
  if (expr != null) expr _And_ combineWith
  else combineWith


// Define decomat extensions for selectvalue manually since the codegen doesn't seem to work right with things like generics e.g. turns List<String> into List<*>
class SelectValue_M<A : Pattern<AP>, B : Pattern<BP>, AP : XR.Expression, BP : List<String>>(a: A, b: B) : Pattern2<A, B, AP, BP, SelectValue>(a, b, Typed<SelectValue>())

operator fun <A : Pattern<AP>, B : Pattern<BP>, AP : XR.Expression, BP : List<String>> SelectValue.Companion.get(a: A, b: B) = SelectValue_M(a, b)
val SelectValue.Companion.Is get() = Is<SelectValue>()
