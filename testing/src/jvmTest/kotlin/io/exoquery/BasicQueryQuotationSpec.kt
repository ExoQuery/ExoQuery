package io.exoquery

import io.exoquery.xr.OP
import io.exoquery.xr.XR
import io.exoquery.xr.XRType
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.equals.shouldBeEqual

class BasicQueryQuotationSpec : FreeSpec({
  data class Person(val id: Int, val name: String, val age: Int)
  data class Robot(val ownerId: Int, val model: String)

  val personTpe = XRType.Product("Person", listOf("id" to XRType.Value, "name" to XRType.Value, "age" to XRType.Value))
  val robotTpe = XRType.Product("Robot", listOf("ownerId" to XRType.Value, "model" to XRType.Value))
  val personEnt = XR.Entity("Person", personTpe)
  val robotEnt = XR.Entity("Robot", robotTpe)
  val pIdent = XR.Ident("p", personTpe)
  val rIdent = XR.Ident("r", robotTpe)


  // Testing of the query DSL note that the quotation mechanics are mostly testing in BasicExpressionQuotationSpec,
  // here we are largely testing the DSL mechanics
  "dsl features" - {
    "table" {
      val cap = capture { Table<Person>() }
      cap.determinizeDynamics() shouldBeEqual SqlQuery(
        personEnt,
        RuntimeSet.Empty,
        ParamSet.Empty
      )
      // Some XR check omit the types so want to explicitly check this just in case
      cap.xr.type shouldBeEqual personTpe
    }
    "map" {
      val cap0 = capture { Table<Person>() }
      val cap = capture { cap0.map { p -> p.name } }
      cap.determinizeDynamics() shouldBeEqual SqlQuery(
        XR.Map(personEnt, pIdent, XR.Property(pIdent, "name")),
        RuntimeSet.Empty,
        ParamSet.Empty
      )
    }
    "flatMap" {
      val cap0 = capture { Table<Robot>() }
      val cap = capture { cap0.flatMap { r -> Table<Person>() } }
      cap.determinizeDynamics() shouldBeEqual SqlQuery(
        XR.FlatMap(robotEnt, rIdent, personEnt),
        RuntimeSet.Empty,
        ParamSet.Empty
      )
    }
    "filter" {
      val cap0 = capture { Table<Person>() }
      val cap = capture { cap0.filter { p -> p.age > 18 } }
      cap.determinizeDynamics() shouldBeEqual SqlQuery(
        XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "age"), OP.Gt, XR.Const.Int(18))),
        RuntimeSet.Empty,
        ParamSet.Empty
      )
    }
    // TODO need to build special logic for converting name.toList into "unnest(name)" and probably need to have a custom parseable unnest in the DSL or something like that
    //"concatMap" {
    //  val cap0 = capture { Table<Person>() }
    //  val cap = capture { cap0.concatMap { p -> p.name.toList() } }
    //  cap.determinizeDynamics() shouldBeEqual SqlQuery(
    //    XR.ConcatMap(personEnt, pIdent, ???),
    //    Runtimes.Empty,
    //    Params.Empty
    //  )
    //}
    "union and unionAll" - {
      val cap0 = capture { Table<Person>() }
      val capA = capture { cap0.filter { p -> p.name == "A" } }
      val capB = capture { cap0.filter { p -> p.name == "B" } }

      "union" {
        val cap = capture { capA.union(capB) }
        cap.determinizeDynamics() shouldBeEqual SqlQuery(
          XR.Union(
            XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "name"), OP.EqEq, XR.Const.String("A"))),
            XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "name"), OP.EqEq, XR.Const.String("B")))
          ),
          RuntimeSet.Empty,
          ParamSet.Empty
        )
      }
      "unionAll" {
        val cap = capture { capA.unionAll(capB) }
        cap.determinizeDynamics() shouldBeEqual SqlQuery(
          XR.UnionAll(
            XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "name"), OP.EqEq, XR.Const.String("A"))),
            XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "name"), OP.EqEq, XR.Const.String("B")))
          ),
          RuntimeSet.Empty,
          ParamSet.Empty
        )
      }
    }
  }
  "dynamic quotation mechanics" - {
    val tru = true
    "c0D=dyn{TableA}, c={c0D.map(...)} -> {T(B0).map(...),R={B0,TableA}}" {
      val cap0 =
        if (tru) {
          capture { Table<Person>() }
        } else {
          throw IllegalArgumentException("Should not be here")
        }
      val cap = capture {
        cap0.map { p -> p.name }
      }
      cap.determinizeDynamics() shouldBeEqual SqlQuery(
        XR.Map(XR.TagForSqlQuery(BID("0"), personTpe), pIdent, XR.Property(pIdent, "name")),
        RuntimeSet.of(BID("0") to SqlQuery<Person>(personEnt, RuntimeSet.Empty, ParamSet.Empty)),
        ParamSet.Empty
      )
    }
    // Too many possible edge cases so do not allow this. If there's an external function being called it should be captured or annotated in some way
    // "c0D=(i)->dyn{TableA.filter(i)}, c={c0D.map(...)} -> {T(B0).map(...),R={B0,TableA}}" {
    //   fun cap0(i: Int) =
    //     if (tru) {
    //       capture { Table<Person>().filter { p -> p.age == param(i) } }
    //     } else {
    //       throw IllegalArgumentException("Should not be here")
    //     }
    //   val cap = capture {
    //     cap0(123).map { p -> p.name }
    //   }
    //   cap.determinizeDynamics().withNonStrictEquality() shouldBe SqlQuery<String>(
    //     XR.Map(XR.TagForSqlQuery(BID("1"), personTpe), pIdent, XR.Property(pIdent, "name")),
    //     RuntimeSet.of(BID("1") to
    //       SqlQuery<Person>(
    //         xr = XR.Filter(personEnt, pIdent, XR.BinaryOp(XR.Property(pIdent, "age"), OP.EqEq, XR.TagForParam.valueTag("0"))),
    //         runtimes = RuntimeSet.Empty,
    //         params = ParamSet(listOf(ParamSingle(BID("0"), 123, ParamSerializer.Int)))
    //       )
    //     ),
    //     ParamSet.Empty
    //   ).withNonStrictEquality()
    // }
  }
})
