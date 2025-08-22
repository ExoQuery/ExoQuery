package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object BatchActionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "insert/simple/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Refiner_STRING}, {1:Refiner_INT})",
      "0" to "Refiner_STRING", "1" to "Refiner_INT"
    ),
    "insert/simple/Params1" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:John}, {1:42})",
      "0" to "John", "1" to "42"
    ),
    "insert/simple/Params2" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Jane}, {1:43})",
      "0" to "Jane", "1" to "43"
    ),
    "insert/simple/Params3" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Jack}, {1:44})",
      "0" to "Jack", "1" to "44"
    ),
    "insert/simple" to kt(
      "[ParamSingle(0, John, String), ParamSingle(1, 42, Int)], [ParamSingle(0, Jane, String), ParamSingle(1, 43, Int)], [ParamSingle(0, Jack, String), ParamSingle(1, 44, Int)]"
    ),
    "insert/simple with setParams/SQL" to cr(
      "INSERT INTO Person (id, name, age) VALUES ({0:Refiner_INT}, {1:Refiner_STRING}, {2:Refiner_INT})",
      "0" to "Refiner_INT", "1" to "Refiner_STRING", "2" to "Refiner_INT"
    ),
    "insert/simple with setParams" to kt(
      "[ParamSingle(0, 1, Int), ParamSingle(1, John, String), ParamSingle(2, 42, Int)], [ParamSingle(0, 2, Int), ParamSingle(1, Jane, String), ParamSingle(2, 43, Int)], [ParamSingle(0, 3, Int), ParamSingle(1, Jack, String), ParamSingle(2, 44, Int)]"
    ),
    "insert/simple with setParams and exclusion/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Refiner_STRING}, {1:Refiner_INT})",
      "0" to "Refiner_STRING", "1" to "Refiner_INT"
    ),
    "insert/simple with setParams and exclusion" to kt(
      "[ParamSingle(0, John, String), ParamSingle(1, 42, Int)], [ParamSingle(0, Jane, String), ParamSingle(1, 43, Int)], [ParamSingle(0, Jack, String), ParamSingle(1, 44, Int)]"
    ),
    "insert/with returning/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Refiner_STRING}, {1:Refiner_INT}) RETURNING id",
      "0" to "Refiner_STRING", "1" to "Refiner_INT"
    ),
    "insert/with returning" to kt(
      "[ParamSingle(0, John, String), ParamSingle(1, 42, Int)], [ParamSingle(0, Jane, String), ParamSingle(1, 43, Int)], [ParamSingle(0, Jack, String), ParamSingle(1, 44, Int)]"
    ),
    "insert/with returning/returningType" to cr(
      "ClauseInQuery"
    ),
    "insert/with returning and params/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Refiner_STRING}, {1:Refiner_INT}) RETURNING id, {2:Refiner_STRING}",
      "0" to "Refiner_STRING", "1" to "Refiner_INT", "2" to "Refiner_STRING"
    ),
    "insert/with returning and params" to kt(
      "[ParamSingle(0, John, String), ParamSingle(1, 42, Int), ParamSingle(2, John, String)], [ParamSingle(0, Jane, String), ParamSingle(1, 43, Int), ParamSingle(2, Jane, String)], [ParamSingle(0, Jack, String), ParamSingle(1, 44, Int), ParamSingle(2, Jack, String)]"
    ),
    "insert/with returning and params/returningType" to cr(
      "ClauseInQuery"
    ),
    "insert/with returning keys/SQL" to cr(
      "INSERT INTO Person (name, age) VALUES ({0:Refiner_STRING}, {1:Refiner_INT})",
      "0" to "Refiner_STRING", "1" to "Refiner_INT"
    ),
    "insert/with returning keys" to kt(
      "[ParamSingle(0, John, String), ParamSingle(1, 42, Int)], [ParamSingle(0, Jane, String), ParamSingle(1, 43, Int)], [ParamSingle(0, Jack, String), ParamSingle(1, 44, Int)]"
    ),
    "insert/with returning keys/returningType" to cr(
      "Keys(columns=[id])"
    ),
    "update/simple/SQL" to cr(
      "UPDATE Person SET name = {0:Refiner_STRING}, age = {1:Refiner_INT} WHERE id = {2:Refiner_INT}",
      "0" to "Refiner_STRING", "1" to "Refiner_INT", "2" to "Refiner_INT"
    ),
    "update/simple/Params1" to cr(
      "UPDATE Person SET name = {0:John-A}, age = {1:52} WHERE id = {2:1}",
      "0" to "John-A", "1" to "52", "2" to "1"
    ),
    "update/simple/Params2" to cr(
      "UPDATE Person SET name = {0:Jane-A}, age = {1:53} WHERE id = {2:2}",
      "0" to "Jane-A", "1" to "53", "2" to "2"
    ),
    "update/simple/Params3" to cr(
      "UPDATE Person SET name = {0:Jack-A}, age = {1:54} WHERE id = {2:3}",
      "0" to "Jack-A", "1" to "54", "2" to "3"
    ),
    "update/simple" to kt(
      "[ParamSingle(0, John-A, String), ParamSingle(1, 52, Int), ParamSingle(2, 1, Int)], [ParamSingle(0, Jane-A, String), ParamSingle(1, 53, Int), ParamSingle(2, 2, Int)], [ParamSingle(0, Jack-A, String), ParamSingle(1, 54, Int), ParamSingle(2, 3, Int)]"
    ),
    "update/simple/returningType" to cr(
      "None"
    ),
    "update/simple - where clause/SQL" to cr(
      "UPDATE Person SET name = {0:Refiner_STRING}, age = {1:Refiner_INT} WHERE id = {2:Refiner_INT} AND age > 50",
      "0" to "Refiner_STRING", "1" to "Refiner_INT", "2" to "Refiner_INT"
    ),
    "update/simple - where clause" to kt(
      "[ParamSingle(0, John-A, String), ParamSingle(1, 52, Int), ParamSingle(2, 1, Int)], [ParamSingle(0, Jane-A, String), ParamSingle(1, 53, Int), ParamSingle(2, 2, Int)], [ParamSingle(0, Jack-A, String), ParamSingle(1, 54, Int), ParamSingle(2, 3, Int)]"
    ),
    "update/simple with setParams and exclusion/SQL" to cr(
      "UPDATE Person SET name = {0:Refiner_STRING}, age = {1:Refiner_INT} WHERE id = {2:Refiner_INT}",
      "0" to "Refiner_STRING", "1" to "Refiner_INT", "2" to "Refiner_INT"
    ),
    "update/simple with setParams and exclusion" to kt(
      "[ParamSingle(0, John-A, String), ParamSingle(1, 52, Int), ParamSingle(2, 1, Int)], [ParamSingle(0, Jane-A, String), ParamSingle(1, 53, Int), ParamSingle(2, 2, Int)], [ParamSingle(0, Jack-A, String), ParamSingle(1, 54, Int), ParamSingle(2, 3, Int)]"
    ),
    "update/simple with setParams and exclusion and returning param/SQL" to cr(
      "UPDATE Person SET name = {0:Refiner_STRING}, age = {1:Refiner_INT} WHERE id = {2:Refiner_INT} RETURNING id, {3:Refiner_STRING}",
      "0" to "Refiner_STRING", "1" to "Refiner_INT", "2" to "Refiner_INT", "3" to "Refiner_STRING"
    ),
    "update/simple with setParams and exclusion and returning param" to kt(
      "[ParamSingle(0, John-A, String), ParamSingle(1, 52, Int), ParamSingle(2, 1, Int), ParamSingle(3, John-A, String)], [ParamSingle(0, Jane-A, String), ParamSingle(1, 53, Int), ParamSingle(2, 2, Int), ParamSingle(3, Jane-A, String)], [ParamSingle(0, Jack-A, String), ParamSingle(1, 54, Int), ParamSingle(2, 3, Int), ParamSingle(3, Jack-A, String)]"
    ),
    "update/simple with setParams and exclusion and returning param/returningType" to cr(
      "ClauseInQuery"
    ),
    "delete/with filter - inside/SQL" to cr(
      "DELETE FROM Person WHERE id = {0:Refiner_INT}",
      "0" to "Refiner_INT"
    ),
    "delete/with filter - inside/Params1" to cr(
      "DELETE FROM Person WHERE id = {0:1}",
      "0" to "1"
    ),
    "delete/with filter - inside/Params2" to cr(
      "DELETE FROM Person WHERE id = {0:2}",
      "0" to "2"
    ),
    "delete/with filter - inside/Params3" to cr(
      "DELETE FROM Person WHERE id = {0:3}",
      "0" to "3"
    ),
    "delete/with filter - inside" to kt(
      "[ParamSingle(0, 1, Int)], [ParamSingle(0, 2, Int)], [ParamSingle(0, 3, Int)]"
    ),
    "delete/with filter/SQL" to cr(
      "DELETE FROM Person WHERE id = {0:Refiner_INT}",
      "0" to "Refiner_INT"
    ),
    "delete/with filter" to kt(
      "[ParamSingle(0, 1, Int)], [ParamSingle(0, 2, Int)], [ParamSingle(0, 3, Int)]"
    ),
    "delete/with returning/SQL" to cr(
      "DELETE FROM Person WHERE id = {0:Refiner_INT} RETURNING id",
      "0" to "Refiner_INT"
    ),
    "delete/with returning" to kt(
      "[ParamSingle(0, 1, Int)], [ParamSingle(0, 2, Int)], [ParamSingle(0, 3, Int)]"
    ),
    "delete/with returning/returningType" to cr(
      "ClauseInQuery"
    ),
  )
}
