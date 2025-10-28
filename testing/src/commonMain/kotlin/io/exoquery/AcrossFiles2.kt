package io.exoquery

import io.exoquery.annotation.CapturedFunction


inline fun crossFileSelectExpr() = sql {
  crossFileSelect().filter { pair ->  pair.first.id > 1 }
}

inline fun crossFileSelectSelect () = sql.select {
  // TODO fails when .nested() is removed. Need to look into why
  val p = from(crossFileSelect().nested())
  val a = join(Table<AddressCrs>()) { it.ownerId == p.first.id }
  p to a
}

@CapturedFunction
inline fun crossFileCapSelectCapExpr(q: SqlQuery<PersonCrs>) = sql {
  crossFileCapSelect(Table<PersonCrs>().filter { p -> p.name == "JohnB" })
}

@CapturedFunction
inline fun crossFileCapSelectCapSelect(q: SqlQuery<PersonCrs>) = sql.select {
  val p = from(crossFileCapSelect(q))
  val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
  p to a
}

//@CapturedDynamic
//inline fun crossFileDynSelectDynSelect(q: SqlQuery<PersonCrs>) = sql.select {
//  val p = from(crossFileDynSelect(q))
//  val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
//  p to a
//}

inline fun crossFileExprExpr() =
  sql {
    crossFileExpr().filter { it.name == "JohnExprExpr"  }
  }

inline fun crossFileActionAction() =
  crossFileAction()

inline fun crossFileBatchActionAction(batchInserts: Sequence<PersonCrs>) =
  crossFileBatchAction(batchInserts)
