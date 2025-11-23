package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object QueryFilterFlatteningReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "can reduce/select-clause->triple + where with filter/XR" to kt(
      "select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; where(p.name == Joe); Triple(first = p.name, second = p.age, third = a.city) }.filter { t -> t.third == NewYork }"
    ),
    "can reduce/select-clause->triple + where with filter" to cr(
      "SELECT p.name AS first, p.age AS second, a.city AS third FROM Person p INNER JOIN Address a ON a.personId = p.id WHERE p.name = 'Joe' AND a.city = 'NewYork'"
    ),
    "can reduce/select-clause->value + where with filter/XR" to kt(
      "select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; where(p.name == Joe); p.name }.filter { t -> t == JoeJoe }"
    ),
    "can reduce/select-clause->value + where with filter" to cr(
      "SELECT p.name AS value FROM Person p INNER JOIN Address a ON a.personId = p.id WHERE p.name = 'Joe' AND p.name = 'JoeJoe'"
    ),
    "can reduce/select stuff, (select-clause->value + where with filter(___ + stuff <pure>))/XR" to kt(
      "select { val stuff = from(Table(Stuff)); val innerQuery = /*ASI*/ select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; where(p.name == Joe); p.name }.filter { t -> t == JoeJoe && stuff.extra == ext }.toExpr; innerQuery.toExpr }"
    ),
    "can reduce/select stuff, (select-clause->value + where with filter(___ + stuff <pure>))" to cr(
      "SELECT (SELECT p.name AS name FROM Person p INNER JOIN Address a ON a.personId = p.id WHERE p.name = 'Joe' AND p.name = 'JoeJoe' AND stuff.extra = 'ext') AS value FROM Stuff stuff"
    ),
    "cannot reduce/anything that contains impurities e g  impure inlines/XR" to kt(
      """select { val stuff = from(Table(Stuff)); val innerQuery = /*ASI*/ select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; where(p.name == Joe); Tuple(first = p.name, second = free("rand()").invoke()) }.filter { t -> t.first == JoeJoe && stuff.extra == ext }.toExpr; innerQuery.toExpr }"""
    ),
    "cannot reduce/anything that contains impurities e g  impure inlines" to cr(
      "SELECT (SELECT t.first AS first, t.second AS second FROM (SELECT p.name AS first, rand() AS second FROM Person p INNER JOIN Address a ON a.personId = p.id WHERE p.name = 'Joe') AS t WHERE t.first = 'JoeJoe' AND stuff.extra = 'ext') FROM Stuff stuff"
    ),
  )
}
