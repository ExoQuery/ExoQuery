package io.exoquery.xr

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// Formerly Quat
@Serializable
sealed interface XRType {
  fun isLeaf() =
    when (this) {
      is Product -> false
      else -> true
    }

  fun isBooleanValue() =
    when (this) {
      is BooleanValue -> true
      else -> false
    }

  fun isBooleanExpression() =
    when (this) {
      is BooleanExpression -> true
      else -> false
    }

  fun isProduct() =
    when (this) {
      is Product -> true
      else -> false
    }

  fun isAbstract() =
    when (this) {
      is Generic -> true
      is Unknown -> true
      else -> false
    }

  fun nonAbstract() = !isAbstract()

  @Serializable
  data class Product(val name: String, val fields: List<Pair<String, XRType>>, val meta: Meta = Meta.Empty) : XRType {
    @Serializable
    data class Meta(val hasRename: kotlin.Boolean, val fieldsWithRename: Set<String>) {
      fun fieldHasRename(field: String) =
        XR.HasRename.hasOrNot(fieldsWithRename.contains(field))

      // Incorporate another Meta (to be used with XRType.Product.leastUpperType. That means
      // the other meta could have fewer fields but that is fine because this is just a check-list
      // of fields that need to be renamed so extra fields here are fine.
      fun orThat(other: Meta) =
        Meta(this.hasRename || other.hasRename, this.fieldsWithRename + other.fieldsWithRename)

      companion object {
        val Empty = Meta(false, emptySet())
      }
    }

    private data class Id(val name: String, val fields: List<Pair<String, XRType>>)

    @Transient
    private val id = Id(name, fields)

    private val fieldsHash by lazy { fields.toMap() }

    sealed interface Resolution {
      data class Found(val type: XRType) : Resolution
      data class NotFound(val error: String) : Resolution
    }
    fun resolveFieldPath(path: List<String>): Resolution = run {
      if (path.isEmpty())
        Resolution.Found(this)
      else {
        val field = fields.find { it.first == path.first() } ?: return Resolution.NotFound("Field ${path.first()} not found in $name")
        // if there is only one path and we found the field for the type we need just return it
        if (path.size == 1)
          Resolution.Found(field.second)
        // if there is more than one path we need to further into the type
        else
          (field.second as? XRType.Product)?.resolveFieldPath(path.drop(1)) ?: return Resolution.NotFound("Field ${path.first()} in $name was not a product!")
      }
    }

    /**
     * The underlying impelmentation in kotlin o List<Pair<String, XRType>>.toMap() is a LinkedHashMap
     * if that changes in the future than we will need to change the result of this to explicitly be a LinkedHashMap.
     */
    fun fieldsHash() = fieldsHash
    fun getField(name: String) =
      if (fields.size < 5)
        fields.find { it.first == name }?.second
      else
        fieldsHash.get(name)

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): kotlin.Boolean =
      other is Product && this.id == other.id

    companion object {
      // Constructors mainly for testing purposes
      fun of(name: String, vararg fields: Pair<String, XRType>) = Product(name, fields.toList(), Meta.Empty)
      fun leaf(name: String, vararg fieldNames: String) = Product(name, fieldNames.map { it to Value }, Meta.Empty)
    }
  }

  @Serializable
  sealed class Boolean : XRType
  @Serializable
  object BooleanValue : Boolean()
  @Serializable
  object BooleanExpression : Boolean()

  @Serializable
  object Unknown : XRType

  @Serializable
  object Null : XRType

  @Serializable
  object Generic : XRType

  @Serializable
  object Value : XRType {
    override fun toString(): String {
      return "Value"
    }
  }

  fun shortString(): String = when (this) {
    is XRType.Product -> "${name}(${fields.map { (k, v) -> "${k}:${v.shortString()}" }.joinToString(",")})"
    is XRType.Generic -> "<G>"
    is XRType.Unknown -> "<U>"
    is XRType.Value -> "V"
    is XRType.Null -> "N"
    is XRType.BooleanValue -> "BV"
    is XRType.BooleanExpression -> "BE"
  }

  companion object {}
}

fun XRType.leastUpperType(other: XRType): XRType? =
  when {
    this is XRType.Generic -> other
    this is XRType.Unknown -> other
    this is XRType.Null -> other
    other is XRType.Generic -> other
    other is XRType.Unknown -> other
    other is XRType.Null -> other
    this is XRType.Value && other is XRType.Value -> XRType.Value
    this is XRType.BooleanExpression && other is XRType.BooleanExpression -> XRType.BooleanExpression
    this is XRType.BooleanValue && other is XRType.BooleanValue -> XRType.BooleanValue
    this is XRType.BooleanValue && other is XRType.BooleanExpression -> XRType.BooleanValue
    this is XRType.BooleanExpression && other is XRType.BooleanValue -> XRType.BooleanValue
    this is XRType.Value && other is XRType.BooleanValue -> XRType.Value
    this is XRType.Value && other is XRType.BooleanExpression -> XRType.Value
    this is XRType.BooleanValue && other is XRType.Value -> XRType.Value
    this is XRType.BooleanExpression && other is XRType.Value -> XRType.Value
    this is XRType.Product && other is XRType.Product -> this.leastUpperTypeProduct(other)
    else -> null
  }

fun XRType.Product.leastUpperTypeProduct(other: XRType.Product): XRType.Product {
  val fields = this.fields.zipNotNullWith(other.fieldsHash())
  val newFields = fields.map { (name, type, otherType) ->
    val newType = type.leastUpperType(otherType) ?: run {
      // TODO Log warning about the invalid reduction
      XRType.Unknown
    }
    name to newType
  }
  val meta = this.meta.orThat(other.meta)
  return XRType.Product(name, newFields, meta)
}

fun <T> List<Pair<String, T>>.zipNotNullWith(other: Map<String, T>) =
  this.mapNotNull { (name, type) ->
    other[name]?.let { otherName ->
      Triple<String, T, T>(name, type, otherName)
    }
  }
