package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object EmbeddedDistinctReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "queries with embedded entities should/function property inside of nested distinct queries/XR" to kt(
      "Table(Emb).map { e -> Parent(id = 1, emb1 = e) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries/SQL" to cr(
      "SELECT DISTINCT 1 AS id, e.a, e.b FROM Emb e"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries with renames/XR" to kt(
      "Table(EMB).map { e -> Parent(ID = 1, emb1 = e) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries with renames/SQL" to cr(
      """SELECT DISTINCT 1 AS ID, e."A" AS A, e."B" AS B FROM "EMB" e"""
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - tuple/XR" to kt(
      "Table(Emb).map { e -> Parent(id = 1, emb1 = e) }.distinct.map { p -> Tuple(first = 2, second = p) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - tuple/SQL" to cr(
      "SELECT DISTINCT 2 AS first, p.id, p.emb1_a AS a, p.emb1_b AS b FROM (SELECT DISTINCT 1 AS id, e.a AS emb1_a, e.b AS emb1_b FROM Emb e) AS p"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries through tuples/XR" to kt(
      "Table(Emb).map { e -> Tuple(first = 1, second = e) }.distinct.map { t -> Parent(id = t.first, emb1 = t.second) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries through tuples/SQL" to cr(
      "SELECT DISTINCT t.first AS id, t.second_a AS a, t.second_b AS b FROM (SELECT DISTINCT 1 AS first, e.a AS second_a, e.b AS second_b FROM Emb e) AS t"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice/XR" to kt(
      "Table(Emb).map { e -> Parent(idP = 1, emb1 = e) }.distinct.map { p -> Grandparent(idG = 2, par = p) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice/SQL" to cr(
      "SELECT DISTINCT 2 AS idG, p.idP, p.emb1_a AS a, p.emb1_b AS b FROM (SELECT DISTINCT 1 AS idP, e.a AS emb1_a, e.b AS emb1_b FROM Emb e) AS p"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple/XR" to kt(
      "Table(Emb).map { e -> Parent(idP = 1, emb1 = e) }.distinct.map { p -> Grandparent(idG = 2, par = p) }.distinct.map { g -> Tuple(first = 3, second = g) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple/SQL" to cr(
      "SELECT DISTINCT 3 AS first, g.idG, g.par_idP AS idP, g.par_emb1_a AS a, g.par_emb1_b AS b FROM (SELECT DISTINCT 2 AS idG, p.idP AS par_idP, p.emb1_a AS par_emb1_a, p.emb1_b AS par_emb1_b FROM (SELECT DISTINCT 1 AS idP, e.a AS emb1_a, e.b AS emb1_b FROM Emb e) AS p) AS g"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple - with renames/XR" to kt(
      "Table(EMB).map { e -> Parent(idP = 1, emb1 = e) }.distinct.map { p -> Grandparent(idG = 2, par = p) }.distinct.map { g -> Tuple(first = 3, second = g) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple - with renames/SQL" to cr(
      """SELECT DISTINCT 3 AS first, g.idG, g.par_idP AS idP, g.par_emb1_A AS A, g.par_emb1_b AS b FROM (SELECT DISTINCT 2 AS idG, p.idP AS par_idP, p.emb1_A AS par_emb1_A, p.emb1_b AS par_emb1_b FROM (SELECT DISTINCT 1 AS idP, e."A" AS emb1_A, e.b AS emb1_b FROM "EMB" e) AS p) AS g"""
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple - with renames in middle (should not quote)/XR" to kt(
      "Table(EMB).map { e -> Parent(idPREN = 1, emb1REN = e) }.distinct.map { p -> Grandparent(idG = 2, par = p) }.distinct.map { g -> Tuple(first = 3, second = g) }.distinct"
    ),
    "queries with embedded entities should/function property inside of nested distinct queries - twice - into tuple - with renames in middle (should not quote)/SQL" to cr(
      """SELECT DISTINCT 3 AS first, g.idG, g.par_idPREN AS idPREN, g.par_emb1REN_A AS A, g.par_emb1REN_b AS b FROM (SELECT DISTINCT 2 AS idG, p.idPREN AS par_idPREN, p.emb1REN_A AS par_emb1REN_A, p.emb1REN_b AS par_emb1REN_b FROM (SELECT DISTINCT 1 AS idPREN, e."A" AS emb1REN_A, e.b AS emb1REN_b FROM "EMB" e) AS p) AS g"""
    ),
  )
}
