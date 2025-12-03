package io.exoquery.plugin.trees

import io.exoquery.ParseError
import io.exoquery.annotation.ExoField
import io.exoquery.parseError
import io.exoquery.plugin.dataClassProperties
import io.exoquery.plugin.firstConstStringOrNull
import io.exoquery.plugin.getAnnotationArgs
import io.exoquery.plugin.inferSerializerForPropertyType
import io.exoquery.plugin.isDataClass
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.callWithParams
import io.exoquery.xr.XRType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

sealed interface KnownSerializer {
  data class Ref(val serializer: IrClassReference): KnownSerializer {
    context(CX.Scope, CX.Builder)
    fun buildExpression(expectedType: IrType, originalElementOrigin: ParseError.Origin) = run {
      // Don't know if it's always safe to make the assumption that an IrClassReference.symbol is an IrClassSymbol so return a specific error
      val symbol: IrClassSymbol = serializer.symbol as? IrClassSymbol ?: parseError("Error getting the class symbol of the class reference ${serializer.dumpKotlinLike()}. The reference was not an IrClassSymbol", originalElementOrigin)
      builder.irGetObject(symbol)
    }
  }
  data object Implicit: KnownSerializer {
    context(CX.Scope, CX.Builder)
    fun buildExpression(expectedType: IrType) = run {
      // When there is a @Serializeable annotation on the class itself then just invoke `kotlinx.serialization.serializer<OfThatType>`
      callWithParams("kotlinx.serialization", "serializer", listOf(expectedType))()
    }
  }
  data object None: KnownSerializer
}

object Elaborate {
  data class Path(val path: List<Seg>, val invocation: IrExpression, val type: IrType, val xrType: XRType, val knownSerializer: KnownSerializer) {
    data class Seg(val name: String, val isRenamed: Boolean) {
      fun tuplize() = name to isRenamed
    }

    override fun toString(): String = path.joinToString(".")
  }

  context(CX.Scope, CX.Builder)
  fun invoke(expr: IrExpression, rootType: XRType.Product) = run {
    if (!expr.type.isDataClass())
      parseError("Expected a data class to elaborate, got ${expr.type}", expr)

    invokeRecurse(emptyList(), expr, expr.type, rootType, KnownSerializer.None) // Don't care about the top-level object serializer since elaboration is field-by-field
  }

  // data class Person(name, age), and an identifier p:Person ->
  //  ([name], p.name), ([age], p.age)
  // data class Person(name: Name(first, last), age), and an identifier p:Person ->
  //  ([name, first], p.name.first), ([name, last], p.name.last), ([age], p.age)
  // However, if name is nullable e.g:
  // data class Person(name: Name(first, last)?, age), and an identifier p:Person ->
  //  ([name, first], if (p.name == null) null else p.first), ([name, last], if (p.name == null) null else p.last), ([age], p.age)
  // Same pattern for futher nested nullables
  context(CX.Scope, CX.Builder)
  private fun invokeRecurse(currPath: List<Path.Seg>, parent: IrExpression, type: IrType, currentXrType: XRType, knownFieldSerializer: KnownSerializer): List<Path> = run {
    if (currentXrType.isProduct()) {
      val cls = type.classOrNull ?: parseError("Expected a class to elaborate, got ${type} which is invalid", parent)
      val clsOwner = cls.owner
      // explicitly mention it since we've already checked above
      currentXrType as XRType.Product


      //error("----------- Safe fields: ${cls.dataClassProperties().map { (name, tpe) ->
      //  ("--------------- ${name} --------------\n${cls.getPropertyGetter(name)}\n-----------------")
      //}} -----------")

      val props = clsOwner.declarations.filterIsInstance<IrProperty>()

      cls.dataClassProperties().flatMap { (propertyName, propertyType) ->
        val property = props.firstOrNull { it.name.asString() == propertyName } ?: error("Property $propertyName not found")

        // To get the @ExoField or @SerialName annotation we need to get the getter in case this was overridden
        val (sqlPropertyName, isRenamed) =
          property.getAnnotationArgs<ExoField>().firstConstStringOrNull()?.let { it to true }
            ?: property.getAnnotationArgs<kotlinx.serialization.SerialName>().firstConstStringOrNull()?.let { it to true }
            ?: propertyName.let { it to false }

        val knownFieldSerializer: KnownSerializer = run {
          val propertyOnTheField =
            // Was there a @Serializeable annotation on the field?
            (property.getAnnotationArgs<kotlinx.serialization.Serializable>().firstOrNull()
              // Was there a @Serializeable annotation on the type (e.g. if there is a type-alias with a @Serializable annotation being used)
              ?: propertyType.getAnnotationArgs<kotlinx.serialization.Serializable>().firstOrNull()
            )
              ?.let { it as? IrClassReference }?.let { KnownSerializer.Ref(it) }

          val propertyOnFieldOrType =
            propertyOnTheField ?: propertyType.inferSerializerForPropertyType()

          propertyOnFieldOrType ?: KnownSerializer.None
        }

        // Need to call the getting-symbol in order to get the field value, also make sure the backing field is bound
        val getterSymbol = property?.getter?.symbol ?: error("Getter for $propertyName not found")
        val backingFieldSymbol = property.backingField?.symbol
        if (backingFieldSymbol != null && !backingFieldSymbol.isBound) {
          error("Backing field for $propertyName is unbound!")
        }

        val nextXrType = currentXrType.fields.find { it.first == sqlPropertyName }?.second
          ?: parseError("Field $propertyName not found in ${currentXrType.name}", parent)

        val explicitReceiver = parent
        if (getterSymbol != null) {
          val call =
            with(builder) {
              irCall(getterSymbol).apply {
                dispatchReceiver = explicitReceiver
              }
            }
          invokeRecurse(currPath + Path.Seg(sqlPropertyName, isRenamed), callNullSafe(parent, type, propertyType, call), propertyType, nextXrType, knownFieldSerializer)
        } else {
          emptyList()
        }
      }
    } else {
      listOf(Path(currPath, parent, type, currentXrType, knownFieldSerializer))
    }
  }

  context(CX.Scope, CX.Builder)
  private fun callNullSafe(parent: IrExpression, parentType: IrType, targetType: IrType, call: IrExpression) = run {
    //val call = parent.callDispatch(propertyName).invoke()
    if (parentType.isNullable()) {
      with(builder) {
        irIfNull(targetType, parent, irNull(targetType), call)
      }
    } else {
      call
    }
  }
}
