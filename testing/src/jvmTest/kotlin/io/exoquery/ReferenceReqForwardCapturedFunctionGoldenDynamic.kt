package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ReferenceReqForwardCapturedFunctionGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "in object/using ahead object/XR" to kt(
      "select { val p = from({ filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(Jack)); p }"
    ),
    "in object/using ahead object/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Jack'"
    ),
    "in object/using ahead object/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested/XR" to kt(
      "select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(Mack)); p }"
    ),
    "in object/using ahead object with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Mack'"
    ),
    "in object/using ahead object with nested/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 2x/XR" to kt(
      """select { val p = from({ filter -> { filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(filter).filter { p -> p.name == TagP("0") } }.toQuery.apply(Abe)); p }"""
    ),
    "in object/using ahead object with nested 2x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Abe' AND p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 2x/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 3x/XR" to kt(
      """select { val p = from({ filter -> { filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(filter).filter { p -> p.name == TagP("0") } }.toQuery.apply(JoeJoe)); p }"""
    ),
    "in object/using ahead object with nested 3x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'JoeJoe' AND p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 3x/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class/XR" to kt(
      """select { val p = from({ filter -> Table(Person).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(Sam)); p }"""
    ),
    "in class/using ahead class/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe1} AND p.name = 'Sam'",
      "0" to "Joe1"
    ),
    "in class/using ahead class/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested/XR" to kt(
      """select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(Sammy)); p }"""
    ),
    "in class/using ahead class with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe2} AND p.name = 'Sammy'",
      "0" to "Joe2"
    ),
    "in class/using ahead class with nested/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested 1/XR" to kt(
      """select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == TagP("1") || p.name == filter } }.toQuery.apply(filter).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(Samson)); p }"""
    ),
    "in class/using ahead class with nested 1/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE (p.name = {0:JoeA3} OR p.name = 'Samson') AND p.name = {1:JoeB3} AND p.name = 'Samson'",
      "0" to "JoeA3", "1" to "JoeB3"
    ),
    "in class/using ahead class with nested 1/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object/XR" to kt(
      "select { val p = from({ filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(Jack)); p }"
    ),
    "in object/using ahead object/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Jack'"
    ),
    "in object/using ahead object/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested/XR" to kt(
      "select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(Mack)); p }"
    ),
    "in object/using ahead object with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Mack'"
    ),
    "in object/using ahead object with nested/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 2x/XR" to kt(
      """select { val p = from({ filter -> { filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(filter).filter { p -> p.name == TagP("0") } }.toQuery.apply(Abe)); p }"""
    ),
    "in object/using ahead object with nested 2x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Abe' AND p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 2x/Phase" to cr(
      "CompileTime"
    ),
    "in object/using ahead object with nested 3x/XR" to kt(
      """select { val p = from({ filter -> { filter -> { filter -> Table(Person).filter { p -> p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(filter).filter { p -> p.name == TagP("0") } }.toQuery.apply(JoeJoe)); p }"""
    ),
    "in object/using ahead object with nested 3x/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'JoeJoe' AND p.name = {0:JoeJoe}",
      "0" to "JoeJoe"
    ),
    "in object/using ahead object with nested 3x/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class/XR" to kt(
      """select { val p = from({ filter -> Table(Person).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(Sam)); p }"""
    ),
    "in class/using ahead class/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe1} AND p.name = 'Sam'",
      "0" to "Joe1"
    ),
    "in class/using ahead class/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested/XR" to kt(
      """select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(filter) }.toQuery.apply(Sammy)); p }"""
    ),
    "in class/using ahead class with nested/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe2} AND p.name = 'Sammy'",
      "0" to "Joe2"
    ),
    "in class/using ahead class with nested/Phase" to cr(
      "CompileTime"
    ),
    "in class/using ahead class with nested 1/XR" to kt(
      """select { val p = from({ filter -> { filter -> Table(Person).filter { p -> p.name == TagP("1") || p.name == filter } }.toQuery.apply(filter).filter { p -> p.name == TagP("0") && p.name == filter } }.toQuery.apply(Samson)); p }"""
    ),
    "in class/using ahead class with nested 1/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE (p.name = {0:JoeA3} OR p.name = 'Samson') AND p.name = {1:JoeB3} AND p.name = 'Samson'",
      "0" to "JoeA3", "1" to "JoeB3"
    ),
    "in class/using ahead class with nested 1/Phase" to cr(
      "CompileTime"
    ),
  )
}
