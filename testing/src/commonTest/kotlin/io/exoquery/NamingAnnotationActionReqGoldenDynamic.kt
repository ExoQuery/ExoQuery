package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object NamingAnnotationActionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "action with rename/should work (and quote) in insert" to cr(
      """INSERT INTO "PERSON" ("NAME", age) VALUES ('Joe', 123) RETURNING "ID", "NAME""""
    ),
    "action with rename/should work (and quote) in insert using self column" to cr(
      """INSERT INTO "PERSON" ("NAME", age) VALUES (("NAME" || '_Suffix'), 123) RETURNING "ID", "NAME""""
    ),
    "action with rename/should work (and quote) in insert using self column with onConflict ignore" to cr(
      """INSERT INTO "PERSON" ("NAME", age) VALUES ('Joe', 123) ON CONFLICT ("ID") DO NOTHING"""
    ),
    "action with rename/should work (and quote) in insert using self column with onConflict update" to cr(
      """INSERT INTO "PERSON" AS x ("NAME", age) VALUES ('Joe', 123) ON CONFLICT ("ID") DO UPDATE SET "NAME" = (EXCLUDED."NAME" || '_Suffix')"""
    ),
    "nested action with rename/should work (and quote) in insert" to cr(
      """INSERT INTO "PERSON" ("FIRST", age) VALUES ('Joe', 123) RETURNING "ID", "FIRST", last"""
    ),
    "nested action with rename/should work (and quote) in insert using self column" to cr(
      """INSERT INTO "PERSON" ("FIRST", age) VALUES (("FIRST" || '_Suffix'), 123) RETURNING "ID", "FIRST", last"""
    ),
    "nested action with rename/should work (and quote) in insert using self column with onConflict ignore" to cr(
      """INSERT INTO "PERSON" ("FIRST", age) VALUES ('Joe', 123) ON CONFLICT ("FIRST") DO NOTHING"""
    ),
    "nested action with rename/should work (and quote) in insert using self column with onConflict update" to cr(
      """INSERT INTO "PERSON" AS x ("FIRST", age) VALUES ('Joe', 123) ON CONFLICT ("ID") DO UPDATE SET "FIRST" = (EXCLUDED."FIRST" || '_Suffix')"""
    ),
    "nested action with rename - intermediates renames should not matter/should work (and quote) in insert" to cr(
      """INSERT INTO "PERSON" ("NAME_FIRST", age) VALUES ('Joe', 123) RETURNING "ID", "NAME""""
    ),
    "nested action with rename - intermediates renames should not matter/should work (and quote) in insert using self column" to cr(
      """INSERT INTO "PERSON" ("NAME_FIRST", age) VALUES (("NAME_FIRST" || '_Suffix'), 123) RETURNING "ID", "NAME""""
    ),
    "nested action with rename - intermediates renames should not matter/should work (and quote) in insert using self column with onConflict ignore" to cr(
      """INSERT INTO "PERSON" ("NAME_FIRST", age) VALUES ('Joe', 123) ON CONFLICT ("NAME_FIRST") DO NOTHING"""
    ),
    "nested action with rename - intermediates renames should not matter/should work (and quote) in insert using self column with onConflict update" to cr(
      """INSERT INTO "PERSON" AS x ("NAME_FIRST", age) VALUES ('Joe', 123) ON CONFLICT ("ID") DO UPDATE SET "NAME_FIRST" = (EXCLUDED."NAME_FIRST" || '_Suffix')"""
    ),
  )
}
