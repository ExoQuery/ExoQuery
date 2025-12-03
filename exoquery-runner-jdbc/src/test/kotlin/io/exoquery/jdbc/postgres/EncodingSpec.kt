package io.exoquery.jdbc.postgres

import io.exoquery.*
import io.exoquery.controller.runActions
import io.exoquery.jdbc.TestDatabases
import io.exoquery.jdbc.encodingdata.JavaTestEntity
import io.exoquery.jdbc.encodingdata.verify
import io.kotest.core.spec.style.FreeSpec

class EncodingSpec: FreeSpec({
  val ctx = TestDatabases.postgres

  beforeEach {
    ctx.runActions(
      """
      DELETE FROM JavaTestEntity
      """
    )
  }

  "Encode/Decode Java Types" - {
    val ent = JavaTestEntity.regular
    "insert" {
      sql {
        insert<JavaTestEntity> {
          set(
            bigDecimalMan to param(ent.bigDecimalMan),
            javaUtilDateMan to param(ent.javaUtilDateMan),
            uuidMan to param(ent.uuidMan),
            bigDecimalOpt to param(ent.bigDecimalOpt),
            javaUtilDateOpt to param(ent.javaUtilDateOpt),
            uuidOpt to param(ent.uuidOpt)
          )
        }
      }.buildFor.Postgres().runOn(ctx)
      val res = sql { Table<JavaTestEntity>() }.buildFor.Postgres().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
    "insert - setParams" {
      sql {
        insert<JavaTestEntity> { setParams(ent) }
      }.buildFor.Postgres().runOn(ctx)
      val res = sql { Table<JavaTestEntity>() }.buildFor.Postgres().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
  }
})
