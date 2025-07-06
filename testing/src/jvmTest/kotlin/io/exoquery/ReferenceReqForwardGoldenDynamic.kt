package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ReferenceReqForwardGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "in object/using ahead object/XR" to kt(
      "select { val p = from(Table(Person)); p }"
    ),
    "in object/using ahead object/SQL" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "in object/using ahead object/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested/XR" to kt(
      "select { val p = from(Table(Person)); p }"
    ),
    "in object/using ahead object with nested/SQL" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "in object/using ahead object with nested/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 2x/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in object/using ahead object with nested 2x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 2x/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 3x/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in object/using ahead object with nested 3x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 3x/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe}",
      "0" to "Joe"
    ),
    "in class/using ahead class/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe}",
      "0" to "Joe"
    ),
    "in class/using ahead class with nested/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested 1/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("1") }.filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class with nested 1/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe} AND p.name = {1:Joe}",
      "0" to "Joe", "1" to "Joe"
    ),
    "in class/using ahead class with nested 1/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object/XR" to kt(
      "select { val p = from(Table(Person)); p }"
    ),
    "in object/using ahead object/SQL" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "in object/using ahead object/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested/XR" to kt(
      "select { val p = from(Table(Person)); p }"
    ),
    "in object/using ahead object with nested/SQL" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "in object/using ahead object with nested/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 2x/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in object/using ahead object with nested 2x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 2x/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 3x/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in object/using ahead object with nested 3x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 3x/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe}",
      "0" to "Joe"
    ),
    "in class/using ahead class/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe}",
      "0" to "Joe"
    ),
    "in class/using ahead class with nested/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested 1/XR" to kt(
      """select { val p = from(Table(Person).filter { p -> p.name == TagP("1") }.filter { p -> p.name == TagP("0") }); p }"""
    ),
    "in class/using ahead class with nested 1/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe} AND p.name = {1:Joe}",
      "0" to "Joe", "1" to "Joe"
    ),
    "in class/using ahead class with nested 1/Phase" to cr(
      "CompileTime"
    ),
  )
}
