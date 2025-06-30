package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object FreeReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "static free/simple sql function" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE MyFunction(p.age)"
    ),
    "static free/simple sql function - pure" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE MyFunction(p.age)"
    ),
    "static free/simple sql function - condition" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE MyFunction(p.age)"
    ),
    "static free/simple sql function - condition" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE MyFunction(p.age)"
    ),
    "query with free/static query in free" to cr(
      "beforeStuff() SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.name = 'Joe' afterStuff()"
    ),
    "query with free/dynamic query in free" to cr(
      "beforeStuff() SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.name = 'Joe' afterStuff()"
    ),
    "query with free/direct query in free" to cr(
      "beforeStuff() SELECT p.id AS id, p.name AS name, p.age AS age FROM Person p WHERE p.name = 'Joe' afterStuff()"
    ),
    "action with free/static action in free" to cr(
      "beforeStuff() INSERT INTO Person (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) afterStuff()",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "action with free/dynamic action in free" to cr(
      "beforeStuff() INSERT INTO Person (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) afterStuff()",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "action with free/direct action in free" to cr(
      "beforeStuff() INSERT INTO Person (id, name, age) VALUES ({0:1}, {1:Joe}, {2:123}) afterStuff()",
      "0" to "1", "1" to "Joe", "2" to "123"
    ),
    "action with free/whole action in free" to cr(
      """
      
                  CREATE TABLE Launch (
                      flightNumber INTEGER NOT NULL,
                      missionName TEXT NOT NULL,
                      details TEXT,
                      launchSuccess INTEGER DEFAULT NULL,
                      launchDateUTC TEXT NOT NULL,
                      patchUrlSmall TEXT,
                      patchUrlLarge TEXT,
                      articleUrl TEXT
                  )
                
      """
    ),
  )
}
