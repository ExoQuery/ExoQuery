package io.exoquery.lang

import io.exoquery.xr.XR
import io.exoquery.xr.XR.*

object UnnestProperty {
  operator fun invoke(ast: XR.Expression): Pair<XR.Expression, List<String>> =
    when {
      // If the property is hidden or it itself yields a product (i.e. it's a property that dereferences an embedded class)
      ast is Property && (ast.visibility == Visibility.Hidden || ast.type.isProduct()) -> {
        val (a, nestedName) = invoke(ast.of)
        a to nestedName
      }
      ast is Property -> {
        val (a, nestedName) = invoke(ast.of)
        a to (nestedName + ast.name)
      }
      //ast is ExternalIdent && ast.visibility == Visibility.Fixed -> ast to listOf(ast.name)
      else -> ast to emptyList()
    }

// Scala:
//    def unnest(ast: Ast): (Ast, List[String]) =
//      ast match {
//        case Property.Opinionated(a, _, _, Hidden) =>
//          unnest(a) match {
//            case (a, nestedName) => (a, nestedName)
//          }
//        // Append the property name. This includes tuple indexes.
//        case Property(a, name) =>
//          unnest(a) match {
//            case (ast, nestedName) =>
//              (ast, nestedName :+ name)
//          }
//        case e @ ExternalIdent.Opinionated(a, _, Fixed) => (e, List(a))
//        case a                                          => (a, Nil)
//      }
}

// Scala:
//  object TokenizeProperty {

//
//    def apply(name: String, prefix: List[String]) =
//      prefixRenameable match {
//        case Renameable.Fixed =>
//          // Typically this happens in a nested query on an multi-level select e.g.
//          // SELECT foobar FROM (SELECT foo.bar /*<- this*/ AS foobar ...)
//          (tokenizeColumn(strategy, prefix.mkString, prefixRenameable) + "." + tokenizeColumn(
//            strategy,
//            name,
//            renameable
//          )).token
//        case _ =>
//          // Typically this happens on the outer (i.e. top-level) clause of a multi-level select e.g.
//          // SELECT foobar /*<- this*/ FROM (SELECT foo.bar AS foobar ...)
//          tokenizeColumn(strategy, prefix.mkString + name, renameable).token
//      }
//  }
