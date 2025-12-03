package io.exoquery.plugin

import io.exoquery.plugin.trees.KnownSerializer
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull

/**
/**
 * Infers a serializer for a type specified on a data-class property.
 *
 * This covers two common situations:
 *
 * Case A - Serializer specified on the *type-use* (property) itself:
 * ```kotlin
 * // Custom serializer for UUID
 * object UUIDSerializer : KSerializer<UUID> {
 *     override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
 *     override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
 *     override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
 * }
 *
 * data class User(
 *     @Serializable(UUIDSerializer::class)
 *     val id: UUID,
 *     val name: String
 * )
 * ```
 * The serializer is attached to the property's type annotation. The backend IR exposes
 * that annotation on the type, and this function reads the first annotation argument
 * as an `IrClassReference` and returns `KnownSerializer.Ref(...)`.
 *
 * Case B - Serializer declared on the class being used:
 * ```kotlin
 * // Custom serializer for the entire class
 * object DateRangeSerializer : KSerializer<DateRange> {
 *     override val descriptor = buildClassSerialDescriptor("DateRange") {
 *         element<String>("start")
 *         element<String>("end")
 *     }
 *     override fun serialize(encoder: Encoder, value: DateRange) {
 *         encoder.encodeStructure(descriptor) {
 *             encodeStringElement(descriptor, 0, value.start.toString())
 *             encodeStringElement(descriptor, 1, value.end.toString())
 *         }
 *     }
 *     override fun deserialize(decoder: Decoder): DateRange =
 *         decoder.decodeStructure(descriptor) {
 *             DateRange(
 *                 LocalDate.parse(decodeStringElement(descriptor, 0)),
 *                 LocalDate.parse(decodeStringElement(descriptor, 1))
 *             )
 *         }
 * }
 *
 * @Serializable(DateRangeSerializer::class)
 * data class DateRange(
 *     val start: LocalDate,
 *     val end: LocalDate
 * )
 *
 * // Usage in another class
 * data class Event(
 *     val name: String,
 *     val duration: DateRange  // Uses DateRangeSerializer automatically
 * )
 * ```
 * If the property type's class has a `@Serializable` annotation, the code inspects the
 * annotation constructor arguments. Three scenarios are possible:
 * 1. If an explicit serializer class is provided, returns `KnownSerializer.Ref(...)`
 * 2. If the annotation is present but no explicit serializer argument is found
 *    (using default `with`), returns `KnownSerializer.Implicit`
 * 3. If neither case applies, returns `KnownSerializer.None`
  */
 */
fun IrType.inferSerializerForPropertyType() = run {
  val propertyType = this

  // If there is a serializer defined on the type-annotation try to get it
  val serializerFromType =
    propertyType.getAnnotationArgs<kotlinx.serialization.Serializable>().firstOrNull()
      ?.let { it as? IrClassReference }?.let { KnownSerializer.Ref(it) }

  // If there is a serializer defined on the class, try to get it
  val serializerFromTypeOrClass =
    serializerFromType
      ?: propertyType.classOrNull?.owner?.getAnnotation<kotlinx.serialization.Serializable>()?.let { annotationCtor ->
        // Note that the despite the fact that `kotlinx.serialization.Serializable` has a default 1st argument (i.e. the `with`)
        // in the backend-IR when the argument is not explicitly specified on the type that is being annotated, there will be
        // zero args that show up in the `valueArguments` field. I believe this is by design so that the compiler-writer can
        // tell the serialization constructor (or any constructor for that matter) is being used with default values.
        val serializerArg = annotationCtor.regularArgs.firstOrNull()
        val serializerArgRef = serializerArg?.let { it as? IrClassReference }?.let { KnownSerializer.Ref(it) }
        serializerArgRef ?: KnownSerializer.Implicit
      }

  serializerFromTypeOrClass ?: KnownSerializer.None
}
