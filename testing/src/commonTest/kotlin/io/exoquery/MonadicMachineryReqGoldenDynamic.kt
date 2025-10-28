package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object MonadicMachineryReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "sql expression (Row)->Table/XR" to kt(
      "select { val p = from(Table(Person)); val a = from({ p -> Table(Address).join { a -> p.id == a.ownerId }.toExpr }.apply(p)); val r = from({ p -> Table(Robot).join { r -> p.id == r.ownerId }.toExpr }.apply(p)); Triple(first = p, second = a, third = r) }"
    ),
    "sql expression (Row)->Table/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city, r.ownerId, r.name, r.model FROM Person p INNER JOIN Address a ON p.id = a.ownerId INNER JOIN Robot r ON p.id = r.ownerId"
    ),
    "sql expression (@Cap (Row)->Table) use/XR" to kt(
      "select { val p = from(Table(Person)); val a = from({ p -> Table(Address).join { a -> p.id == a.ownerId }.toExpr }.apply(p).toQuery); val r = from({ p -> Table(Robot).join { r -> p.id == r.ownerId }.toExpr }.apply(p).toQuery); Triple(first = p, second = a, third = r) }"
    ),
    "sql expression (@Cap (Row)->Table) use/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city, r.ownerId, r.name, r.model FROM Person p INNER JOIN Address a ON p.id = a.ownerId INNER JOIN Robot r ON p.id = r.ownerId"
    ),
    "sql expression use (@Cap (Row)()->Table)/XR" to kt(
      "select { val p = from(Table(Person)); val a = from({ this -> Table(Address).join { a -> this.id == a.ownerId }.toExpr }.apply(p).toQuery); val r = from({ this -> Table(Robot).join { r -> this.id == r.ownerId }.toExpr }.apply(p).toQuery); Triple(first = p, second = a, third = r) }"
    ),
    "sql expression use (@Cap (Row)()->Table)/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city, r.ownerId, r.name, r.model FROM Person p INNER JOIN Address a ON p.id = a.ownerId INNER JOIN Robot r ON p.id = r.ownerId"
    ),
    "sql (@Cap (Row)()->Table)/XR" to kt(
      "select { val p = from(Table(Person)); val a = from({ this -> Table(Address).join { a -> this.id == a.ownerId } }.toQuery.apply(p)); val r = from({ this -> Table(Robot).join { r -> this.id == r.ownerId } }.toQuery.apply(p)); Triple(first = p, second = a, third = r) }"
    ),
    "sql (@Cap (Row)()->Table)/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city, r.ownerId, r.name, r.model FROM Person p INNER JOIN Address a ON p.id = a.ownerId INNER JOIN Robot r ON p.id = r.ownerId"
    ),
  )
}
