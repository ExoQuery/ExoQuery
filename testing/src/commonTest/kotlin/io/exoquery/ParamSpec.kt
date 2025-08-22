//package io.exoquery
//
//import io.exoquery.serial.ParamSerializer
//import io.exoquery.serial.contextualSerializer
//import io.exoquery.sql.PostgresDialect
//import io.exoquery.sql.Renderer
//import io.exoquery.testdata.Person
//import io.exoquery.testdata.pIdent
//import io.exoquery.testdata.personEnt
//import io.exoquery.xr.OP
//import io.exoquery.xr.XR
//import io.exoquery.xr.XRType
//import io.kotest.core.spec.style.FreeSpec
//import io.kotest.matchers.shouldBe
//import kotlinx.datetime.LocalDate
//import kotlinx.serialization.ContextualSerializer
//import kotlinx.serialization.Serializable
//
//// TODO need to test all the kinds of params on the IR level
//// TODO then need to implement `params` calls that have custom tokenization
//// TODO to think about, DSL needs to at least support let-functions, maybe also run
//class ParamSpec: FreeSpec({
//
//  "fullQuery" - {
//    "param" {
//      // TODO need to also test dynamic-path of this
//      val cap = capture { Table<Person>().filter { p -> p.name == param("name") } }
//      cap.determinizeDynamics() shouldBe SqlQuery(
//        XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "name"), OP.EqEq, XR.TagForParam.valueTag("0"))),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), "name", ParamSerializer.String)))
//      )
//      val build = cap.build<PostgresDialect>()
//      build.token.build() shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = ?"
//      val buildDet = build.determinizeDynamics()
//      buildDet.token.renderWith(Renderer()) shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:name}"
//      buildDet.params.map { it.withNonStrictEquality() } shouldBe listOf(
//        ParamSingle(
//          BID("0"),
//          "name",
//          ParamSerializer.String
//        )
//      )
//
//      val buildRuntime = cap.buildRuntime(PostgresDialect(), null)
//      buildRuntime.token.build() shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = ?"
//      val buildRuntimeDet = buildRuntime.determinizeDynamics()
//      buildRuntimeDet.token.renderWith(Renderer()) shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:name}"
//      buildRuntimeDet.params.map { it.withNonStrictEquality() } shouldBe listOf(
//        ParamSingle(
//          BID("0"),
//          "name",
//          ParamSerializer.String
//        )
//      )
//    }
//    "params" {
//      // TODO need to also test dynamic-path of this
//      val cap =
//        capture { Table<Person>().filter { p -> p.name in params(listOf("name1", "name2")) } }.determinizeDynamics()
//      cap shouldBe SqlQuery(
//        // It should be a standard filter but the `in` should be rendered as a list.contains(value) XR.MethodCall instances
//        XR.Filter(
//          personEnt,
//          pIdent,
//          XR.MethodCall(
//            XR.TagForParam.valueTag("0"),
//            "contains",
//            listOf(XR.Property(pIdent, "name")),
//            XR.CallType.PureFunction,
//            XR.ClassId("io.exoquery", "Params"),
//            false,
//            XRType.Value
//          )
//        ),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamMulti(BID("0"), listOf("name1", "name2"), ParamSerializer.String)))
//      )
//      val build = cap.build<PostgresDialect>()
//      build.token.build() shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name IN (?, ?)"
//      val buildDet = build.determinizeDynamics()
//      buildDet.token.renderWith(Renderer()) shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name IN ({0:[name1, name2]})"
//      buildDet.params.map { it.withNonStrictEquality() } shouldBe listOf(
//        ParamMulti(
//          BID("0"),
//          listOf("name1", "name2"),
//          ParamSerializer.String
//        )
//      )
//
//      val buildRuntime = cap.buildRuntime(PostgresDialect(), null)
//      buildRuntime.token.build() shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name IN (?, ?)"
//      val buildRuntimeDet = buildRuntime.determinizeDynamics()
//      buildRuntimeDet.token.renderWith(Renderer()) shouldBe "SELECT p.id, p.name, p.age FROM Person p WHERE p.name IN ({0:[name1, name2]})"
//      buildRuntimeDet.params.map { it.withNonStrictEquality() } shouldBe listOf(
//        ParamMulti(
//          BID("0"),
//          listOf("name1", "name2"),
//          ParamSerializer.String
//        )
//      )
//    }
//  }
//  "param" - {
//    "string" {
//      capture.expression { param("name") }.determinizeDynamics() shouldBe SqlExpression(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), "name", ParamSerializer.String)))
//      )
//    }
//    "int" {
//      capture.expression { param(123) }.determinizeDynamics() shouldBe SqlExpression(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), 123, ParamSerializer.Int)))
//      )
//    }
//  }
//
//  val date = LocalDate(2021, 1, 1)
//
//  @Serializable
//  data class MyCustomDate(val year: Int, val month: Int, val day: Int)
//
//  val myDate = MyCustomDate(2021, 1, 1)
//
//  "paramCtx" - {
//    "date" {
//      val cap = capture.expression { paramCtx(date) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), date, contextualSerializer<kotlinx.datetime.LocalDate>())))
//      ).withNonStrictEquality()
//    }
//    "myCustomDate" {
//      val cap = capture.expression { paramCtx(myDate) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), myDate, contextualSerializer<MyCustomDate>())))
//      ).withNonStrictEquality()
//    }
//  }
//
//  "paramCustom" - {
//    "date" {
//      val cap = capture.expression { paramCustom(date, ContextualSerializer(LocalDate::class)) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), date, contextualSerializer<kotlinx.datetime.LocalDate>())))
//      ).withNonStrictEquality()
//    }
//    "myCustomDate" {
//      val cap = capture.expression { paramCustom(myDate, ContextualSerializer(MyCustomDate::class)) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), myDate, contextualSerializer<MyCustomDate>())))
//      ).withNonStrictEquality()
//    }
//  }
//
//  "paramCustomValue" - {
//    "date" {
//      val v = ValueWithSerializer.invoke(date, ContextualSerializer(LocalDate::class))
//      val cap = capture.expression { param(v) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), date, contextualSerializer<LocalDate>())))
//      ).withNonStrictEquality()
//    }
//    "myDate" {
//      val v = ValueWithSerializer.invoke(myDate, ContextualSerializer(MyCustomDate::class))
//      capture.expression { param(v) }.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamSingle(BID("0"), myDate, contextualSerializer<MyCustomDate>())))
//      ).withNonStrictEquality()
//    }
//  }
//
//  "params" {
//    val myList = listOf(1, 2, 3)
//    val cap = capture.expression { params(myList) }
//    cap.xr.type shouldBe XRType.Value
//    cap.determinizeDynamics() shouldBe SqlExpression(
//      XR.TagForParam.valueTag("0"),
//      RuntimeSet.Empty,
//      ParamSet(listOf(ParamMulti(BID("0"), myList, ParamSerializer.Int)))
//    )
//  }
//
//  "paramsCustom" - {
//    "date" {
//      val myList = listOf(date, date)
//      val cap = capture.expression { paramsCustom(myList, ContextualSerializer(LocalDate::class)) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamMulti(BID("0"), myList, contextualSerializer<LocalDate>())))
//      ).withNonStrictEquality()
//    }
//    "myCustomDate" {
//      val myList = listOf(myDate, myDate)
//      val cap = capture.expression { paramsCustom(myList, ContextualSerializer(MyCustomDate::class)) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamMulti(BID("0"), myList, contextualSerializer<MyCustomDate>())))
//      ).withNonStrictEquality()
//    }
//  }
//
//  "paramsCustomValue" - {
//    "date" {
//      val myList = listOf(date, date)
//      val v = ValuesWithSerializer.invoke(myList, ContextualSerializer(LocalDate::class))
//      val cap = capture.expression { params(v) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamMulti(BID("0"), myList, contextualSerializer<LocalDate>())))
//      ).withNonStrictEquality()
//    }
//    "myCustomDate" {
//      val myList = listOf(myDate, myDate)
//      val v = ValuesWithSerializer.invoke(myList, ContextualSerializer(MyCustomDate::class))
//      val cap = capture.expression { params(v) }
//      cap.xr.type shouldBe XRType.Value
//      cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlExpression<LocalDate>(
//        XR.TagForParam.valueTag("0"),
//        RuntimeSet.Empty,
//        ParamSet(listOf(ParamMulti(BID("0"), myList, contextualSerializer<MyCustomDate>())))
//      ).withNonStrictEquality()
//    }
//  }
//})
