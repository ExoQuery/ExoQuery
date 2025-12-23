package io.exoquery

import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.TraceType

object TestRepro2 {
  val nestedSelect2 = sql.select {
    val p = from(Table<PersonCrs>())
    val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
    p to a
  }

  val nestedSelect =
    sql.select {
      // TODO fails when .nested() is removed. Need to look into why
      val p = from(nestedSelect2.nested())
      val a = join(Table<AddressCrs>()) { it.ownerId == p.first.id }
      p to a
    }

  val q = sql {
    nestedSelect.filter { pair -> pair.first.first.name == "JoeOuter" }
    }.dynamic()

  val q2 = sql {
    crossFileSelectSelect().filter { pair -> pair.first.first.name == "JoeOuter" }
  }
}

fun main() {
  println("--------------- q ----------------")
  println(TestRepro2.q.build<PostgresDialect>().value)
  println("--------------- q2 ----------------")
  println(TestRepro2.q2.build<PostgresDialect>().value)
  println("--------------- done ----------------")
  println("SELECT a.first_first_id AS id, a.first_first_name AS name, a.first_second_ownerId AS ownerId, a.first_second_street AS street, a.second_ownerId AS ownerId, a.second_street AS street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS p, (SELECT p.id AS first_first_id, p.name AS first_first_name, p.ownerId AS first_second_ownerId, p.street AS first_second_street, a.ownerId AS second_ownerId, a.street AS second_street FROM INNER JOIN AddressCrs a ON a.ownerId = (p.id + 1)) AS a WHERE a.first_first_name = 'JoeOuter'")

}
