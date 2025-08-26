@file:Suppress("DANGEROUS_CHARACTERS")

package io.exoquery.xr

operator fun XR.Ident.Companion.invoke(id: String) = XR.Ident(id, XRType.Generic, XR.Location.Synth)
operator fun XR.Ident.Companion.invoke(name: String, type: XRType) = XR.Ident(name, type, XR.Location.Synth)
operator fun XR.Entity.Companion.invoke(id: String) = XR.Entity(id, XRType.Product("Test", emptyList()))

fun XR.Product.Companion.TupleN(elements: List<XR.Expression>) =
  when (elements.size) {
    0 -> XR.Product("Empty")
    1 -> XR.Product("Single", "first" to elements[0])
    2 -> XR.Product("Pair", "first" to elements[0], "second" to elements[1])
    3 -> XR.Product("Triple", "first" to elements[0], "second" to elements[1], "third" to elements[2])
    4 -> XR.Product(
      "Tuple4",
      "first" to elements[0], "second" to elements[1], "third" to elements[2], "fourth" to elements[3]
    )

    5 -> XR.Product(
      "Tuple5",
      "first" to elements[0],
      "second" to elements[1],
      "third" to elements[2],
      "fourth" to elements[3],
      "fifth" to elements[4]
    )

    6 -> XR.Product(
      "Tuple6",
      "first" to elements[0],
      "second" to elements[1],
      "third" to elements[2],
      "fourth" to elements[3],
      "fifth" to elements[4],
      "sixth" to elements[5]
    )

    else -> throw IllegalArgumentException("Only up to 6 elements are supported for this operation")
  }

fun XRType.Companion.LeafTuple(size: Int) =
  when (size) {
    0 -> XRType.Product("Empty", listOf())
    1 -> XRType.Product("Single", listOf("first" to XRType.Value))
    2 -> XRType.Product("Pair", listOf("first" to XRType.Value, "second" to XRType.Value))
    3 -> XRType.Product("Triple", listOf("first" to XRType.Value, "second" to XRType.Value, "third" to XRType.Value))
    4 -> XRType.Product(
      "Tuple4",
      listOf("first" to XRType.Value, "second" to XRType.Value, "third" to XRType.Value, "fourth" to XRType.Value)
    )

    5 -> XRType.Product(
      "Tuple5",
      listOf(
        "first" to XRType.Value,
        "second" to XRType.Value,
        "third" to XRType.Value,
        "fourth" to XRType.Value,
        "fifth" to XRType.Value
      )
    )

    6 -> XRType.Product(
      "Tuple6",
      listOf(
        "first" to XRType.Value,
        "second" to XRType.Value,
        "third" to XRType.Value,
        "fourth" to XRType.Value,
        "fifth" to XRType.Value,
        "sixth" to XRType.Value
      )
    )

    else -> throw IllegalArgumentException("Only up to 6 elements are supported for this operation")
  }

fun XRType.Companion.LeafProduct(vararg elemNames: String) =
  XRType.Product("LeafProduct", elemNames.map { it to XRType.Value })
