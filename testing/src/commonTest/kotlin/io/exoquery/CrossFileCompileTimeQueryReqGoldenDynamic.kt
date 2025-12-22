package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object CrossFileCompileTimeQueryReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units/XR" to kt(
      "select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.name == JoeOuter }"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units/SQL" to cr(
      "SELECT pair.first_id AS id, pair.first_name AS name, pair.second_ownerId AS ownerId, pair.second_street AS street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS pair WHERE pair.first_name = 'JoeOuter'"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units/XR" to kt(
      "select { val p = from(select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.nested); val a = join(Table(AddressCrs)) { a.ownerId == p.first.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.first.name == JoeOuter }"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units/SQL" to cr(
      "SELECT pair.first_first_id AS id, pair.first_first_name AS name, pair.first_second_ownerId AS ownerId, pair.first_second_street AS street, pair.second_ownerId AS ownerId, pair.second_street AS street FROM (SELECT p.first_id AS first_first_id, p.first_name AS first_first_name, p.second_ownerId AS first_second_ownerId, p.second_street AS first_second_street, a.ownerId AS second_ownerId, a.street AS second_street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS p INNER JOIN AddressCrs a ON a.ownerId = p.first_id) AS pair WHERE pair.first_first_name = 'JoeOuter'"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/regular functions/(1) inline functions defined in previous-previous compilation units/XR" to kt(
      "select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.id > 1 }.filter { pair -> pair.first.name == JoeOuter }"
    ),
    "compile-time queries should work for/regular functions/(1) inline functions defined in previous-previous compilation units/SQL" to cr(
      "SELECT pair.first_id AS id, pair.first_name AS name, pair.second_ownerId AS ownerId, pair.second_street AS street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS pair WHERE pair.first_id > 1 AND pair.first_name = 'JoeOuter'"
    ),
    "compile-time queries should work for/regular functions/(1) inline functions defined in previous-previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units used directly/XR" to kt(
      "select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units used directly/SQL" to cr(
      "SELECT p.id, p.name, a.ownerId, a.street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous compilation units used directly/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units used directly/XR" to kt(
      "select { val p = from(select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.nested); val a = join(Table(AddressCrs)) { a.ownerId == p.first.id }; Tuple(first = p, second = a) }"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units used directly/SQL" to cr(
      "SELECT p.first_id AS id, p.first_name AS name, p.second_ownerId AS ownerId, p.second_street AS street, a.ownerId, a.street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS p INNER JOIN AddressCrs a ON a.ownerId = p.first_id"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous compilation units used directly/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous(expr) compilation units used directly/XR" to kt(
      "select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.id > 1 }"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous(expr) compilation units used directly/SQL" to cr(
      "SELECT pair.first_id AS id, pair.first_name AS name, pair.second_ownerId AS ownerId, pair.second_street AS street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS pair WHERE pair.first_id > 1"
    ),
    "compile-time queries should work for/regular functions/inline functions defined in previous-previous(expr) compilation units used directly/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous compilation units/XR" to kt(
      "{ q -> q.filter { p -> p.name == JohnA } }.toQuery.apply(Table(PersonCrs).filter { p -> p.name == JoeInner }).filter { pair -> pair.name == JoeOuter }"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous compilation units/SQL" to cr(
      "SELECT p1.id, p1.name FROM PersonCrs p1 WHERE p1.name = 'JoeInner' AND p1.name = 'JohnA' AND p1.name = 'JoeOuter'"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous compilation units/XR" to kt(
      "{ q -> select { val p = from({ q -> q.filter { p -> p.name == JohnA } }.toQuery.apply(q)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) } }.toQuery.apply(Table(PersonCrs).filter { p -> p.name == JoeInner }).filter { pair -> pair.first.name == JoeOuter }"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous compilation units/SQL" to cr(
      "SELECT p1.id, p1.name, a.ownerId, a.street FROM PersonCrs p1 INNER JOIN AddressCrs a ON a.ownerId = p1.id WHERE p1.name = 'JoeInner' AND p1.name = 'JohnA' AND p1.name = 'JoeOuter'"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous(expr) compilation units/XR" to kt(
      "{ q -> { q -> q.filter { p -> p.name == JohnA } }.toQuery.apply(Table(PersonCrs).filter { p -> p.name == JohnB }) }.toQuery.apply(Table(PersonCrs).filter { p -> p.name == JoeInner }).filter { pair -> pair.name == JoeOuter }"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous(expr) compilation units/SQL" to cr(
      "SELECT p.id, p.name FROM PersonCrs p WHERE p.name = 'JohnB' AND p.name = 'JohnA' AND p.name = 'JoeOuter'"
    ),
    "compile-time queries should work for/captured functions/inline captured functions defined in previous-previous(expr) compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units/XR" to kt(
      "Table(PersonCrs).filter { it -> it.name == JohnExpr }.filter { it -> it.name == JoeExprOuter }"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units/SQL" to cr(
      "SELECT it.id, it.name FROM PersonCrs it WHERE it.name = 'JohnExpr' AND it.name = 'JoeExprOuter'"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units/XR" to kt(
      "Table(PersonCrs).filter { it -> it.name == JohnExpr }.filter { it -> it.name == JohnExprExpr }.filter { it -> it.name == JoeExprOuter }"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units/SQL" to cr(
      "SELECT it.id, it.name FROM PersonCrs it WHERE it.name = 'JohnExpr' AND it.name = 'JohnExprExpr' AND it.name = 'JoeExprOuter'"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units/Phase" to cr(
      "CompileTime"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units used directly/XR" to kt(
      "Table(PersonCrs).filter { it -> it.name == JohnExpr }"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units used directly/SQL" to cr(
      "SELECT it.id, it.name FROM PersonCrs it WHERE it.name = 'JohnExpr'"
    ),
    "compile-time expressions should work for/inline functions defined in previous compilation units used directly/Phase" to cr(
      "CompileTime"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units used directly/XR" to kt(
      "Table(PersonCrs).filter { it -> it.name == JohnExpr }.filter { it -> it.name == JohnExprExpr }"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units used directly/SQL" to cr(
      "SELECT it.id, it.name FROM PersonCrs it WHERE it.name = 'JohnExpr' AND it.name = 'JohnExprExpr'"
    ),
    "compile-time expressions should work for/inline functions defined in previous-previous compilation units used directly/Phase" to cr(
      "CompileTime"
    ),
  )
}
