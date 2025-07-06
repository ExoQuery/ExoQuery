package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ExpressionFunctionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "String/toInt" to cr(
      "SELECT CAST(p.name AS INTEGER) AS value FROM Person p"
    ),
    "String/toLong" to cr(
      "SELECT CAST(p.name AS BIGINT) AS value FROM Person p"
    ),
    "String/toBoolean" to cr(
      "SELECT CAST(p.name AS BOOLEAN) AS value FROM Person p"
    ),
    "String/casting" to cr(
      "SELECT p.name || p.age AS value FROM Person p"
    ),
    "String/string concat" to cr(
      "SELECT p.name || ' ' || p.name AS value FROM Person p"
    ),
    "String/substr - regular" to cr(
      "SELECT SUBSTRING(p.name, 1, 2) AS value FROM Person p"
    ),
    "String/substr" to cr(
      "SELECT SUBSTRING(p.name, 1, 2) AS value FROM Person p"
    ),
    "String/left" to cr(
      "SELECT LEFT(p.name, 2) AS value FROM Person p"
    ),
    "String/right" to cr(
      "SELECT RIGHT(p.name, 2) AS value FROM Person p"
    ),
    "String/replace" to cr(
      "SELECT REPLACE(p.name, 'a', 'b') AS value FROM Person p"
    ),
    "String/upper - sql" to cr(
      "SELECT UPPER(p.name) AS value FROM Person p"
    ),
    "String/lower - sql" to cr(
      "SELECT LOWER(p.name) AS value FROM Person p"
    ),
    "StringInterpolation/multiple elements" to cr(
      "SELECT p.name || '-' || p.age AS value FROM Person p"
    ),
    "StringInterpolation/single element" to cr(
      "SELECT p.name AS value FROM Person p"
    ),
    "StringInterpolation/single constant" to cr(
      "SELECT 'constant' AS value FROM Person p"
    ),
    "StringInterpolation/many elements" to cr(
      "SELECT l.name || '-foo-' || l.age || '-bar-' || l.address || '-baz-' || l.phone || '-blin' AS value FROM LotsOfFields l"
    ),
    "Int/toLong" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Int/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
    "Int/toFloat" to cr(
      "SELECT CAST(p.age AS REAL) AS value FROM Person p"
    ),
    "Long/toInt" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Long/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
    "Long/toFloat" to cr(
      "SELECT CAST(p.age AS REAL) AS value FROM Person p"
    ),
    "Float/toInt" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Float/toLong" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Float/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
    "String/toInt" to cr(
      "SELECT CAST(p.name AS INTEGER) AS value FROM Person p"
    ),
    "String/toLong" to cr(
      "SELECT CAST(p.name AS BIGINT) AS value FROM Person p"
    ),
    "String/toBoolean" to cr(
      "SELECT CAST(p.name AS BOOLEAN) AS value FROM Person p"
    ),
    "String/casting" to cr(
      "SELECT p.name || p.age AS value FROM Person p"
    ),
    "String/string concat" to cr(
      "SELECT p.name || ' ' || p.name AS value FROM Person p"
    ),
    "String/substr - regular" to cr(
      "SELECT SUBSTRING(p.name, 1, 2) AS value FROM Person p"
    ),
    "String/substr" to cr(
      "SELECT SUBSTRING(p.name, 1, 2) AS value FROM Person p"
    ),
    "String/left" to cr(
      "SELECT LEFT(p.name, 2) AS value FROM Person p"
    ),
    "String/right" to cr(
      "SELECT RIGHT(p.name, 2) AS value FROM Person p"
    ),
    "String/replace" to cr(
      "SELECT REPLACE(p.name, 'a', 'b') AS value FROM Person p"
    ),
    "String/upper - sql" to cr(
      "SELECT UPPER(p.name) AS value FROM Person p"
    ),
    "String/lower - sql" to cr(
      "SELECT LOWER(p.name) AS value FROM Person p"
    ),
    "StringInterpolation/multiple elements" to cr(
      "SELECT p.name || '-' || p.age AS value FROM Person p"
    ),
    "StringInterpolation/single element" to cr(
      "SELECT p.name AS value FROM Person p"
    ),
    "StringInterpolation/single constant" to cr(
      "SELECT 'constant' AS value FROM Person p"
    ),
    "StringInterpolation/many elements" to cr(
      "SELECT l.name || '-foo-' || l.age || '-bar-' || l.address || '-baz-' || l.phone || '-blin' AS value FROM LotsOfFields l"
    ),
    "Int/toLong" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Int/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
    "Int/toFloat" to cr(
      "SELECT CAST(p.age AS REAL) AS value FROM Person p"
    ),
    "Long/toInt" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Long/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
    "Long/toFloat" to cr(
      "SELECT CAST(p.age AS REAL) AS value FROM Person p"
    ),
    "Float/toInt" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Float/toLong" to cr(
      "SELECT p.age AS value FROM Person p"
    ),
    "Float/toDouble" to cr(
      "SELECT CAST(p.age AS DOUBLE PRECISION) AS value FROM Person p"
    ),
  )
}
