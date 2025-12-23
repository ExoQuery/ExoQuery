package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object QueryReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "basic query/XR" to kt(
      "Table(Person)"
    ),
    "basic query" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "basic query - deprecated capture/XR" to kt(
      "Table(Person)"
    ),
    "basic query - deprecated capture" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "query with map/XR" to kt(
      "Table(Person).map { p -> p.name }"
    ),
    "query with map" to cr(
      "SELECT p.name AS value FROM Person p"
    ),
    "query with groupBy/XR" to kt(
      "select { val p = from(Table(Person)); groupBy(p.name); Tuple(first = p.name, second = avg_GC(p.age)) }"
    ),
    "query with groupBy" to cr(
      "SELECT p.name AS first, avg(p.age) AS second FROM Person p GROUP BY p.name"
    ),
    "query with groupBy + having/XR" to kt(
      "select { val p = from(Table(Person)); groupBy(p.name); having(avg_GC(p.age) > 18.toDouble_MCS()); Tuple(first = p.name, second = avg_GC(p.age)) }"
    ),
    "query with groupBy + having" to cr(
      "SELECT p.name AS first, avg(p.age) AS second FROM Person p GROUP BY p.name HAVING avg(p.age) > 18"
    ),
    "query with groupBy + having + orderBy/XR" to kt(
      "select { val p = from(Table(Person)); groupBy(p.name); having(avg_GC(p.age) > 18.toDouble_MCS()); sortBy(avg_GC(p.age) to Desc); Tuple(first = p.name, second = avg_GC(p.age)) }"
    ),
    "query with groupBy + having + orderBy" to cr(
      "SELECT p.name AS first, avg(p.age) AS second FROM Person p GROUP BY p.name HAVING avg(p.age) > 18 ORDER BY avg(p.age) DESC"
    ),
    "query with with-clause + groupBy + having/XR" to kt(
      "select { val p = from(Table(Person)); where(p.name == Joe); groupBy(p.name); having(avg_GC(p.age) > 18.toDouble_MCS()); Tuple(first = p.name, second = avg_GC(p.age)) }"
    ),
    "query with with-clause + groupBy + having" to cr(
      "SELECT p.name AS first, avg(p.age) AS second FROM Person p WHERE p.name = 'Joe' GROUP BY p.name HAVING avg(p.age) > 18"
    ),
    "union with impure free - should not collapse/XR" to kt(
      """select { val u = from(Table(Person).filter { p -> p.name == Joe }.union(Table(Person).filter { p -> p.name == Jack })); val a = join(Table(Address)) { a.ownerId == u.id }; free("stuff(, ${'$'}{u.name}, )").invoke() }"""
    ),
    "union with impure free - should not collapse" to cr(
      "SELECT stuff(u.name) AS value FROM ((SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Joe') UNION (SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE p1.name = 'Jack')) AS u INNER JOIN Address a ON a.ownerId = u.id"
    ),
    "union with pure function - should collapse/XR" to kt(
      "select { val u = from(Table(Person).filter { p -> p.name == Joe }.union(Table(Person).filter { p -> p.name == Jack })); val a = join(Table(Address)) { a.ownerId == u.id }; u.name.uppercase_MC() }"
    ),
    "union with pure function - should collapse" to cr(
      "(SELECT UPPER(u.name) AS value FROM Person u INNER JOIN Address a ON a.ownerId = u.id WHERE u.name = 'Joe') UNION (SELECT UPPER(u.name) AS value FROM Person u INNER JOIN Address a ON a.ownerId = u.id WHERE u.name = 'Jack')"
    ),
    "union with aggregation - shuold not collapse/XR" to kt(
      "select { val u = from(Table(Person).filter { p -> p.name == Joe }.union(Table(Person).filter { p -> p.name == Jack })); val a = join(Table(Address)) { a.ownerId == u.id }; avg_GC(u.age) }"
    ),
    "union with aggregation - shuold not collapse" to cr(
      "SELECT avg(u.age) AS value FROM ((SELECT p.id, p.name, p.age FROM Person p WHERE p.name = 'Joe') UNION (SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE p1.name = 'Jack')) AS u INNER JOIN Address a ON a.ownerId = u.id"
    ),
    "map with aggregation/XR" to kt(
      "Table(Person).map { p -> avg_GC(p.age) }"
    ),
    "map with aggregation" to cr(
      "SELECT avg(p.age) AS value FROM Person p"
    ),
    "map with count/XR" to kt(
      "Table(Person).map { p -> count_GC(p.age) }"
    ),
    "map with count" to cr(
      "SELECT count(p.age) AS value FROM Person p"
    ),
    "map with count star/XR" to kt(
      "Table(Person).map { p -> COUNT_STAR_GC() }"
    ),
    "map with count star" to cr(
      "SELECT count(*) AS value FROM Person p"
    ),
    "map with count distinct/XR" to kt(
      "Table(Person).map { p -> countDistinct_GC(p.name, p.age) }"
    ),
    "map with count distinct" to cr(
      "SELECT count(DISTINCT p.name, p.age) AS value FROM Person p"
    ),
    "select with distinct count/XR" to kt(
      "select { val p = from(Table(Person)); countDistinct_GC(p.name, p.age) }"
    ),
    "select with distinct count" to cr(
      "SELECT count(DISTINCT p.name, p.age) AS value FROM Person p"
    ),
    "map with stddev/XR" to kt(
      "Table(Person).map { p -> stddev_GC(p.age) }"
    ),
    "map with stddev" to cr(
      "SELECT stddev(p.age) AS value FROM Person p"
    ),
    "select with stdev and another column/XR" to kt(
      "select { val p = from(Table(Person)); Tuple(first = stddev_GC(p.age), second = p.name) }"
    ),
    "select with stdev and another column" to cr(
      "SELECT stddev(p.age) AS first, p.name AS second FROM Person p"
    ),
    "query with filter/XR" to kt(
      "Table(Person).filter { p -> p.age > 18 }"
    ),
    "query with filter" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18"
    ),
    "query with where/XR" to kt(
      "Table(Person).filter { x -> x.age > 18 }"
    ),
    "query with where" to cr(
      "SELECT x.id, x.name, x.age FROM Person x WHERE x.age > 18"
    ),
    "filter + correlated isEmpty/XR" to kt(
      "Table(Person).filter { p -> p.age.toDouble_MCS() > Table(Person).map { p -> p.age }.avg_MC() }"
    ),
    "filter + correlated isEmpty" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.age > (SELECT avg(p1.age) FROM Person p1)"
    ),
    "filter + correlated + value/XR" to kt(
      "Table(Person).filter { p -> p.age.toDouble_MCS() > Table(Person).map { p -> avg_GC(p.age) - min_GC(p.age) }.toExpr }"
    ),
    "filter + correlated + value" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.age > (SELECT avg(p1.age) - min(p1.age) FROM Person p1)"
    ),
    "correlated contains/- correlated contains - simple/XR" to kt(
      "Table(Person).filter { p -> Table(Address).map { p -> p.ownerId }.toExpr.contains_MC(p.id) }"
    ),
    "correlated contains/- correlated contains - simple" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (SELECT p1.ownerId AS ownerId FROM Address p1)"
    ),
    "correlated contains/- correlated contains - different var names 1/XR" to kt(
      "Table(Person).filter { p -> Table(Address).map { it -> it.ownerId }.toExpr.contains_MC(p.id) }"
    ),
    "correlated contains/- correlated contains - different var names 1" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (SELECT it.ownerId AS ownerId FROM Address it)"
    ),
    "correlated contains/- correlated contains - different var names 2/XR" to kt(
      "Table(Person).filter { p -> Table(Address).map { pp -> pp.ownerId }.toExpr.contains_MC(p.id) }"
    ),
    "correlated contains/- correlated contains - different var names 2" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.id IN (SELECT pp.ownerId AS ownerId FROM Address pp)"
    ),
    "correlated contains/- correlated contains - only it vars/XR" to kt(
      "Table(Person).filter { it -> Table(Address).map { it -> it.ownerId }.toExpr.contains_MC(it.id) }"
    ),
    "correlated contains/- correlated contains - only it vars" to cr(
      "SELECT it.id, it.name, it.age FROM Person it WHERE it.id IN (SELECT it1.ownerId AS ownerId FROM Address it1)"
    ),
    "query with flatMap/XR" to kt(
      "Table(Person).flatMap { p -> Table(Address).filter { a -> a.ownerId == p.id } }"
    ),
    "query with flatMap" to cr(
      "SELECT a.ownerId, a.street, a.city FROM Person p, Address a WHERE a.ownerId = p.id"
    ),
    "query with union/XR" to kt(
      "Table(Person).filter { p -> p.age > 18 }.union(Table(Person).filter { p -> p.age < 18 })"
    ),
    "query with union" to cr(
      "(SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18) UNION (SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE p1.age < 18)"
    ),
    "query with unionAll/XR" to kt(
      "Table(Person).filter { p -> p.age > 18 }.unionAll(Table(Person).filter { p -> p.age < 18 })"
    ),
    "query with unionAll" to cr(
      "(SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18) UNION ALL (SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE p1.age < 18)"
    ),
    "query with unionAll - symbolic/XR" to kt(
      "Table(Person).filter { p -> p.age > 18 }.unionAll(Table(Person).filter { p -> p.age < 18 })"
    ),
    "query with unionAll - symbolic" to cr(
      "(SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18) UNION ALL (SELECT p1.id, p1.name, p1.age FROM Person p1 WHERE p1.age < 18)"
    ),
    "query with surrounding free/XR" to kt(
      """free(", ${'$'}{Table(Person).filter { p -> p.name == Joe }},  FOR UPDATE").asPure()"""
    ),
    "query with surrounding free" to cr(
      "SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.name = 'Joe' FOR UPDATE"
    ),
    "query with free in captured function/XR" to kt(
      """{ v -> free(", ${'$'}v,  FOR UPDATE").asPure() }.toQuery.apply(Table(Person).filter { p -> p.age > 21 })"""
    ),
    "query with free in captured function" to cr(
      "(SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.age > 21) FOR UPDATE"
    ),
    "query with free in captured function - receiver position/XR" to kt(
      """{ this -> free(", ${'$'}this,  FOR UPDATE").asPure() }.toQuery.apply(Table(Person).filter { p -> p.age > 21 })"""
    ),
    "query with free in captured function - receiver position" to cr(
      "(SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.age > 21) FOR UPDATE"
    ),
    "flat joins inside subquery/where/XR" to kt(
      "select { val p = from(Table(Person)); val innerRobot = join(select { val r = from(Table(Robot)); where(r.ownerId == p.id); Tuple(first = r.name, second = r.ownerId) }) { r.second == p.id }; Tuple(first = p, second = innerRobot) }"
    ),
    "flat joins inside subquery/where" to cr(
      "SELECT p.id, p.name, p.age, innerRobot.first, innerRobot.second FROM Person p INNER JOIN (SELECT r.name AS first, r.ownerId AS second FROM Robot r WHERE r.ownerId = p.id) AS innerRobot ON innerRobot.second = p.id"
    ),
    "flat joins inside subquery/groupBy/XR" to kt(
      "select { val p = from(Table(Person)); val innerRobot = join(select { val r = from(Table(Robot)); groupBy(r.ownerId); Tuple(first = r.name, second = r.ownerId) }) { r.second == p.id }; Tuple(first = p, second = innerRobot) }"
    ),
    "flat joins inside subquery/groupBy" to cr(
      "SELECT p.id, p.name, p.age, innerRobot.first, innerRobot.second FROM Person p INNER JOIN (SELECT r.name AS first, r.ownerId AS second FROM Robot r GROUP BY r.ownerId) AS innerRobot ON innerRobot.second = p.id"
    ),
    "flat joins inside subquery/sortBy/XR" to kt(
      "select { val p = from(Table(Person)); val innerRobot = join(select { val r = from(Table(Robot)); sortBy(r.name to Desc); Tuple(first = r.name, second = r.ownerId) }) { r.second == p.id }; Tuple(first = p, second = innerRobot) }"
    ),
    "flat joins inside subquery/sortBy" to cr(
      "SELECT p.id, p.name, p.age, innerRobot.first, innerRobot.second FROM Person p INNER JOIN (SELECT r.name AS first, r.ownerId AS second FROM Robot r ORDER BY r.name DESC) AS innerRobot ON innerRobot.second = p.id"
    ),
    "transformation of nested select clauses/where clauses are combined/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); where(a.city == Someplace); a }); where(p.name == Joe); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/where clauses are combined" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, Address a WHERE a.city = 'Someplace' AND p.name = 'Joe'"
    ),
    "transformation of nested select clauses/groupBy clauses cause nesting/variation A/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); groupBy(a.city); a }); where(p.name == Joe); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/groupBy clauses cause nesting/variation A" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, (SELECT a.ownerId, a.street, a.city FROM Address a GROUP BY a.city) AS a WHERE p.name = 'Joe'"
    ),
    "transformation of nested select clauses/groupBy clauses cause nesting/Variation B/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); groupBy(a.city); a }); groupBy(p.name); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/groupBy clauses cause nesting/Variation B" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, (SELECT a.ownerId, a.street, a.city FROM Address a GROUP BY a.city) AS a GROUP BY p.name"
    ),
    "transformation of nested select clauses/sortBy clauses cause nesting/variation A/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); sortBy(a.city to Asc); a }); where(p.name == Joe); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/sortBy clauses cause nesting/variation A" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, (SELECT a.ownerId, a.street, a.city FROM Address a ORDER BY a.city ASC) AS a WHERE p.name = 'Joe'"
    ),
    "transformation of nested select clauses/sortBy clauses cause nesting/Variation B/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); sortBy(a.city to Asc); a }); sortBy(p.name to Desc); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/sortBy clauses cause nesting/Variation B" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, (SELECT a.ownerId, a.street, a.city FROM Address a ORDER BY a.city ASC) AS a ORDER BY p.name DESC"
    ),
    "transformation of nested select clauses/combo of all cases will cause nesting/XR" to kt(
      "select { val p = from(Table(Person)); val a = from(select { val a = from(Table(Address)); where(a.city == Someplace); groupBy(a.city); sortBy(a.street to Asc); a }); where(p.name == Joe); groupBy(p.name); sortBy(p.age to Desc); Tuple(first = p, second = a) }"
    ),
    "transformation of nested select clauses/combo of all cases will cause nesting" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p, (SELECT a.ownerId, a.street, a.city FROM Address a WHERE a.city = 'Someplace' GROUP BY a.city ORDER BY a.street ASC) AS a WHERE p.name = 'Joe' GROUP BY p.name ORDER BY p.age DESC"
    ),
    "implicit joins/XR" to kt(
      "Table(Person).flatMap { p -> Table(Address).filter { a -> a.ownerId == p.id }.map { a -> Tuple(first = p.name, second = a.city) } }"
    ),
    "implicit joins" to cr(
      "SELECT p.name AS first, a.city AS second FROM Person p, Address a WHERE a.ownerId = p.id"
    ),
    "nested fragment filter - wrong alias resolution bug (fixed)/XR" to kt(
      "select { val row = from({ base -> base.filter { it -> it.a.age > 18 } }.toQuery.apply({ select { val person = from(Table(Person)); val address = join(Table(Address)) { addr.ownerId == person.id }; Composite(a = person, b = address) } }.toQuery.apply())); row }"
    ),
    "nested fragment filter - wrong alias resolution bug (fixed)" to cr(
      "SELECT person.id, person.name, person.age, address.ownerId, address.street, address.city FROM Person person INNER JOIN Address address ON address.ownerId = person.id WHERE person.age > 18"
    ),
    "composeFrom join - duplicate subquery alias bug (fixed)/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> Table(B).filter { it -> it.status == active }.join { b -> b.id == this.bId } }.toQuery.apply(a)); val c = from({ this -> Table(C).filter { it -> it.status == active }.join { c -> c.id == this.cId } }.toQuery.apply(a)); Result(aId = a.id, bId = b.id, cId = c.id) }"
    ),
    "composeFrom join - duplicate subquery alias bug (fixed)" to cr(
      "SELECT a.id AS aId, b.id AS bId, c.id AS cId FROM A a INNER JOIN (SELECT b.id, b.status FROM B b WHERE b.status = 'active') AS b ON b.id = a.bId INNER JOIN (SELECT c.id, c.status FROM C c WHERE c.status = 'active') AS c ON c.id = a.cId"
    ),
    "PushAlias tests for composeFrom join/flatJoin with nested select query/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> select { val bb = from(Table(B)); where(bb.status == active); bb }.join { selectedB -> selectedB.id == this.bId } }.toQuery.apply(a)); Tuple(first = a.id, second = b.value) }"
    ),
    "PushAlias tests for composeFrom join/flatJoin with nested select query" to cr(
      "SELECT a.id AS first, b.value AS second FROM A a INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb WHERE bb.status = 'active') AS b ON b.id = a.bId"
    ),
    "PushAlias tests for composeFrom join/flatJoin with map and filter chain/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> Table(B).filter { it -> it.status == active }.map { it -> it }.join { filteredB -> filteredB.id == this.bId } }.toQuery.apply(a)); Tuple(first = a.id, second = b.value) }"
    ),
    "PushAlias tests for composeFrom join/flatJoin with map and filter chain" to cr(
      "SELECT a.id AS first, b.value AS second FROM A a INNER JOIN (SELECT b.id, b.status, b.value FROM B b WHERE b.status = 'active') AS b ON b.id = a.bId"
    ),
    "PushAlias tests for composeFrom join/flatJoin with flatMap/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> Table(B).flatMap { bb -> Table(C).filter { c -> c.bId == bb.id }.map { c -> bb } }.join { mappedB -> mappedB.id == this.bId } }.toQuery.apply(a)); Tuple(first = a.id, second = b.value) }"
    ),
    "PushAlias tests for composeFrom join/flatJoin with flatMap" to cr(
      "SELECT a.id AS first, b.value AS second FROM A a INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb, C c WHERE c.bId = bb.id) AS b ON b.id = a.bId"
    ),
    "PushAlias tests for composeFrom join/multiple flatJoins with different query types/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> select { val bb = from(Table(B)); where(bb.status == active); bb }.join { selectedB -> selectedB.id == this.bId } }.toQuery.apply(a)); val c = from({ this -> Table(C).filter { it -> it.name == test }.join { filteredC -> filteredC.bId == this.cId } }.toQuery.apply(a)); Triple(first = a.id, second = b.value, third = c.name) }"
    ),
    "PushAlias tests for composeFrom join/multiple flatJoins with different query types" to cr(
      "SELECT a.id AS first, b.value AS second, c.name AS third FROM A a INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb WHERE bb.status = 'active') AS b ON b.id = a.bId INNER JOIN (SELECT c.id, c.bId, c.name FROM C c WHERE c.name = 'test') AS c ON c.bId = a.cId"
    ),
    "PushAlias tests for composeFrom join/multiple flatJoins with different query types - and a duplicate/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> select { val bb = from(Table(B)); where(bb.status == active); bb }.join { selectedB -> selectedB.id == this.bId } }.toQuery.apply(a)); val b1 = from({ this -> select { val bb = from(Table(B)); where(bb.status == active); bb }.join { selectedB -> selectedB.id == this.bId } }.toQuery.apply(a)); val c = from({ this -> Table(C).filter { it -> it.name == test }.join { filteredC -> filteredC.bId == this.cId } }.toQuery.apply(a)); Triple(first = a.id, second = b.value + b1.value, third = c.name) }"
    ),
    "PushAlias tests for composeFrom join/multiple flatJoins with different query types - and a duplicate" to cr(
      "SELECT a.id AS first, b.value + b1.value AS second, c.name AS third FROM A a INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb WHERE bb.status = 'active') AS b ON b.id = a.bId INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb WHERE bb.status = 'active') AS b1 ON b1.id = a.bId INNER JOIN (SELECT c.id, c.bId, c.name FROM C c WHERE c.name = 'test') AS c ON c.bId = a.cId"
    ),
    "PushAlias tests for composeFrom join/flatJoin with complex nested select/XR" to kt(
      "select { val a = from(Table(A)); val b = from({ this -> select { val bb = from(Table(B)); where(bb.status == active); groupBy(bb.status); having(avg_GC(bb.value) > 10.toDouble_MCS()); sortBy(bb.status to Asc); bb }.join { complexB -> complexB.id == this.bId } }.toQuery.apply(a)); Tuple(first = a.id, second = b.value) }"
    ),
    "PushAlias tests for composeFrom join/flatJoin with complex nested select" to cr(
      "SELECT a.id AS first, b.value AS second FROM A a INNER JOIN (SELECT bb.id, bb.status, bb.value FROM B bb WHERE bb.status = 'active' GROUP BY bb.status HAVING avg(bb.value) > 10 ORDER BY bb.status ASC) AS b ON b.id = a.bId"
    ),
    "filtered joined fragment - duplicate table in FROM clause bug (fixed)/XR" to kt(
      "select { val r = from({ this -> this.filter { it -> it.a.id > 0 } }.toQuery.apply({ select { val a = from(Table(A)); val b = join(Table(B)) { b.aId == a.id }; Composite(a = a, b = b) } }.toQuery.apply())); where(r.b.id > 0); r.a.id }"
    ),
    "filtered joined fragment - duplicate table in FROM clause bug (fixed)" to cr(
      "SELECT a.id AS value FROM A a INNER JOIN B b ON b.aId = a.id WHERE a.id > 0 AND b.id > 0"
    ),
    "nested select with filter on nested pair - double nested/XR" to kt(
      "select { val p = from(select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.nested); val a = join(Table(AddressCrs)) { a.ownerId == p.first.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.first.name == JoeOuter }"
    ),
    "nested select with filter on nested pair - double nested" to cr(
      "SELECT p.first_id AS id, p.first_name AS name, p.second_ownerId AS ownerId, p.second_street AS street, a.ownerId, a.street FROM (SELECT p.id AS first_id, p.name AS first_name, a.ownerId AS second_ownerId, a.street AS second_street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id) AS p INNER JOIN AddressCrs a ON a.ownerId = p.first_id WHERE p.first_name = 'JoeOuter'"
    ),
    "nested select with filter on pair - single nested/XR" to kt(
      "select { val p = from(Table(PersonCrs)); val a = join(Table(AddressCrs)) { a.ownerId == p.id }; Tuple(first = p, second = a) }.filter { pair -> pair.first.name == JoeOuter }"
    ),
    "nested select with filter on pair - single nested" to cr(
      "SELECT p.id, p.name, a.ownerId, a.street FROM PersonCrs p INNER JOIN AddressCrs a ON a.ownerId = p.id WHERE p.name = 'JoeOuter'"
    ),
    "select with where groupBy and left join - filter on grouped field/XR" to kt(
      "select { val p = from(Table(Person)); val a = leftJoin(Table(Address)) { a.ownerId == p.id }; where(p.age > 18); groupBy(p); p }.filter { ccc -> ccc.name == Main St }"
    ),
    "select with where groupBy and left join - filter on grouped field" to cr(
      "SELECT ccc.id, ccc.name, ccc.age FROM (SELECT p.id, p.name, p.age FROM Person p LEFT JOIN Address a ON a.ownerId = p.id WHERE p.age > 18 GROUP BY p.id, p.name, p.age) AS ccc WHERE ccc.name = 'Main St'"
    ),
    "destructured composite + extension join - HAVING loses field prefix bug (fixed)/XR" to kt(
      "select { val destruct = from({ select { val a = from(Table(A)); val b = join(Table(B)) { b.aId == a.id }; where(a.id > 0); Comp(a = a, b = b) } }.toQuery.apply()); val a = /*ASI*/ destruct.a; val b = /*ASI*/ destruct.b; val c = from({ this -> Table(C).join { c -> c.bId == this.id } }.toQuery.apply(b)); groupBy(a.id); having(sum_GC(b.value) > 10); a.id }"
    ),
    "destructured composite + extension join - HAVING loses field prefix bug (fixed)" to cr(
      "SELECT destruct.a_id AS value FROM (SELECT a.id AS a_id, b.id AS b_id, b.aId AS b_aId, b.value AS b_value FROM A a INNER JOIN B b ON b.aId = a.id WHERE a.id > 0) AS destruct INNER JOIN C c ON c.bId = destruct.b_id GROUP BY destruct.a_id HAVING sum(destruct.b_value) > 10"
    ),
  )
}
