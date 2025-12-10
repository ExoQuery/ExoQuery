package io.exoquery.serial

import io.exoquery.ValueWithSerializer
import io.exoquery.ValuesWithSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.NothingSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

inline fun <reified T : Any> contextualSerializer(): ParamSerializer<T> =
  ParamSerializer.Custom(ContextualSerializer(T::class), T::class)

inline fun <reified T : Any> contextualSerializerNullable(): ParamSerializer<T?> =
  ParamSerializer.Custom(ContextualSerializer(T::class).nullable, T::class)

// SqlExpression(xr=Statement(tokens=[StringToken(string=TagP("0"))]), runtimes=RuntimeSet(runtimes=[]), params=ParamSet(lifts=[ParamMulti(id=BID(value=0), value=[2021-01-01, 2021-01-01], serial=CustomCompareable(Id(cls=class kotlinx.datetime.LocalDate,
// descriptor=ContextDescriptor(kClass: class kotlinx.datetime.LocalDate, original: kotlinx.serialization.ContextualSerializer()))))]))

// SqlExpression(xr=Statement(tokens=[StringToken(string=TagP("0"))]), runtimes=RuntimeSet(runtimes=[]), params=ParamSet(lifts=[ParamMulti(id=BID(value=0), value=[2021-01-01, 2021-01-01], serial=CustomCompareable(Id(cls=class kotlin.collections.List,
// descriptor=ContextDescriptor(kClass: class kotlinx.datetime.LocalDate, original: kotlinx.serialization.ContextualSerializer()))))]))


inline fun <reified T> customSerializer(serializer: KSerializer<T>): ParamSerializer<T> =
  ParamSerializer.Custom(serializer, T::class)

// Called externally by the expr model when creating a ParamSingle from a param(ValueWithSerializer) call
inline fun <reified T : Any> customValueSerializer(customValue: ValueWithSerializer<T>): ParamSerializer<T> =
  customValue.asParam()

// Called externally by the expr model when creating a ParamMulti from a param(ValuesWithSerializer) call
inline fun <reified T : Any> customValueListSerializer(customValue: ValuesWithSerializer<T>): ParamSerializer<T> =
  customValue.asParam()

interface ParamSerializer<T> {
  val serializer: SerializationStrategy<T>
  val cls: KClass<*>

  fun withNonStrictEquality() = this

  class Custom<T>(override val serializer: KSerializer<T>, override val cls: KClass<*>) : ParamSerializer<T> {
    override fun withNonStrictEquality(): ParamSerializer<T> = CustomCompareable(serializer, cls)
  }

  /** For testing purposes, we need to be able to compare two instances of Custom. */
  class CustomCompareable<T>(override val serializer: KSerializer<T>, override val cls: KClass<*>) : ParamSerializer<T> {
    private val id by lazy { Id(cls, serializer.descriptor) }

    companion object {
      // We can't compare two instances of Custom directly because the ParamSerializer instances don't compare. The best that we can do is to compare the class and kind of the serializer.
      private data class Id(val cls: KClass<*>, val descriptor: SerialDescriptor)
    }

    override fun equals(other: Any?): kotlin.Boolean = other is CustomCompareable<*> && other.id == id && other.cls == cls
    override fun hashCode(): kotlin.Int = id.hashCode()
    override fun toString(): kotlin.String = "CustomCompareable(${id.toString()})"
  }

  data object LocalDate : ParamSerializer<kotlinx.datetime.LocalDate> {
    override val serializer = ContextualSerializer(kotlinx.datetime.LocalDate::class)
    override val cls = kotlinx.datetime.LocalDate::class
  }
  data object LocalTime : ParamSerializer<kotlinx.datetime.LocalTime> {
    override val serializer = ContextualSerializer(kotlinx.datetime.LocalTime::class)
    override val cls = kotlinx.datetime.LocalTime::class
  }
  data object LocalDateTime : ParamSerializer<kotlinx.datetime.LocalDateTime> {
    override val serializer = ContextualSerializer(kotlinx.datetime.LocalDateTime::class)
    override val cls = kotlinx.datetime.LocalDateTime::class
  }
  data object Instant : ParamSerializer<kotlinx.datetime.Instant> {
    override val serializer = ContextualSerializer(kotlinx.datetime.Instant::class)
    override val cls = kotlinx.datetime.Instant::class
  }
  data object ByteArray : ParamSerializer<kotlin.ByteArray> {
    override val serializer = ContextualSerializer(kotlin.ByteArray::class)
    override val cls = kotlin.ByteArray::class
  }
  data object String : ParamSerializer<kotlin.String> {
    override val serializer = serializer<kotlin.String>()
    override val cls = kotlin.String::class
  }
  data object Char : ParamSerializer<kotlin.Char> {
    override val serializer = serializer<kotlin.Char>()
    override val cls = kotlin.Char::class
  }
  data object Int : ParamSerializer<kotlin.Int> {
    override val serializer = serializer<kotlin.Int>()
    override val cls = kotlin.Int::class
  }
  data object Long : ParamSerializer<kotlin.Long> {
    override val serializer = serializer<kotlin.Long>()
    override val cls = kotlin.Long::class
  }
  data object Short : ParamSerializer<kotlin.Short> {
    override val serializer = serializer<kotlin.Short>()
    override val cls = kotlin.Short::class
  }
  data object Byte : ParamSerializer<kotlin.Byte> {
    override val serializer = serializer<kotlin.Byte>()
    override val cls = kotlin.Byte::class
  }
  data object Float : ParamSerializer<kotlin.Float> {
    override val serializer = serializer<kotlin.Float>()
    override val cls = kotlin.Float::class
  }
  data object Double : ParamSerializer<kotlin.Double> {
    override val serializer = serializer<kotlin.Double>()
    override val cls = kotlin.Double::class
  }
  data object Boolean : ParamSerializer<kotlin.Boolean> {
    override val serializer = serializer<kotlin.Boolean>()
    override val cls = kotlin.Boolean::class
  }
  data object NullType : ParamSerializer<kotlin.Nothing> {
    override val serializer = NothingSerializer()
    override val cls = kotlin.Boolean::class
  }
}
