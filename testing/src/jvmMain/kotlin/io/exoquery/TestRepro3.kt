@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class, TraceType.Standard::class)

package io.exoquery

import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.TraceType

object TestRepro3 {

  val nestedSelect =
    sql.select {
      val p = from(Table<PersonCrs>())
      val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
      p to a
    }

  val q = sql {
    nestedSelect.filter { pair -> pair.first.name == "JoeOuter" }
  }.dynamic()

}

fun main() {
  println("--------------- q ----------------")
  println(TestRepro3.q.build<PostgresDialect>().value)

  //println(TestRepro3.nestedSelect.build<PostgresDialect>().value)
}
