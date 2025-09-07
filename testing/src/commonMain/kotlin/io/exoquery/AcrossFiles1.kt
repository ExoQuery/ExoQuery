package io.exoquery

import io.exoquery.annotation.CapturedDynamic
import io.exoquery.annotation.CapturedFunction

data class PersonCrs(val id: Int, val name: String)
data class AddressCrs(val ownerId: Int, val street: String)


inline fun crossFileSelect() = capture.select {
  val p = from(Table<PersonCrs>())
  val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
  p to a
}

inline fun crossFileExpr() =
  capture {
    Table<PersonCrs>().filter { it.name == "JohnExpr"  }
  }

inline fun crossFileAction() =
  capture {
    insert<PersonCrs> { set(name to "JohnActionA") }
  }

// TODO try enabling this
//@PublishedApi internal
val batchInserts = listOf(PersonCrs(1, "JohnBatchA")).asSequence()

inline fun crossFileBatchAction(batchInserts: Sequence<PersonCrs>) =
  capture.batch(batchInserts) {
    insert<PersonCrs> { set(name to "JohnActionA") }
  }

@CapturedFunction
inline fun crossFileCapSelect(q: SqlQuery<PersonCrs>) =
  // TODO what happens if it's a capture.select here?
  capture {
    q.filter { p -> p.name == "JohnA" }
  }

//@CapturedDynamic
//inline fun crossFileDynSelect(q: SqlQuery<PersonCrs>) =
//  capture {
//    q.filter { p -> p.name == "JohnA" }
//  }
