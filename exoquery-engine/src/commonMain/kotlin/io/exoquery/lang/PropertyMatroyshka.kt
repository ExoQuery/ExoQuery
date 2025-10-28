package io.exoquery.lang

import io.decomat.*
import io.exoquery.xr.XR

object PropertyMatryoshka {

  /**
   * For expr.foo.bar which is represented by the structure Property(Property(expr, foo), bar)
   * return the core `expr` and then the properties in order of the path i.e. foo,bar
   * so it will be (expr, List(Property(..., foo), Property(..., bar))). Originally this just returned
   * the strings foo, bar but then I realized that other parts of the property (than the name)
   * might want to be used e.g. the location.
   */
  fun traverse(initial: XR.Property, accum: List<XR.Property> = listOf()): Pair<XR.Expression, List<XR.Property>> =
    with(initial) {
      when {
        // Property(Property(...))
        of is XR.Property ->
          traverse(of, listOf(initial) + accum)
        else ->
          of to listOf(initial) + accum
      }
    }

  operator fun <AP : Pattern<XR.Expression>, BP : Pattern<List<XR.Property>>> get(a: AP, b: BP) =
    customPattern2("PropertyMatryoshka.traverse", a, b) { it: XR.Expression ->
      when (it) {
        is XR.Property -> traverse(it).let { (xr, path) -> Components2(xr, path) }
        else -> null
      }
    }
}

fun isPropertyOrCore(it: XR.Expression) =
  it is XR.Property || it is XR.Ident || it is XR.Free || it is XR.Const

fun PropertyOrCore() =
  Is<XR.Expression> {
    val check = it is XR.Property || it is XR.Ident || it is XR.Free || it is XR.Const
    check
  }

fun Core() = Is<XR.Expression> { it is XR.Ident || it is XR.Free || it is XR.Const }
