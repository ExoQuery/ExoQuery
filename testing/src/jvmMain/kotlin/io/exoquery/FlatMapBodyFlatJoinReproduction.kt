//@file:PhasesDisabled(DisableablePhase.CrunchFlatJoins::class)
@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class, TraceType.Standard::class)
package io.exoquery

import io.exoquery.annotation.PhasesDisabled
import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.DisableablePhase
import io.exoquery.util.TraceType

/**
 * Standalone reproduction for the case: `this is XR.FlatMap && body is FlatJoin`
 *
 * This case occurs at SqlQueryModel.kt:328 in the flattenContexts function.
 *
 * Similar to LastResortFlatJoin which handles:
 *   Filter(FlatJoin(...))
 *
 * This case handles:
 *   FlatMap(head, id, FlatJoin(...))
 *
 * The pattern occurs when:
 * 1. We have a base query (head)
 * 2. We flatMap over it
 * 3. The body of the flatMap is directly a FlatJoin (WITHOUT a Map wrapper)
 *
 * From the comment at SqlQueryModel.kt:328:
 * "A flat-join query with no maps e.g: `qr1.flatMap(e1 => qr1.join(e2 => e1.i == e2.i))`"
 *
 * This is created by using Table.flatMap with composeFrom.join and NOT using .map() at the end.
 * The SqlQueryModel.kt:328 case then converts this to:
 *   FlatMap(head, id, Map(FlatJoin(body.head, body.id, cc), body.id, cc, loc))
 * where cc = XR.Product.fromProductIdent(body.id)
 */
fun main() {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)

  // Direct translation of the comment at SqlQueryModel.kt:328:
  // "A flat-join query with no maps e.g: `qr1.flatMap(e1 => qr1.join(e2 => e1.i == e2.i))`"
  //
  // This creates: FlatMap(Entity(Person), p, FlatJoin(Entity(Address), a, a.ownerId == p.id))
  // The flatMap body is directly a FlatJoin without any Map wrapper
  val a = capture {
    Table<Person>().flatMap { p ->
      // TODO Just introduced an optimization that tests this in CrunchFlatJoins, introduce specs to test with an without CrunchFlatJoins enabled
      // also:
      // TODO documentation should say NOT to add maps and filters after composeFrom (maybe should have a compileTime check for this)
      //      but instead to put them on the joined-table first. Should add an annotation called EnableExperimentalFeatures that
      //      allows it to be done

      // Note: composeFrom.join WITHOUT .map() at the end
      // This returns the FlatJoin directly as the body of the flatMap
      composeFrom.join(Table<Address>().filter { a -> a.city == "Someplace" }) { a -> a.ownerId == p.id }
    }
  }

  val b = sql.select {
    val x = from(a)
    where { x.street == "123 Place" }
    x
  }

  println("XR:\n${b.xr.showRaw()}\n")
  println("SQL:\n${b.buildPrettyFor.Postgres().value}\n")
}
