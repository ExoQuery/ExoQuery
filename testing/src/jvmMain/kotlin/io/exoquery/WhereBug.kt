@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class, TraceType.Standard::class)
package io.exoquery

import io.exoquery.annotation.SqlFragment
import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.TraceType

object WhereBug {
  data class A(val id: Int)
  data class B(val id: Int, val aId: Int)
  data class Composite(val a: A, val b: B)

  @SqlFragment fun joined(): SqlQuery<Composite> = sql.select {
    val a = from(Table<A>())
    val b = join(Table<B>()) { b -> b.aId == a.id }
    Composite(a, b)
  }

  @SqlFragment fun SqlQuery<Composite>.filtered(): SqlQuery<Composite> = sql {
    this@filtered.nested().filter { it.a.id > 0 }
  }

  val buggy = sql.select {
    val r = from(joined().filtered())
    where { r.b.id > 0 }  // triggers: FROM A a, (SELECT ... FROM INNER JOIN B ...)
    r.a.id
  }.dynamic()
}

fun main() {
  val query = WhereBug.buggy.buildPrettyFor.Postgres().value
  println(query)
  /*
  SELECT
    r.a_id AS value
  FROM
    A a,
    (
      SELECT
        a.id AS a_id,
        b.id AS b_id,
        b.aId AS b_aId
      FROM
        INNER JOIN B b ON b.aId = a.id <- INVALID! CAN'T DO "FROM INNER JOIN" WITHOUT A PRECEDING TABLE
    ) AS r
  WHERE
    r.a_id > 0
    AND r.b_id > 0
   */
}
