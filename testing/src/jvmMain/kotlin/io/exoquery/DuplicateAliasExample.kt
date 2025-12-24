package io.exoquery

/**
 * BUG REPRODUCTION: Lambda parameter names ignored in composeFrom.join subquery aliases
 *
 * When using composeFrom.join with filtered subqueries, both subqueries get the same
 * alias ("it") despite having explicit lambda parameter names ("b", "c").
 *
 * EXPECTED: Subqueries get distinct aliases matching lambda parameters (AS b, AS c)
 * ACTUAL:   Both subqueries get alias "it" causing SQL naming conflict
 *
 * The lambda parameter name only affects the ON clause reference, not the subquery alias.
 */
fun main() {
  data class A(val id: Long, val bId: Long, val cId: Long)
  data class B(val id: Long, val status: String)
  data class C(val id: Long, val status: String)

  data class Result(val aId: Long, val bId: Long, val cId: Long)

  @SqlFragment
  fun A.activeB() = sql {
    composeFrom.join(Table<B>().filter { it.status == "active" }) { b -> b.id == this@activeB.bId }
  }

  @SqlFragment
  fun A.activeC() = sql {
    composeFrom.join(Table<C>().filter { it.status == "active" }) { c -> c.id == this@activeC.cId }
  }

  val query = sql.select {
    val a = from(Table<A>())
    val b = from(a.activeB())
    val c = from(a.activeC())
    Result(a.id, b.id, c.id)
  }.dynamic()

  //println("----------------- XR ---------------\n" + query.xr.showRaw())
  println("----------------- SQL (SQLite) ---------------\n" + query.buildPrettyFor.Sqlite().value)
  //println("----------------- SQL (Postgres) ---------------\n" + query.buildPrettyFor.Postgres().value)
}
