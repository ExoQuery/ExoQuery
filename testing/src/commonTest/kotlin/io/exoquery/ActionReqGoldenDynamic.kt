package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ActionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "insert/simple/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }"
    ),
    "insert/simple/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123)"
    ),
    "insert/simple - nullable/XR" to kt(
      "insert<PersonNullable> { set(thisinsert.name to Joe, thisinsert.age to 123) }"
    ),
    "insert/simple - nullable/SQL" to cr(
      "INSERT INTO PersonNullable (name, age) VALUES ('Joe', 123)"
    ),
    "insert/simple with params/XR" to kt(
      """insert<Person> { set(thisinsert.name to TagP("0"), thisinsert.age to TagP("1")) }"""
    ),
    "insert/simple with params/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Joe}, {1:123})",
      "671837ae-24be-40ba-b540-37c83a492563" to "Joe", "d42b91ff-423a-41de-b99f-a238525bc66c" to "123"
    ),
    "insert/simple with params - nullable/XR" to kt(
      """insert<PersonNullable> { set(thisinsert.name to TagP("0"), thisinsert.age to TagP("1")) }"""
    ),
    "insert/simple with params - nullable/SQL" to cr(
      "INSERT INTO PersonNullable (name, age) VALUES ({0:Joe}, {1:123})",
      "0c29d854-4696-44f8-a407-07b0c65584c6" to "Joe", "cea53a88-409e-4178-b0d7-854a5fab63cf" to "123"
    ),
    "insert/simple with params - nullable - actual null/XR" to kt(
      """insert<PersonNullable> { set(thisinsert.name to TagP("0"), thisinsert.age to TagP("1")) }"""
    ),
    "insert/simple with params - nullable - actual null/SQL" to cr(
      "INSERT INTO PersonNullable (name, age) VALUES ({0:null}, {1:123})",
      "a946d9cd-da59-4559-98c1-d4152d72294f" to "null", "154a1121-724d-4e6b-9b59-d714063a384d" to "123"
    ),
    "insert/simple with setParams/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }"""
    ),
    "insert/simple with setParams/SQL" to cr(
      "INSERT INTO Person (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123})",
      "fba0b48f-6f05-4c96-a0c0-4b937fea43c1" to "1", "ca38f082-b6f9-4dd9-becd-e01accc3bd11" to "Joe", "4457f224-7fc3-47e9-9986-e8dc2ef37500" to "123"
    ),
    "insert/simple with setParams - nullable/XR" to kt(
      """insert<PersonNullable> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }"""
    ),
    "insert/simple with setParams - nullable/SQL" to cr(
      "INSERT INTO PersonNullable (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123})",
      "0de205bd-4f5e-4773-bbbc-bb7e3f75053c" to "1", "d170badd-5052-4827-beab-a82a9390d166" to "Joe", "46c8c670-0ecf-4ae3-a8d7-3e4bc207d8bd" to "123"
    ),
    "insert/simple with setParams - nullable - actual null/XR" to kt(
      """insert<PersonNullable> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }"""
    ),
    "insert/simple with setParams - nullable - actual null/SQL" to cr(
      "INSERT INTO PersonNullable (id, name, age) VALUES ({0:1}, {1:null}, {2:123})",
      "b27215f1-703d-4af8-bede-f31b4e7138c1" to "1", "cdcda3a7-c98f-4174-a0ae-40d551a6f047" to "null", "3e3a2b67-5ab0-44f5-a9bd-298e460aef2f" to "123"
    ),
    "insert/simple with setParams and exclusion/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")).excluding(listOf(thisinsert.id)) }"""
    ),
    "insert/simple with setParams and exclusion/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Joe}, {1:123})",
      "eae4a812-44f6-4763-b309-460f42083990" to "Joe", "49b8966a-7c55-4068-a2ce-4c8322b90c1f" to "123"
    ),
    "insert/simple with setParams and exclusion - multiple/XR" to kt(
      """insert<Person> { set(thisinsert.id to TagP("0"), thisinsert.name to TagP("1"), thisinsert.age to TagP("2")).excluding(listOf(thisinsert.id, thisinsert.name)) }"""
    ),
    "insert/simple with setParams and exclusion - multiple/SQL" to cr(
      "INSERT INTO Person (age) VALUES ({0:123})",
      "ce10e86b-fcda-46a1-b09b-0d7899dcc35c" to "123"
    ),
    "insert/with returning/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.returning { p -> p.id }"
    ),
    "insert/with returning/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123) RETURNING id"
    ),
    "insert/with returning/SQL-SqlServer" to cr(
      "INSERT INTO Person (name, age) OUTPUT INSERTED.id VALUES ('Joe', 123)"
    ),
    "insert/with returning/returningType" to cr(
      "ClauseInQuery"
    ),
    "insert/with returning - multiple/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.returning { p -> Tuple(first = p.id, second = p.name) }"
    ),
    "insert/with returning - multiple/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123) RETURNING id, name"
    ),
    "insert/with returning - multiple/SQL-SqlServer" to cr(
      "INSERT INTO Person (name, age) OUTPUT INSERTED.id, INSERTED.name VALUES ('Joe', 123)"
    ),
    "insert/with returning - multiple/returningType" to cr(
      "ClauseInQuery"
    ),
    "insert/with returning params/XR" to kt(
      """insert<Person> { set(thisinsert.name to TagP("1"), thisinsert.age to TagP("2")) }.returning { p -> Tuple(first = p.name, second = TagP("0")) }"""
    ),
    "insert/with returning params/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Joe}, {1:123}) RETURNING name, {2:myParamValue}",
      "c48769c0-d089-4bed-9497-bc671a67ac7d" to "Joe", "7acc8e06-fb17-4f7a-b8ab-479a2b7b2b29" to "123", "0e02712c-75d5-4083-9f9d-78051ef32451" to "myParamValue"
    ),
    "insert/with returning params/Params" to kt(
      "[ParamSingle(0, Joe, String), ParamSingle(1, 123, Int), ParamSingle(2, myParamValue, String)]"
    ),
    "insert/with returning params/returningType" to cr(
      "ClauseInQuery"
    ),
    "insert/with returningKeys/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.returningKeys { listOf(thisreturningKeys.id) }"
    ),
    "insert/with returningKeys/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123)"
    ),
    "insert/with returningKeys/returningType" to cr(
      "Keys(columns=[id])"
    ),
    "insert/with returningKeys - multiple/XR" to kt(
      "insert<Person> { set(thisinsert.name to Joe, thisinsert.age to 123) }.returningKeys { listOf(thisreturningKeys.id, thisreturningKeys.name) }"
    ),
    "insert/with returningKeys - multiple/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ('Joe', 123)"
    ),
    "insert/with returningKeys - multiple/returningType" to cr(
      "Keys(columns=[id, name])"
    ),
    "update/simple/XR" to kt(
      "update<Person> { set(thisupdate.name to Joe, thisupdate.age to 123) }.filter { p -> p.id == 1 }"
    ),
    "update/simple/SQL" to cr(
      "UPDATE Person SET name = 'Joe', age = 123 WHERE id = 1"
    ),
    "update/no condition/XR" to kt(
      "update<Person> { set(thisupdate.name to Joe, thisupdate.age to 123) }"
    ),
    "update/no condition/SQL" to cr(
      "UPDATE Person SET name = 'Joe', age = 123"
    ),
    "update/with setParams/XR" to kt(
      """update<Person> { set(thisupdate.id to TagP("0"), thisupdate.name to TagP("1"), thisupdate.age to TagP("2")) }.filter { p -> p.id == 1 }"""
    ),
    "update/with setParams/SQL" to cr(
      "UPDATE Person SET id = {0:1}, name = {1:Joe}, age = {2:123} WHERE id = 1",
      "e4be59ce-7c15-4b47-bd2f-18d3633a3472" to "1", "bbe6498d-509d-4cb3-b48e-ef296ab63afc" to "Joe", "4bf63e1b-7322-4d33-b6cb-e181e8a02add" to "123"
    ),
    "update/with setParams and exclusion/XR" to kt(
      """update<Person> { set(thisupdate.id to TagP("0"), thisupdate.name to TagP("1"), thisupdate.age to TagP("2")).excluding(listOf(thisupdate.id)) }.filter { p -> p.id == 1 }"""
    ),
    "update/with setParams and exclusion/SQL" to cr(
      "UPDATE Person SET name = {0:Joe}, age = {1:123} WHERE id = 1",
      "128ae6a4-648b-4f3e-a881-8a4974a6bea9" to "Joe", "43eacf23-8f63-4ae8-8abf-ae2a625a93da" to "123"
    ),
    "update/with returning/XR" to kt(
      "update<Person> { set(thisupdate.name to Joe, thisupdate.age to 123) }.filter { p -> p.id == 1 }.returning { p -> p.id }"
    ),
    "update/with returning/SQL" to cr(
      "UPDATE Person SET name = 'Joe', age = 123 WHERE id = 1 RETURNING id"
    ),
    "update/with returning/SQL-SqlServer" to cr(
      "UPDATE Person SET name = 'Joe', age = 123 OUTPUT INSERTED.id WHERE id = 1"
    ),
    "update/with returning/returningType" to cr(
      "ClauseInQuery"
    ),
    "update/with returningKeys/XR" to kt(
      "update<Person> { set(thisupdate.name to Joe, thisupdate.age to 123) }.filter { p -> p.id == 1 }.returningKeys { listOf(thisreturningKeys.id) }"
    ),
    "update/with returningKeys/SQL" to cr(
      "UPDATE Person SET name = 'Joe', age = 123 WHERE id = 1"
    ),
    "update/with returningKeys/returningType" to cr(
      "Keys(columns=[id])"
    ),
    "delete/simple/XR" to kt(
      "delete<Person>.filter { p -> p.id == 1 }"
    ),
    "delete/simple/SQL" to cr(
      "DELETE FROM Person WHERE id = 1"
    ),
    "delete/no condition/XR" to kt(
      "delete<Person>"
    ),
    "delete/no condition/SQL" to cr(
      "DELETE FROM Person"
    ),
    "delete/with returning/XR" to kt(
      "delete<Person>.filter { p -> p.id == 1 }.returning { p -> p.id }"
    ),
    "delete/with returning/SQL" to cr(
      "DELETE FROM Person WHERE id = 1 RETURNING id"
    ),
    "delete/with returning/returningType" to cr(
      "ClauseInQuery"
    ),
    "delete/with returningKeys/XR" to kt(
      "delete<Person>.filter { p -> p.id == 1 }.returningKeys { listOf(thisreturningKeys.id) }"
    ),
    "delete/with returningKeys/SQL" to cr(
      "DELETE FROM Person WHERE id = 1"
    ),
    "delete/with returningKeys/returningType" to cr(
      "Keys(columns=[id])"
    ),
  )
}
