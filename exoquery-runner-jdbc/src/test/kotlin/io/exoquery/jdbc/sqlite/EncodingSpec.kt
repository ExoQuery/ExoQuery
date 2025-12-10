package io.exoquery.jdbc.sqlite

import io.exoquery.*
import io.exoquery.controller.TerpalSqlInternal
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.TestDatabases
import io.exoquery.jdbc.encodingdata.JavaTestEntity
import io.exoquery.jdbc.encodingdata.EncodingTestEntity
import io.exoquery.jdbc.encodingdata.verify
import io.kotest.core.spec.style.FreeSpec

class EncodingSpec: FreeSpec({
  val ctx = TestDatabases.sqlite

  @OptIn(TerpalSqlUnsafe::class)
  beforeEach {
    ctx.runActionsUnsafe(
      """
      DELETE FROM JavaTestEntity;
      DELETE FROM EncodingTestEntity
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
      }.buildFor.Sqlite().runOn(ctx)
      val res = sql { Table<JavaTestEntity>() }.buildFor.Sqlite().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
    "insert - setParams" {
      sql {
        insert<JavaTestEntity> { setParams(ent) }
      }.buildFor.Sqlite().runOn(ctx)
      val res = sql { Table<JavaTestEntity>() }.buildFor.Sqlite().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
  }
  "Encode/Decode EncodingTestEntity" - {
    val ent = EncodingTestEntity.regular
    "insert - ctx" {
      sql {
        insert<EncodingTestEntity> {
          set(
            stringMan to param(ent.stringMan),
            booleanMan to param(ent.booleanMan),
            byteMan to param(ent.byteMan),
            shortMan to param(ent.shortMan),
            intMan to param(ent.intMan),
            longMan to param(ent.longMan),
            floatMan to param(ent.floatMan),
            doubleMan to param(ent.doubleMan),
            byteArrayMan to param(ent.byteArrayMan),
            customMan to param(ent.customMan),
            stringOpt to param(ent.stringOpt),
            booleanOpt to param(ent.booleanOpt),
            byteOpt to param(ent.byteOpt),
            shortOpt to param(ent.shortOpt),
            intOpt to param(ent.intOpt),
            longOpt to param(ent.longOpt),
            floatOpt to param(ent.floatOpt),
            doubleOpt to param(ent.doubleOpt),
            byteArrayOpt to param(ent.byteArrayOpt),
            customOpt to param(ent.customOpt)
          )
        }
      }.buildFor.Sqlite().runOn(ctx)
      val res = sql { Table<EncodingTestEntity>() }.buildFor.Sqlite().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
    "insert - ctx" {
      sql {
        insert<EncodingTestEntity> {
          set(
            stringMan to param(ent.stringMan),
            booleanMan to param(ent.booleanMan),
            byteMan to paramCtx(ent.byteMan),
            shortMan to param(ent.shortMan),
            intMan to param(ent.intMan),
            longMan to param(ent.longMan),
            floatMan to param(ent.floatMan),
            doubleMan to param(ent.doubleMan),
            byteArrayMan to paramCtx(ent.byteArrayMan),
            customMan to param(ent.customMan),
            stringOpt to param(ent.stringOpt),
            booleanOpt to param(ent.booleanOpt),
            byteOpt to paramCtx(ent.byteOpt),
            shortOpt to param(ent.shortOpt),
            intOpt to param(ent.intOpt),
            longOpt to param(ent.longOpt),
            floatOpt to param(ent.floatOpt),
            doubleOpt to param(ent.doubleOpt),
            byteArrayOpt to paramCtx(ent.byteArrayOpt),
            customOpt to param(ent.customOpt)
          )
        }
      }.buildFor.Sqlite().runOn(ctx)
      val res = sql { Table<EncodingTestEntity>() }.buildFor.Sqlite().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
    "insert - setParams" {
      sql {
        insert<EncodingTestEntity> { setParams(ent) }
      }.buildFor.Sqlite().runOn(ctx)
      val res = sql { Table<EncodingTestEntity>() }.buildFor.Sqlite().runOn(ctx).let { it.firstOrNull() ?: error("Expected one element list but got: ${it}") }
      verify(res, ent)
    }
  }
})
