package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object VariableReductionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "using it-variable should reduce to the letter `a` on the other side of the clause/XR" to kt(
      "select { val p = from(Table(Person)); val a = join(Table(Address)) { p.id == a.ownerId }; Tuple(first = p, second = a) }"
    ),
    "using it-variable should reduce to the letter `a` on the other side of the clause/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p INNER JOIN Address a ON p.id = a.ownerId"
    ),
    "node-level deconstruction shuold work/XR" to kt(
      "Table(Pair).map { destruct -> { val p = destruct.first; val a = destruct.second; Tuple(first = p.name, second = a.street) } }"
    ),
    "node-level deconstruction shuold work/SQL" to cr(
      "SELECT destruct.name AS first, destruct.street AS second FROM Pair destruct"
    ),
    "leaf-level deconstruction should work/in map - single/XR" to kt(
      "Table(Person).map { destruct -> { val id = destruct.id; val name = destruct.name; val age = destruct.age; name } }"
    ),
    "leaf-level deconstruction should work/in map - single/SQL" to cr(
      "SELECT destruct.name AS value FROM Person destruct"
    ),
    "leaf-level deconstruction should work/in map - multi/XR" to kt(
      "Table(Person).map { destruct -> { val id = destruct.id; val name = destruct.name; val age = destruct.age; Tuple(first = name, second = age) } }"
    ),
    "leaf-level deconstruction should work/in map - multi/SQL" to cr(
      "SELECT destruct.name AS first, destruct.age AS second FROM Person destruct"
    ),
    "leaf-level deconstruction should work/in filter/XR" to kt(
      "Table(Person).filter { destruct -> { val id = destruct.id; val name = destruct.name; val age = destruct.age; name == Joe } }"
    ),
    "leaf-level deconstruction should work/in filter/SQL" to cr(
      "SELECT destruct.id, destruct.name, destruct.age FROM Person destruct WHERE destruct.name = 'Joe'"
    ),
  )
}
