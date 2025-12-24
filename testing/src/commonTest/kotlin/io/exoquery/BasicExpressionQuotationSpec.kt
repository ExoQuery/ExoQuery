package io.exoquery

import io.exoquery.serial.ParamSerializer
import io.exoquery.xr.`+++`
import io.exoquery.xr.XR
import io.exoquery.xr.XR.Location
import io.exoquery.xr.XR.ParamType
import io.exoquery.xr.XRType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.equals.shouldBeEqual

fun XR.TagForParam.Companion.Minimal(id: BID, paramType: ParamType, type: XRType, loc: Location = Location.Synth) =
  XR.TagForParam(id, paramType, null, XR.ClassId.Empty, type, loc)

class BasicExpressionQuotationSpec : FreeSpec({
  "static quotation mechanics" - {
    "c0={n0+lift}, c1=c0, c={n1+c1} -> {n1+(n0+lift)}" {
      val cap0 = sql.expression { 123 + param(456) }
      cap0.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(123) `+++` XR.TagForParam.Minimal(BID("0"), XR.ParamType.Single, XRType.Value, XR.Location.Synth),
        RuntimeSet.Empty,
        ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
      )

      val cap1 = cap0
      val cap = sql.expression { 789 + cap1.use }

      // Note that loc properties are different but those are not used in XR equals functions
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` (XR.Const.Int(123) `+++` XR.TagForParam.Minimal(
          BID("0"),
          XR.ParamType.Single,
          XRType.Value,
          XR.Location.Synth
        )),
        RuntimeSet.Empty,
        ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
      )
    }
    "c0()={n0+lift}, c={n1+c0()} -> {n1+(n0+lift)}" {
      fun cap0() = sql.expression { 123 + param(456) }
      val cap = sql.expression { 789 + cap0().use }
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` (XR.Const.Int(123) `+++` XR.TagForParam.Minimal(
          BID("0"),
          XR.ParamType.Single,
          XRType.Value,
          XR.Location.Synth
        )),
        RuntimeSet.Empty,
        ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
      )
    }
    "c0(i)={n0+i}, c={n1+c0(nn)} -> {n1+(n0+nn)}" {
      fun cap0(input: Int) = sql.expression { 123 + param(input) }
      val cap = sql.expression { 789 + cap0(456).use }
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` (XR.Const.Int(123) `+++` XR.TagForParam(
          BID("0"),
          XR.ParamType.Single,
          null, XR.ClassId.Empty, // unused for the comparision
          XRType.Value,
          XR.Location.Synth
        )),
        RuntimeSet.Empty,
        ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
      )
    }

    "cls Foo{c0=nA+lift}, f=Foo, c={nB+f.c0} -> {nB+(nA+lift)}" {
      class Foo {
        val cap0 = sql.expression { 456 + param(456) }
      }

      val f = Foo()
      val cap = sql.expression { 789 + f.cap0.use }
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` (XR.Const.Int(456) `+++` XR.TagForParam.Minimal(BID("0"), XR.ParamType.Single, XRType.Value)),
        RuntimeSet.Empty,
        ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
      )
    }
    "cls Foo{c0()=nA}, f=Foo, c={nB+f.c0()} -> {nB+(nA)}" {
      class Foo {
        fun cap0() = sql.expression { 456 }
      }

      val f = Foo()
      val cap = sql.expression { 789 + f.cap0().use }
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` XR.Const.Int(456),
        RuntimeSet.Empty,
        ParamSet.of()
      )
    }
  }
  "dynamic quotation mechanics" - {
    "c0D=dyn{nA}, c={nB+c0D} -> {nB+T(B0),R={B0,nA}}" {
      val x = true

      @SqlDynamic
      fun cap0() =
        if (x)
          sql.expression { 123 }
        else
          sql.expression { 456 }

      val cap = sql.expression { 789 + cap0().use }

      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` XR.TagForSqlExpression(BID("0"), XRType.Value),
        RuntimeSet.of(BID("0") to SqlExpression<Any>(XR.Const.Int(123), RuntimeSet.of(), ParamSet.of())),
        ParamSet.of()
      )
    }
    "cls Foo{c0D=dyn{nA+lift}}, f=Foo, c={nB+f.c0D} -> {nB+T(B0),R={B0,nA+lift}}" {
      val v = true

      class Foo {
        @SqlDynamic
        val cap0 =
          if (v) sql.expression { 456 + param(456) } else sql.expression { 456 + param(999) }
      }

      val f = Foo()
      val cap = sql.expression { 789 + f.cap0.use }

      println(cap.determinizeDynamics().show())
      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` XR.TagForSqlExpression(BID("1"), XRType.Value),
        RuntimeSet.of(
          BID("1") to
              SqlExpression<Any>(
                XR.Const.Int(456) `+++` XR.TagForParam.Minimal(BID("0"), XR.ParamType.Single, XRType.Value),
                RuntimeSet.of(),
                ParamSet(listOf(ParamSingle(BID("0"), 456, ParamSerializer.Int)))
              )
        ),
        ParamSet.of()
      )
    }
    "cls Foo{c0D(i)=dyn{nA+lift(i)}}, f=Foo, c={nB+f.c0D(nn)} -> {nB+T(B0),R={B0,nA+lift(nn)}}" {
      val v = true

      class Foo {
        @SqlDynamic
        fun cap0(input: SqlExpression<Int>) =
          if (v) sql.expression { 101112 + input.use } else sql.expression { 456 + param(999) }
      }

      val f = Foo()
      val cap = sql.expression { 789 + f.cap0(sql.expression { 456 }).use }

      println(cap.determinizeDynamics().show())

      cap.determinizeDynamics() shouldBeEqual SqlExpression(
        XR.Const.Int(789) `+++` XR.TagForSqlExpression(BID("1"), XRType.Value),
        RuntimeSet.of(
          BID("1") to
              SqlExpression<Any>(
                XR.Const.Int(101112) `+++` XR.TagForSqlExpression(BID("0"), XRType.Value),
                RuntimeSet.of(
                  BID("0") to SqlExpression<Any>(
                    XR.Const.Int(456),
                    RuntimeSet.of(),
                    ParamSet.of()
                  )
                ),
                ParamSet.Empty
              )
        ),
        ParamSet.of()
      )
    }
  }
})
