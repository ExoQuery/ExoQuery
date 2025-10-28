package io.exoquery

import io.exoquery.annotation.CapturedFunction

data class PersonCrs(val id: Int, val name: String)
data class AddressCrs(val ownerId: Int, val street: String)


inline fun crossFileSelect() = sql.select {
  val p = from(Table<PersonCrs>())
  val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
  p to a
}

inline fun crossFileExpr() =
  sql {
    Table<PersonCrs>().filter { it.name == "JohnExpr"  }
  }

inline fun crossFileAction() =
  sql {
    insert<PersonCrs> { set(name to "JohnActionA") }
  }

// TODO try enabling this
//@PublishedApi internal
val batchInserts = listOf(PersonCrs(1, "JohnBatchA")).asSequence()

inline fun crossFileBatchAction(batchInserts: Sequence<PersonCrs>) =
  sql.batch(batchInserts) {
    insert<PersonCrs> { set(name to "JohnActionA") }
  }

@CapturedFunction
inline fun crossFileCapSelect(q: SqlQuery<PersonCrs>) =
  // TODO what happens if it's a sql.select here?
  sql {
    q.filter { p -> p.name == "JohnA" }
  }

//@CapturedDynamic
//inline fun crossFileDynSelect(q: SqlQuery<PersonCrs>) =
//  sql {
//    q.filter { p -> p.name == "JohnA" }
//  }
