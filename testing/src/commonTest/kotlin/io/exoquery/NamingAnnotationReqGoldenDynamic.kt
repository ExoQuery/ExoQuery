package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object NamingAnnotationReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "quotation should/no apply quotes in postgres if all lower-case/XR" to kt(
      "Table(person_annotated).filter { it -> it.first_name == Joe && it.last_name == Bloggs }"
    ),
    "quotation should/no apply quotes in postgres if all lower-case/SQL" to cr(
      "SELECT it.first_name AS first_name, it.last_name AS last_name, it.age FROM person_annotated it WHERE it.first_name = 'Joe' AND it.last_name = 'Bloggs'"
    ),
    "quotation should/apply quotes in generic DBs if all lower-case/XR" to kt(
      "Table(person_annotated).filter { it -> it.first_name == Joe && it.last_name == Bloggs }"
    ),
    "quotation should/apply quotes in generic DBs if all lower-case/SQL" to cr(
      """SELECT it."first_name" AS first_name, it."last_name" AS last_name, it.age FROM "person_annotated" it WHERE it."first_name" = 'Joe' AND it."last_name" = 'Bloggs'"""
    ),
    "naming overrides should work in the correct order/in a query/XR" to kt(
      "Table(person_annotated).filter { it -> it.first_name == Joe && it.last_name == Bloggs }"
    ),
    "naming overrides should work in the correct order/in a query/SQL" to cr(
      "SELECT it.first_name AS first_name, it.last_name AS last_name, it.age FROM person_annotated it WHERE it.first_name = 'Joe' AND it.last_name = 'Bloggs'"
    ),
    "naming overrides should work in the correct order/in a query - with overrides/XR" to kt(
      "Table(PERSON_ANNOTATED).filter { it -> it.FIRST_NAME == Joe && it.last_name == Bloggs }"
    ),
    "naming overrides should work in the correct order/in a query - with overrides/SQL" to cr(
      """SELECT it."FIRST_NAME" AS FIRST_NAME, it.last_name AS last_name, it.age FROM "PERSON_ANNOTATED" it WHERE it."FIRST_NAME" = 'Joe' AND it.last_name = 'Bloggs'"""
    ),
    "naming overrides should work in the correct order/in an action/XR" to kt(
      "insert<person_annotated> { set(thisinsert.first_name to Joe, thisinsert.last_name to Bloggs, thisinsert.age to 42) }"
    ),
    "naming overrides should work in the correct order/in an action/SQL" to cr(
      "INSERT INTO person_annotated (first_name, last_name, age) VALUES ('Joe', 'Bloggs', 42)"
    ),
    "naming overrides should work in the correct order/in an action - with overrides/XR" to kt(
      "insert<PERSON_ANNOTATED> { set(thisinsert.FIRST_NAME to Joe, thisinsert.last_name to Bloggs, thisinsert.age to 42) }"
    ),
    "naming overrides should work in the correct order/in an action - with overrides/SQL" to cr(
      """INSERT INTO "PERSON_ANNOTATED" ("FIRST_NAME", last_name, age) VALUES ('Joe', 'Bloggs', 42)"""
    ),
    "naming overrides should work in the correct order/in an action with setParams/XR" to kt(
      """insert<person_annotated> { set(thisinsert.first_name to TagP("0"), thisinsert.last_name to TagP("1"), thisinsert.age to TagP("2")) }"""
    ),
    "naming overrides should work in the correct order/in an action with setParams/SQL" to cr(
      "INSERT INTO person_annotated (first_name, last_name, age) VALUES ({0:Joe}, {1:Bloggs}, {2:42})",
      "0" to "Joe", "1" to "Bloggs", "2" to "42"
    ),
    "naming overrides should work in the correct order/in an action with setParams - with overrides/XR" to kt(
      """insert<PERSON_ANNOTATED> { set(thisinsert.FIRST_NAME to TagP("0"), thisinsert.last_name to TagP("1"), thisinsert.age to TagP("2")) }"""
    ),
    "naming overrides should work in the correct order/in an action with setParams - with overrides/SQL" to cr(
      """INSERT INTO "PERSON_ANNOTATED" ("FIRST_NAME", last_name, age) VALUES ({0:Joe}, {1:Bloggs}, {2:42})""",
      "0" to "Joe", "1" to "Bloggs", "2" to "42"
    ),
  )
}
