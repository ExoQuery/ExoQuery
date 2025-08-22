package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ActionOnConflictReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "onConflictUpdate/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictUpdate(listOf(thishidden.id))(listOf(thishidden.name to excluding.name, thishidden.age to excluding.age))"
    ),
    "onConflictUpdate/SQL" to cr(
      "INSERT INTO Person AS x (name, age) VALUES ('Joe', 123) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age"
    ),
    "onConflictUpdate - complex/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictUpdate(listOf(thishidden.id))(listOf(thishidden.name to thisonconflict.name + excluding.name, thishidden.age to thisonconflict.age + excluding.age))"
    ),
    "onConflictUpdate - complex/SQL" to cr(
      "INSERT INTO Person AS x (name, age) VALUES ('Joe', 123) ON CONFLICT (id) DO UPDATE SET name = (x.name || EXCLUDED.name), age = (x.age + EXCLUDED.age)"
    ),
    "onConflictUpdate - complex + returning/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictUpdate(listOf(thishidden.id))(listOf(thishidden.name to thisonconflict.name + excluding.name, thishidden.age to thisonconflict.age + excluding.age)).returning { p -> p.id }"
    ),
    "onConflictUpdate - complex + returning/SQL" to cr(
      "INSERT INTO Person AS x (name, age) VALUES ('Joe', 123) ON CONFLICT (id) DO UPDATE SET name = (x.name || EXCLUDED.name), age = (x.age + EXCLUDED.age) RETURNING id"
    ),
    "onConflictUpdate - multiple/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictUpdate(listOf(thishidden.id, thishidden.name))(listOf(thishidden.name to excluding.name, thishidden.age to excluding.age))"
    ),
    "onConflictUpdate - multiple/SQL" to cr(
      "INSERT INTO Person AS x (name, age) VALUES ('Joe', 123) ON CONFLICT (id, name) DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age"
    ),
    "onConflictUpdate - setParams/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }.onConflictUpdate(listOf(thishidden.id))(listOf(thishidden.name to excluding.name, thishidden.age to excluding.age))"""
    ),
    "onConflictUpdate - setParams/SQL" to cr(
      "INSERT INTO Person AS x (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "onConflictUpdate - setParams + exclusion/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")).excluding(listOf(thisinsert.id)) }.onConflictUpdate(listOf(thishidden.id))(listOf(thishidden.name to excluding.name, thishidden.age to excluding.age))"""
    ),
    "onConflictUpdate - setParams + exclusion/SQL" to cr(
      "INSERT INTO Person AS x (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "onConflictIgnore/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictIgnore(listOf(thisinsert.id))"
    ),
    "onConflictIgnore/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123) ON CONFLICT (id) DO NOTHING"
    ),
    "onConflictIgnore - multiple/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.onConflictIgnore(listOf(thisinsert.id, thisinsert.name))"
    ),
    "onConflictIgnore - multiple/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123) ON CONFLICT (id, name) DO NOTHING"
    ),
    "onConflictIgnore - multiple - setParams/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }.onConflictIgnore(listOf(thisinsert.id, thisinsert.name))"""
    ),
    "onConflictIgnore - multiple - setParams/SQL" to cr(
      "INSERT INTO Person (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) ON CONFLICT (id, name) DO NOTHING",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "onConflictIgnore - setParams + exclusion/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")).excluding(listOf(thisinsert.id)) }.onConflictIgnore(listOf(thisinsert.id, thisinsert.name))"""
    ),
    "onConflictIgnore - setParams + exclusion/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Joe}, {1:123}) ON CONFLICT (id, name) DO NOTHING",
      "0" to "Joe", "1" to "123"
    ),
  )
}
