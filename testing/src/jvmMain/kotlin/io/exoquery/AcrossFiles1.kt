package io.exoquery

data class MyPersonC(val id: Int, val name: String)
data class MyAddressC(val ownerId: Int, val street: String)

val crossFile = capture.select {
  val p = from(Table<MyPersonC>())
  val a = join(Table<MyAddressC>()) { it.ownerId == p.id }
  p to a
}

fun local() {
  val result = crossFile.buildFor.Postgres()
  println(result.value)
}
