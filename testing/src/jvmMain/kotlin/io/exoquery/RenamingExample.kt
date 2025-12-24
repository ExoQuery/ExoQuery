package io.exoquery

fun main() {
  data class Emb(@ExoField("A") val a: Int, @ExoField("B") val b: Int)
  data class Parent(val id: Int, val emb1: Emb)

  val q = sql {
    Table<Emb>().map { e -> Parent(1, e) }.distinct()
  }.dynamic()
  println(q.buildPrettyFor.Postgres().value)
}
