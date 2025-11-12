package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.serial.ParamSerializer
import kotlin.reflect.KClass


/*
 * val expr: @Captured SqlExpression<Int> = sql { foo + bar }
 * val query: SqlQuery<Person> = sql { Table<Person>() }
 * val query2: SqlQuery<Int> = sql { query.map { p -> p.age } }
 *
 * so:
 * // Capturing a generic expression returns a SqlExpression
 * fun <T> sql(block: () -> T): SqlExpression<T>
 * for example:
 * {{{
 * val combo: SqlExpression<Int> = sql { foo + bar }
 * }}}
 *
 * // Capturing a SqlQuery returns a SqlQuery
 * fun <T> sql(block: () -> SqlQuery<T>): SqlQuery<T>
 * for example:
 * {{{
 * val query: SqlQuery<Person> = sql { Table<Person>() }
 * }}}
 */
@ExoInternal
sealed interface Param<T : Any> {
  val id: BID
  val serial: ParamSerializer<T>
  fun withNonStrictEquality(): Param<T>
  fun showValue(): String
  fun withNewBid(newBid: BID): Param<T>
  val description: String?
}


@ExoInternal
data class ParamMulti<T : Any>(override val id: BID, val value: List<T?>, override val serial: ParamSerializer<T>, override val description: String? = null) : Param<T> {
  override fun withNonStrictEquality(): ParamMulti<T> =
    copy(serial = serial.withNonStrictEquality())

  override fun showValue() = value.toString()
  override fun withNewBid(newBid: BID): ParamMulti<T> = copy(id = newBid)
  override fun toString() = "ParamMulti(${id.value}, $value, $serial)" // don't want to show description here because it could add too much noise
}

@ExoInternal
data class ParamSingle<T : Any>(override val id: BID, val value: T?, override val serial: ParamSerializer<T>, override val description: String? = null) : Param<T> {
  override fun withNonStrictEquality(): ParamSingle<T> =
    copy(serial = serial.withNonStrictEquality())

  override fun showValue() = value.toString()
  override fun withNewBid(newBid: BID): ParamSingle<T> = copy(id = newBid)
  override fun toString() = "ParamSingle(${id.value}, $value, $serial)" // don't want to show description here because it could add too much noise
}

/**
 * For sql.batch { param -> insert { set(name = param(p.name <- encoding the value here!)) }  }
 */
@ExoInternal
data class ParamBatchRefiner<Input, Output : Any>(override val id: BID, val refiner: (Input?) -> Output?, override val serial: ParamSerializer<Output>, override val description: String? = null) : Param<Output> {
  fun refine(input: Input): ParamSingle<Output> = ParamSingle<Output>(id, refiner(input), serial, description)
  fun refineAny(input: Any?): ParamSingle<Output> = refine(input as Input)

  override fun withNonStrictEquality(): ParamBatchRefiner<Input, Output> = this.copy(serial = serial.withNonStrictEquality())
  override fun showValue(): String = "Refiner_${serial.serializer.descriptor.kind}"
  override fun withNewBid(newBid: BID): ParamBatchRefiner<Input, Output> = copy(id = newBid)
  override fun toString() = "ParamBatchRefiner(${id.value}, ${showValue()}, $serial)"
}

@ExoInternal
data class ParamSet(val lifts: List<Param<*>>) {
  operator fun plus(other: ParamSet): ParamSet = ParamSet(lifts + other.lifts)
  fun withNonStrictEquality(): ParamSet = ParamSet(lifts.map { it.withNonStrictEquality() })

  companion object {
    fun of(vararg lifts: Param<*>) = ParamSet(lifts.toList())

    // Added this here to be consistent with Runtimes.Empty but unlike Runtimes.Empty it has no
    // special usage (i.e. the parser does not look for this value directly in the IR)
    val Empty = ParamSet(emptyList())
  }
}
