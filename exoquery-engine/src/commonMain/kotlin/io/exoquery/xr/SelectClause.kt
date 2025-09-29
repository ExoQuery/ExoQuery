package io.exoquery.xr

import io.exoquery.pprint.PPrinterConfig
import io.exoquery.pprint.Tree
import io.exoquery.printing.PrintSkipLoc
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// TODO NEED TO TEST THIS
class StatefulSelectClauseTransformer<S>(private val delegate: StatefulTransformer<S>){
  private fun wrap(delegate: StatefulTransformer<S>) = StatefulSelectClauseTransformer<S>(delegate)

  fun invoke(selectClause: SelectClause): Pair<SelectClause, StatefulTransformer<S>> =
    with (selectClause) {
      val (assignmentsA, stateA) = applyList(assignments) { t, asi -> t.invoke(asi) }
      val (whereA, stateB) = where?.let { stateA.invoke(it) } ?: Pair(where, this@StatefulSelectClauseTransformer)
      val (havingA, stateB2) = having?.let { stateB.invoke(it) } ?: Pair(having, stateB)
      val (groupByA, stateC) = groupBy?.let { stateB2.invoke(it) } ?: Pair(groupBy, this@StatefulSelectClauseTransformer)
      val (sortByA, stateD) = sortBy?.let { stateC.invoke(it) } ?: Pair(sortBy, this@StatefulSelectClauseTransformer)
      val (selectA, stateE) = stateD.delegate.invoke(select)
      copy(
        assignments = assignmentsA,
        where = whereA,
        having = havingA,
        groupBy = groupByA,
        sortBy = sortByA,
        select = selectA
      ) to stateE
    }

  operator fun invoke(sx: SX.U.Assignment) =
    with (sx) {
      when (this) {
        is SX.From -> {
          val (aA, stateA) = delegate.invoke(xr)
          copy(xr = aA) to wrap(stateA)
        }
        is SX.Join -> {
          val (aA, stateA) = delegate.invoke(onQuery)
          val (aB, stateB) = stateA.invoke(condition)
          copy(onQuery = aA, condition = aB) to wrap(stateB)
        }
        is SX.ArbitraryAssignment -> {
          val (aA, stateA) = delegate.invoke(expression)
          copy(expression = aA) to wrap(stateA)
        }
      }
    }

  operator fun invoke(sx: SX.Where) =
    with (sx) {
      val (aA, stateA) = delegate.invoke(condition)
      copy(condition = aA) to wrap(stateA)
    }

  operator fun invoke(sx: SX.Having) =
    with (sx) {
      val (aA, stateA) = delegate.invoke(condition)
      copy(condition = aA) to wrap(stateA)
    }

  operator fun invoke(sx: SX.GroupBy) =
    with (sx) {
      val (aA, stateA) = delegate.invoke(grouping)
      copy(grouping = aA) to wrap(stateA)
    }

  operator fun invoke(sx: SX.SortBy) =
    with (sx) {
      val (newCri, state) = applyList(sx.criteria) { t, ord -> t.invoke(ord) }
      copy(criteria = newCri) to state
    }

  operator fun invoke(ord: XR.OrderField) =
    with(ord) {
      when (this) {
        is XR.OrderField.By -> {
          val (newField, state) = delegate.invoke(field)
          copy(field = newField) to wrap(state)
        }
        is XR.OrderField.Implicit -> {
          val (newField, state) = delegate.invoke(field)
          copy(field = newField) to wrap(state)
        }
      }
    }


  fun <U, R> applyList(list: List<U>, f: (StatefulSelectClauseTransformer<S>, U) -> Pair<R, StatefulSelectClauseTransformer<S>>): Pair<List<R>, StatefulSelectClauseTransformer<S>> {
    val (newList, transformer) =
      list.fold(Pair(mutableListOf<R>(), this)) { (values, t), v ->
        val (vt, vtt) = f(t, v)
        values += vt
        Pair(values, vtt)
      }

    return Pair(newList.toList(), transformer)
  }
}


// The structure should be:
// val from: SX.From, val joins: List<SX.JoinClause>, val where: SX.Where?, val groupBy: SX.GroupBy?, val sortBy: SX.SortBy?
@Serializable
data class SelectClause(
  val assignments: List<SX.U.Assignment>,
  val where: SX.Where?,
  val having: SX.Having?,
  val groupBy: SX.GroupBy?,
  val sortBy: SX.SortBy?,
  val select: XR.Expression,
  override val type: XRType,
  override val loc: XR.Location = XR.Location.Synth
) : XR.CustomQuery.Convertable {

  override fun toQueryXR(isOutermost: Boolean): XR.Query = SelectClauseToXR(this, isOutermost)
  fun allComponents(): List<SX> = assignments + listOfNotNull(where, groupBy, having, sortBy)

  // Do nothing for now, in the cuture recurse in queries and expressions inside the SX clauses
  override fun handleStatelessTransform(t: StatelessTransformer): XR.CustomQuery.Convertable =
    copy(
      assignments.map {
        when (it) {
          is SX.From -> {
            val from = it
            from.copy(t.invokeIdent(from.variable), t(from.xr))
          }
          is SX.Join -> {
            val join = it
            join.copy(variable = t.invokeIdent(join.variable), onQuery = t(join.onQuery), conditionVariable = t.invokeIdent(join.conditionVariable), condition = t(join.condition))
          }
          is SX.ArbitraryAssignment -> {
            val assignment = it
            assignment.copy(variable = t.invokeIdent(assignment.variable), expression = t(assignment.expression))
          }
        }
      },
      where?.let { where -> where.copy(t(where.condition)) },
      having?.let { having -> having.copy(t(having.condition)) },
      groupBy?.let { groupBy -> groupBy.copy(t(groupBy.grouping)) },
      sortBy?.let { sortBy ->
        val newCriteria = sortBy.criteria.map { ord -> ord.transform { t.invoke(it) } }
        sortBy.copy(criteria = newCriteria)
      },
      t(select)
    )


  override fun <S> handleStatefulTransformer(transformer: StatefulTransformer<S>): Pair<XR.CustomQuery.Convertable, StatefulTransformer<S>> =
    StatefulSelectClauseTransformer(transformer).invoke(this)

  override fun showTree(config: PPrinterConfig): Tree = PrintSkipLoc<SelectClause>(serializer(), config).treeify(this, null, false, false)

  companion object {
    fun justSelect(select: XR.Expression, loc: XR.Location): SelectClause = SelectClause(emptyList(), null, null, null, null, select, select.type, loc)

    // A friendlier constructor for tests
    fun of(
      assignments: List<SX.U.Assignment>,
      where: SX.Where? = null,
      having: SX.Having? = null,
      groupBy: SX.GroupBy? = null,
      sortBy: SX.SortBy? = null,
      select: XR.Expression,
      type: XRType,
      loc: XR.Location = XR.Location.Synth
    ): SelectClause = SelectClause(assignments, where, having, groupBy, sortBy, select, type, loc)
  }

  /** Do the equivalent of [io.exoquery.norm.NormalizeCustomQueries] for user introspection. Assume the segment being acted on is the top-level query */
  fun toXrTransform(): XR.Query = SelectClauseToXR(this, true)
  fun toXrRef(): XR.CustomQueryRef = XR.CustomQueryRef(this)


  data class Id(val assignments: List<SX.U.Assignment>, val where: SX.Where?, val having: SX.Having?, val groupBy: SX.GroupBy?, val sortBy: SX.SortBy?, val select: XR.Expression)

  @Transient
  val id = Id(assignments, where, having, groupBy, sortBy, select)
  override fun equals(other: Any?): Boolean = (this === other) || (other is SelectClause && id == other.id)
  override fun hashCode(): Int = id.hashCode()
}
