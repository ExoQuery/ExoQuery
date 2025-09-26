package io.exoquery.xr

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.exoquery.xr.StatelessTransformerSpec.Subject as StatelessSubject
import io.exoquery.xr.StatefulTransformerSpec.Subject as StatefulSubject

class SelectClauseTransformSpec: FreeSpec({

  "stateless transform" - {
    "select clause" {
      val select = SelectClause.of(
        assignments = listOf(
          SX.From(XR.Ident("a"), XR.Entity("A")),
          SX.Join(XR.JoinType.Inner, XR.Ident("b"), XR.Entity("B"), XR.Ident("c"), XR.BinaryOp(XR.Ident("c"), OP.EqEq, XR.Ident("a")))
        ),
        where = SX.Where(XR.BinaryOp(XR.Ident("a"), OP.Gt, XR.Const.Int(5))),
        having = SX.Having(XR.BinaryOp(XR.Ident("a"), OP.Lt, XR.Const.Int(10))),
        groupBy = SX.GroupBy(XR.Ident("a")),
        sortBy = SX.SortBy(listOf(XR.OrderField.By(XR.Ident("a"), XR.Ordering.AscNullsFirst))),
        select = XR.Ident("a"),
        type = XRType.Value
      )

      val expectedClause =
        SelectClause.of(
          assignments = listOf(
            // NOTE: Just like for flatMaps/maps/etc... where we don't transform the variable, we don't transform
            //       the here either because ultimately they will become the these things.
            SX.From(variable = XR.Ident("a"), XR.Entity("A'")),
            SX.Join(XR.JoinType.Inner, variable = XR.Ident("b"), XR.Entity("B'"), conditionVariable = XR.Ident("c"), XR.BinaryOp(XR.Ident("c'"), OP.EqEq, XR.Ident("a'")))
          ),
          where = SX.Where(XR.BinaryOp(XR.Ident("a'"), OP.Gt, XR.Const.Int(50))),
          having = SX.Having(XR.BinaryOp(XR.Ident("a'"), OP.Lt, XR.Const.Int(100))),
          groupBy = SX.GroupBy(XR.Ident("a'")),
          sortBy = SX.SortBy(listOf(XR.OrderField.By(XR.Ident("a'"), XR.Ordering.AscNullsFirst))),
          select = XR.Ident("a'"),
          type = XRType.Value
        )

      select.handleStatelessTransform(
        StatelessSubject(
          XR.Ident("a") to XR.Ident("a'"),
          XR.Ident("b") to XR.Ident("b'"),
          XR.Ident("c") to XR.Ident("c'"),
          XR.Entity("A") to XR.Entity("A'"),
          XR.Entity("B") to XR.Entity("B'"),
          XR.Const.Int(5) to XR.Const.Int(50),
          XR.Const.Int(10) to XR.Const.Int(100)
        )
      ) shouldBe expectedClause

    }
  }

  "stateful transformer" - {
    "select clause" {
      val select = SelectClause.of(
        assignments = listOf(
          SX.From(variable = XR.Ident("a"), XR.Entity("A")),
          SX.Join(XR.JoinType.Inner, variable = XR.Ident("b"), XR.Entity("B"), conditionVariable = XR.Ident("c"), XR.BinaryOp(XR.Ident("c"), OP.EqEq, XR.Ident("a")))
        ),
        where = SX.Where(XR.BinaryOp(XR.Ident("a"), OP.Gt, XR.Const.Int(5))),
        having = SX.Having(XR.BinaryOp(XR.Ident("a"), OP.Lt, XR.Const.Int(10))),
        groupBy = SX.GroupBy(XR.Ident("a")),
        sortBy = SX.SortBy(listOf(XR.OrderField.By(XR.Ident("a"), XR.Ordering.AscNullsFirst))),
        select = XR.Ident("a"),
        type = XRType.Value
      )

      val expectedClause =
        SelectClause.of(
          assignments = listOf(
            // NOTE: Just like for flatMaps/maps/etc... where we don't transform the variable, we don't transform
            //       the here either because ultimately they will become the these things.
            SX.From(variable = XR.Ident("a"), XR.Entity("A'")),
            SX.Join(XR.JoinType.Inner, variable = XR.Ident("b"), XR.Entity("B'"), conditionVariable = XR.Ident("c"), XR.BinaryOp(XR.Ident("c'"), OP.EqEq, XR.Ident("a'")))
          ),
          where = SX.Where(XR.BinaryOp(XR.Ident("a'"), OP.Gt, XR.Const.Int(50))),
          having = SX.Having(XR.BinaryOp(XR.Ident("a'"), OP.Lt, XR.Const.Int(100))),
          groupBy = SX.GroupBy(XR.Ident("a'")),
          sortBy = SX.SortBy(listOf(XR.OrderField.By(XR.Ident("a'"), XR.Ordering.AscNullsFirst))),
          select = XR.Ident("a'"),
          type = XRType.Value
        )

      val (at, att) = select.handleStatefulTransformer(
        StatefulSubject(
          listOf(),
          XR.Ident("a") to XR.Ident("a'"),
          XR.Ident("b") to XR.Ident("b'"),
          XR.Ident("c") to XR.Ident("c'"),
          XR.Entity("A") to XR.Entity("A'"),
          XR.Entity("B") to XR.Entity("B'"),
          XR.Const.Int(5) to XR.Const.Int(50),
          XR.Const.Int(10) to XR.Const.Int(100)
        )
      )
      at shouldBe expectedClause
      att.state shouldBe listOf(
        XR.Entity("A"),
        XR.Entity("B"),
        XR.Ident("c"),
        XR.Ident("a"),
        XR.Ident("a"),
        XR.Const.Int(5),
        XR.Ident("a"),
        XR.Const.Int(10),
        XR.Ident("a"),
        XR.Ident("a"),
        XR.Ident("a")
      )
    }
  }
})
