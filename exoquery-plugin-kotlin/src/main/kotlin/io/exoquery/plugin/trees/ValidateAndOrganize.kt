package io.exoquery.plugin.trees

import io.exoquery.plugin.transform.CX
import io.exoquery.xr.SX
import io.exoquery.xr.SX.U
import io.exoquery.xr.SelectClause
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement

object ValidateAndOrganize {
  private interface Phase {
    data object BEGIN : Phase
    data object FROM : Phase
    data object JOIN : Phase
    data object MODIFIER : Phase // I.e. for group(+having)/sort/take/drop
  }

  // The "pure functiona' way to do this would be to have a State object that is copied with new values but since
  // everything here is completely private and the only way to interact with it is through the invoke function, we it should be fine for now.
  private class State(var phase: Phase = Phase.BEGIN, val clauses: MutableList<U.Assignment> = mutableListOf(), var where: SX.Where? = null, var groupBy: SX.GroupBy? = null, var having: SX.Having? = null, var sortBy: SX.SortBy? = null) {
    context(CX.Scope, CX.Parsing)
    fun validPhases(vararg validPhases: Phase) = { errorMsg: String, expr: IrElement ->
      if (!validPhases.contains(phase)) logger.error(errorMsg, expr)
    }

    fun setPhaseTo(phase: Phase) {
      this.phase = phase
    }

    context(CX.Scope, CX.Parsing)
    fun addFrom(from: SX.From, expr: IrElement) {
      validPhases(Phase.BEGIN, Phase.FROM)("Cannot add a FROM clause after a JOIN clause or any other kind of clause", expr)
      setPhaseTo(Phase.FROM)
      clauses += from
    }

    context(CX.Scope, CX.Parsing)
    fun addJoin(join: SX.Join, expr: IrElement) {
      validPhases(Phase.JOIN, Phase.FROM)("At least one FROM-clause is needed and can only add JOIN clauses after FROM and before any WHERE/GROUP/SORT clauses.", expr)
      setPhaseTo(Phase.JOIN)
      clauses += join
    }

    context(CX.Scope, CX.Parsing)
    fun addWhere(where: SX.Where, expr: IrElement) {
      //if (phase != Phase.JOIN && phase != Phase.FROM) error("", expr)
      //phase = Phase.MODIFIER
      validPhases(Phase.JOIN, Phase.FROM, Phase.MODIFIER)("Only one `WHERE` clause is allowed and it must be after from/join calls and before any group/sort clauses", expr)
      setPhaseTo(Phase.MODIFIER) // Basically `WHERE` is like it's on phase but we don't need to create one since it can only occur once
      this.where = where
    }

    context(CX.Scope, CX.Parsing)
    fun addGroupBy(groupBy: SX.GroupBy, expr: IrElement) {
      validPhases(Phase.JOIN, Phase.FROM, Phase.MODIFIER)("Only one `GROUP BY` clause is allowed an it must be after from/join calls and before any sort clauses", expr)
      setPhaseTo(Phase.MODIFIER)
      this.groupBy = groupBy
    }

    context(CX.Scope, CX.Parsing)
    fun addHaving(having: SX.Having, expr: IrElement) {
      validPhases(Phase.MODIFIER)("A `HAVING` clause can only be used if there is a `GROUP BY` clause and only one `HAVING` clause is allowed.", expr)
      if (groupBy == null) logger.error("A `HAVING` clause can only be used if there is a `GROUP BY` clause", expr)
      // Having is like where in that it can only occur once so no need to create a new phase for it
      this.having = having
    }

    context(CX.Scope, CX.Parsing)
    fun addSortBy(sortBy: SX.SortBy, expr: IrElement) {
      validPhases(Phase.JOIN, Phase.FROM, Phase.MODIFIER)("Only one `SORT BY` clause is allowed an it must be after from/join calls and before any group clauses", expr)
      setPhaseTo(Phase.MODIFIER)
      this.sortBy = sortBy
    }

    // Ignore other states for the arbitrary assignments. They can technically be between anything
    context(CX.Scope, CX.Parsing)
    fun addArbitraryAssignment(assignment: SX.ArbitraryAssignment, expr: IrElement) {
      clauses += assignment
    }
  }

  context(CX.Scope, CX.Parsing)
  operator fun invoke(statements: List<Pair<SX, IrStatement>>, ret: XR.Expression): SelectClause {
    val state = State()
    statements.forEach { (sx, stmt) ->
      when (sx) {
        is SX.From -> state.addFrom(sx, stmt)
        is SX.Join -> state.addJoin(sx, stmt)
        is SX.Where -> state.addWhere(sx, stmt)
        is SX.Having -> state.addHaving(sx, stmt)
        is SX.GroupBy -> state.addGroupBy(sx, stmt)
        is SX.SortBy -> state.addSortBy(sx, stmt)
        is SX.ArbitraryAssignment -> state.addArbitraryAssignment(sx, stmt)
      }
    }
    return SelectClause(state.clauses, state.where, state.having, state.groupBy, state.sortBy, ret, ret.type)
  }

}
