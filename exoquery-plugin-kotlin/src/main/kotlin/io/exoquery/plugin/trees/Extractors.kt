@file:Suppress("NAME_SHADOWING", "NAME_SHADOWING")

package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.ValueWithSerializer
import io.exoquery.annotation.ExoEntity
import io.exoquery.annotation.ExoField
import io.exoquery.annotation.ExoValue
import io.exoquery.parseError
import io.exoquery.plugin.*
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.ReceiverCaller
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isValueClassType
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

fun <T> List0() = Is(listOf<T>())

object List1 {
  operator fun <AP : Pattern<A>, A> get(elem1: AP) =
    customPattern1("List1", elem1) { it: List<A> -> if (it.size == 1) Components1(it.first()) else null }
}

object List2 {
  operator fun <AP : Pattern<T>, BP : Pattern<T>, T> get(elem1: AP, elem2: BP) =
    customPattern2("List2", elem1, elem2) { it: List<T> ->
      if (it.size == 2) {
        val first: T = it.get(0)
        val second: T = it.get(1)
        Components2(first, second)
      } else
        null
    }
}

val IrType.simpleTypeArgs: List<IrType>
  get() =
    when (this) {
      is IrSimpleType ->
        this.arguments.mapNotNull { it.typeOrNull }
      else ->
        listOf()
    }

object Ir {
  object Expression {
    context(CX.Scope) operator fun <AP : Pattern<IrExpression>> get(components: AP) =
      customPattern1("Ir.Expression", components) { it: IrExpression ->
        Components1(it)
      }
  }

  object CastingTypeOperator {
    context(CX.Scope) operator fun <AP : Pattern<IrExpression>, BP : Pattern<IrType>> get(op: AP, type: BP) =
      customPattern2("Ir.TypeOperatorCall", op, type) { it: IrTypeOperatorCall ->
        if (it.operator == IrTypeOperator.CAST || it.operator == IrTypeOperator.IMPLICIT_CAST) {
          Components2(it.argument, it.typeOperand)
        } else {
          null
        }
      }
  }

  object DenullingTypeOperator {
    context(CX.Scope) operator fun <AP : Pattern<IrExpression>> get(op: AP) =
      customPattern1("Ir.TypeOperatorCall", op) { it: IrCall ->
        if (it.symbol.safeName == BuiltInOperatorNames.CHECK_NOT_NULL) {
          Components1(it.arguments.first())
        } else {
          null
        }
      }
  }

  object StringConcatenation {
    // Can't do just get(components: Pattern<List<IrExpression>) need to do:
    // <AP: Pattern<List<IrExpression>>> get(components: AP) or it doesn't work because
    // it needs to have a concrete pattern instance
    context(CX.Scope) operator fun <AP : Pattern<List<IrExpression>>> get(components: AP) =
      customPattern1("StringConcatenation", components) { it: IrExpression ->
        if (it is IrStringConcatenation) {
          Components1(it.arguments)
        } else {
          null
        }
      }
  }

  object Expr {
    class ClassOf<R>(val classNameRaw: ClassId?, val pluginCtx: IrPluginContext) : Pattern0<IrExpression>(Typed<IrExpression>()) {
      override fun matches(r: ProductClass<IrExpression>): Boolean =
        classNameRaw?.let { className ->
          Typed<IrExpression>().typecheck(r.productClassValueUntyped) &&
              r.productClassValue.type.let { tpe ->
                className == tpe.classId() || tpe.superTypes().any { it.classId() == className }
              }
        } ?: false

      // private val checkClass =
      //   classNameRaw?.let {
      //     pluginCtx.referenceClass(classNameRaw)
      //   }
      // Better implementation but need to be tested more
      //override fun matches(r: ProductClass<IrExpression>): Boolean {
      //  if (!Typed<IrExpression>().typecheck(r.productClassValueUntyped)) return false
      //  if (checkClass == null) return false
      //  val expressionType = r.productClassValue.type
      //  val expressionClass = expressionType.getClass() ?: return false
      //  return expressionClass.isSubclassOf(checkClass.owner)
      //}

      companion object {
        context(CX.Scope)
        inline operator fun <reified T> invoke() =
          ClassOf<T>(T::class.classId(), pluginCtx)
      }
    }

    class IsTypeOf private constructor (val type: IrType, val typeSystem: IrTypeSystemContext) : Pattern0<IrExpression>(Typed<IrExpression>()) {
      override fun matches(r: ProductClass<IrExpression>): Boolean =
        Typed<IrExpression>().typecheck(r.productClassValueUntyped) &&
            r.productClassValue.type.isSubtypeOf(type, typeSystem)

      companion object {
        context(CX.Scope)
        operator fun invoke(type: IrType) = IsTypeOf(type, typeSystem)
      }
    }

    class HasAnnotation(val annotationNameRaw: FqName?) : Pattern0<IrExpression>(Typed<IrExpression>()) {
      override fun matches(r: ProductClass<IrExpression>): Boolean =
        annotationNameRaw?.let { annotationName ->
          Typed<IrExpression>().typecheck(r.productClassValueUntyped) &&
              r.productClassValue.hasAnnotation(annotationName)
        } ?: false

      companion object {
        inline operator fun <reified T> invoke() =
          HasAnnotation(classIdOf<T>()?.packageFqName)
      }
    }
  }

  object Type {

//    object Nullable {
//      context(CX.Scope) operator fun <AP: Pattern<IrType>> get(realType: AP) =
//        customPattern1("Type.Nullable", realType) { it: IrType ->
//
//          if (cls != null && simpleTypeArgs.size == 1 && it.isClass<List<*>>()) {
//            Components1(simpleTypeArgs.first())
//          }
//          else null
//        }
//    }

    object NullableOf {
      context(CX.Scope) operator fun <AP : Pattern<IrType>> get(realType: AP) =
        customPattern1("Type.NullableOf", realType) { it: IrType ->
          if (it.isNullable()) {
            Components1(it.makeNotNull())
          } else null
        }
    }

    object KotlinList {
      context(CX.Scope) operator fun <AP : Pattern<IrType>> get(realType: AP) =
        customPattern1("Type.KotlinList", realType) { it: IrType ->
          val cls = it.classOrNull
          val simpleTypeArgs = it.simpleTypeArgs
          if (cls != null && simpleTypeArgs.size == 1 && it.isClass<List<*>>()) {
            Components1(simpleTypeArgs.first())
          } else null
        }
    }

    object DataClass {
      data class Name(val name: String, val isRenamed: Boolean)
      data class Prop(val name: String, val type: IrType, val isMarkedValue: Boolean, val isRenamed: Boolean, val originalName: String)

      context(CX.Scope) operator fun <AP : Pattern<Name>, BP : Pattern<List<DataClass.Prop>>> get(name: AP, fields: BP) =
        customPattern2("Type.DataClass", name, fields) { it: IrType ->
          val cls = it.classOrNull
          if (cls != null && cls.isDataClass()) {
            val (name, isRenamed) =
              cls.owner.getAnnotationArgs<ExoEntity>().firstConstStringOrNull()?.let { it to true } // Try to get entity name from ExoEntity
                ?: cls.owner.getAnnotationArgs<SerialName>().firstConstStringOrNull()?.let { it to true } // Then try SerialName from the class
                ?: it.classFqName?.sanitizedClassName()?.let { it to false } // Then try the class fully-qualified name
                ?: cls.safeName.let { it to false } // If all else fails, use the class symbol name

            val props = cls.owner.declarations.filterIsInstance<IrProperty>()
            val propNames =
              cls.dataClassProperties().map { (propName, propType) ->
                val irProp =
                  props.firstOrNull { it.name.asString() == propName } ?: error("Property $propName not found")

                val (realPropName, hasFieldAnnotation) =
                  irProp.getAnnotationArgs<ExoField>().firstConstStringOrNull()?.let { it to true }
                    ?: irProp.getAnnotationArgs<kotlinx.serialization.SerialName>().firstConstStringOrNull()?.let { it to true }
                    ?: (propName to false)

                val isValue = hasFieldAnnotation || irProp.hasAnnotation<ExoValue>() || cls.owner.hasAnnotation<Contextual>() || cls.owner.hasAnnotation<ExoValue>()
                val isRenamed = hasFieldAnnotation
                DataClass.Prop(realPropName, propType, isValue, isRenamed, propName)
              }

            // Note that this was not matching without props.toList() because it was a Sequence object instead of a list
            // this is improtant to note since if the types to not line up the match won't happen although the IDE
            // or build will not complain about the mismatched types (at least during compilation of this file)
            val output = Components2(Name(name, isRenamed), propNames.toList())
            output
          } else null
        }
    }

    class ClassOfType<R>(val classNameRaw: ClassId?) : Pattern0<IrType>(Typed<IrType>()) {
      override fun matches(r: ProductClass<IrType>): Boolean =
        classNameRaw?.let { className ->
          Typed<IrType>().typecheck(r.productClassValueUntyped) &&
              r.productClassValue.let { tpe ->
                className == tpe.classId() || tpe.superTypes().any { it.classId() == className }
              }
        } ?: false

      companion object {
        inline operator fun <reified T> invoke() =
          ClassOfType<T>(T::class.classId())
      }
    }

    object Generic {
      context(CX.Scope) operator fun get(type: Pattern0<IrType>) =
        customPattern1("Ir.Call.Generic", type) { it: IrType ->
          if (it.isTypeParameter())
            Components1(it)
          else
            null
        }
    }

    object Value {
      context(CX.Scope)
      private fun isValueType(it: IrType) =
          it.isValueClassType() ||
            it.isString() ||
            it.isChar() ||
            it.isLong() ||
            it.isShort() ||
            it.isInt() ||
            it.isByte() ||
            it.isBoolean() ||
            it.isFloat() ||
            it.isDouble() ||
            it.isLongArray() ||
            it.isShortArray() ||
            it.isIntArray() ||
            it.isByteArray() ||
            it.isBooleanArray() ||
            it.isFloatArray() ||
            it.isDoubleArray() ||
            it.isNullableNothing() ||
            it.isClassStrict<kotlinx.datetime.LocalDate>() ||
            it.isClassStrict<kotlinx.datetime.LocalTime>() ||
            it.isClassStrict<kotlinx.datetime.LocalDateTime>() ||
            it.isClassStrict<kotlinx.datetime.Instant>() ||
            it.isClassStrict<java.time.LocalDate>() ||
            it.isClassStrict<java.time.LocalTime>() ||
            it.isClassStrict<java.time.LocalDateTime>() ||
            it.isClassStrict<java.time.ZonedDateTime>() ||
            it.isClassStrict<java.time.Instant>() ||
            it.isClassStrict<java.time.OffsetTime>() ||
            it.isClassStrict<java.time.OffsetDateTime>() ||
            it.isClassStrict<java.sql.Date>() ||
            it.isClassStrict<java.sql.Time>() ||
            it.isClassStrict<java.sql.Timestamp>() ||
            it.isClassStrict<java.math.BigDecimal>() ||
            it.isClassStrict<java.math.BigInteger>() ||
            it.isClass<ValueWithSerializer<*>>() ||
            it.hasAnnotation<Contextual>() ||
            (it.classOrNull?.owner?.hasAnnotation<Contextual>() ?: false) ||
            it.hasAnnotation<ExoValue>() ||
            (it.classOrNull?.owner?.hasAnnotation<ExoValue>() ?: false)

      context(CX.Scope) operator fun get(type: Pattern0<IrType>) =
        customPattern1("Ir.Call.Value", type) { it: IrType ->
          if (isValueType(it) || it.hasAnnotation<kotlinx.serialization.Contextual>() || it.hasAnnotation<io.exoquery.annotation.ExoValue>()) {
            // If mistake is made in the Components-returning section, the match will not be successful
            // but the compiler will not tell us we are returing something incorrect
            Components1(it)
          } else null
        }
    }

  }

  object Block {
    operator fun <AP : Pattern<List<IrStatement>>, BP : Pattern<IrExpression>> get(statements: AP, ret: BP) =
      customPattern2("Ir.Block", statements, ret) { it: IrBlock ->
        val stmt = it.statements.takeLast(1).firstOrNull()
        val lastStatement =
          when {
            (stmt != null && stmt is IrExpression) -> stmt
            else -> error("Unexepected last-statement block was not an expression: ${it.dumpKotlinLike()}")
          }

        val others = it.statements.dropLast(1)
        Components2(others, lastStatement)
      }
  }

  object Variable {
    context (CX.Scope) operator fun <AP : Pattern<String>, BP : Pattern<B>, B : IrExpression> get(name: AP, rhs: BP) =
      customPattern2("Ir.Variable", name, rhs) { it: IrVariable ->
        it.initializer?.let { init -> Components2(it.name.sanitizedSymbolName(), init) }
      }
  }

  object Field {
    context (CX.Scope) operator fun <AP : Pattern<String>, BP : Pattern<IrExpression>> get(name: AP, rhs: BP) =
      customPattern2("Ir.Field", name, rhs) { it: IrField ->
        it.initializer?.let { initExprBody -> Components2(it.name.sanitizedSymbolName(), initExprBody.expression) }
      }
  }

  class GetObjectValue<R>(val classNameRaw: ClassId?) : Pattern0<IrGetObjectValue>(Typed<IrGetObjectValue>()) {
    override fun matches(r: ProductClass<IrGetObjectValue>): Boolean =
      classNameRaw?.let { className ->
        Typed<IrGetObjectValue>().typecheck(r.productClassValueUntyped) &&
            r.productClassValue.type.let { tpe ->
              className == tpe.classId() || tpe.superTypes().any { it.classId() == className }
            }
      } ?: false

    companion object {
      inline operator fun <reified T> invoke() =
        GetObjectValue<T>(T::class.classId())
    }
  }

  object GetValue {
    context (CX.Scope) operator fun get(value: Pattern0<IrGetValue>) =
      customPattern1("Ir.GetValue", value) { it: IrGetValue ->
        Components1(it)
      }

    object Symbol {
      context (CX.Scope) operator fun get(value: Pattern0<IrSymbol>) =
        customPattern1("Ir.GetValue.Symbol", value) { it: IrGetValue ->
          Components1(it.symbol)
        }
    }
  }

  object GetField {
    context (CX.Scope) operator fun get(value: Pattern0<IrGetField>) =
      customPattern1("Ir.GetField", value) { it: IrGetField ->
        Components1(it)
      }

    object Symbol {
      context (CX.Scope) operator fun get(value: Pattern0<IrSymbol>) =
        customPattern1("Ir.GetField.Symbol", value) { it: IrGetField ->
          Components1(it.symbol)
        }
    }
  }

  object GetEnumValue {
    operator fun get(value: Pattern0<IrSymbol>) =
      customPattern1("Ir.GetEnumValue", value) { it: IrGetEnumValue ->
        Components1(it.symbol)
      }
  }


  object ConstString {
    operator fun get(value: Pattern0<String>) =
      customPattern1("Ir.Const.String", value) { it: IrConst ->
        if (it.kind == IrConstKind.String)
          Components1((Const.Value.fromIrConst(it) as Ir.Const.Value.String).value)
        else
          null
      }
  }

  object Const {
    /**
     * Decomat breaks internally if there is a null-value in any inner part of the pattern,
     * therefore make sure to capute this into a Value.Null instead.
     * (note that this approach assumes we don't have a situation where we need to distinguish
     * between a null value whose type is Null and a null value that has some other type e.g. Int?)
     */
    sealed interface Value {
      data class Boolean(val value: kotlin.Boolean) : Value
      data class Char(val value: kotlin.Char) : Value
      data class Byte(val value: kotlin.Byte) : Value
      data class Short(val value: kotlin.Short) : Value
      data class Int(val value: kotlin.Int) : Value
      data class Long(val value: kotlin.Long) : Value
      data class String(val value: kotlin.String) : Value
      data class Float(val value: kotlin.Float) : Value
      data class Double(val value: kotlin.Double) : Value
      object Null : Value {
        override fun toString(): kotlin.String = "Null"
      }

      companion object {
        fun fromIrConst(it: IrConst): Value =
          when (it.kind) {
            IrConstKind.Boolean -> Boolean(it.value as kotlin.Boolean)
            IrConstKind.Char -> Char(it.value as kotlin.Char)
            IrConstKind.Byte -> Byte(it.value as kotlin.Byte)
            IrConstKind.Short -> Short(it.value as kotlin.Short)
            IrConstKind.Int -> Int(it.value as kotlin.Int)
            IrConstKind.Long -> Long(it.value as kotlin.Long)
            IrConstKind.String -> String(it.value as kotlin.String)
            IrConstKind.Float -> Float(it.value as kotlin.Float)
            IrConstKind.Double -> Double(it.value as kotlin.Double)
            IrConstKind.Null -> Null
          }
      }
    }

    operator fun get(value: Pattern0<Value>) =
      customPattern1("Ir.Const", value) { it: IrConst ->
        Components1(Value.fromIrConst(it))
      }
  }

  object When {
    context (CX.Scope) operator fun <AP : Pattern<L>, L : List<A>, A : IrBranch> get(value: AP) =
      customPattern1("Ir.When", value) { it: IrWhen ->
        Components1(it.branches)
      }
  }

  object Branch {
    // would to have a genrics A and B here but that seems to slow down kotlin pattern match to a crawl
    operator fun <AP : Pattern<IrExpression>, BP : Pattern<IrExpression>> get(condition: AP, result: BP) =
      customPattern2("Ir.Branch", condition, result) { it: IrBranch ->
        Components2(it.condition, it.result)
      }
  }

  object Vararg {
    context (CX.Scope) operator fun <AP : Pattern<List<IrExpression>>> get(x: AP): Pattern1<AP, List<IrExpression>, IrVararg> =
      customPattern1("Ir.Vararg", x) { it: IrVararg ->
        Components1(it.elements.toList())
      }
  }

  object ConstructorCall1 {
    inline fun <reified T> of() =
      Matcher(T::class.classId())

    class Matcher(val classNameRaw: ClassId?) {
      context (CX.Scope) operator fun <AP: Pattern<A>, A: IrExpression> get(x: AP): Pattern1<AP, A, IrConstructorCall> =
        customPattern1("Ir.Call.ConstructorCall1", x) { it: IrConstructorCall ->
          if (it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
            Components1(it.regularArgs.first())
          } else {
            null
          }
        }
    }
  }

  object ConstructorCall2 {
    inline fun <reified T> of() =
      Matcher(T::class.classId())

    class Matcher(val classNameRaw: ClassId?) {
      context (CX.Scope) operator fun <AP: Pattern<A>, BP: Pattern<B>, A: IrExpression, B: IrExpression> get(x: AP, y: BP): Pattern2<AP, BP, A, B, IrConstructorCall> =
        customPattern2("Ir.Call.ConstructorCall2", x, y) { it: IrConstructorCall ->
          if (it.regularArgs.size == 2 && it.regularArgs.all { it != null }) {
            Components2(it.regularArgs[0], it.regularArgs[1])
          } else {
            null
          }
        }
    }
  }

  object ConstructorCallNullableN {
    inline fun <reified T> of() =
      Matcher(T::class.classId())

    data class Args(val args: List<IrExpression?>) {
      operator fun get(index: Int): IrExpression? = args[index]
    }

    class Matcher(val classNameRaw: ClassId?) {
      context (CX.Scope) operator fun <AP: Pattern<Args>> get(x: AP): Pattern1<AP, Args, IrConstructorCall> =
        customPattern1("Ir.Call.ConstructorCallNullableN", x) { it: IrConstructorCall ->
          // Note: we could do pluginCtx.referenceClass(classNameRaw) and then
          // do a proper subtype check but I think when someone wants a constructor
          // it should be a very strict type-check and not consider super-classes.
          // Can think about a different type of pattern-matching if subtype-matching is needed
          val id = it.type.classId()
          if (id != null && id == classNameRaw) {
            Components1(Args(it.regularArgs))
          } else {
            null
          }
        }
    }
  }

  object Call {
    object NamedExtensionFunctionZeroArg {
      context (CX.Scope) operator fun <AP : Pattern<String>, BP : Pattern<E>, E : IrExpression> get(name: AP, reciever: BP): Pattern2<AP, BP, String, E, IrCall> =
        customPattern2("NamedExtensionFunctionZeroArg", name, reciever) { it: IrCall ->
          val reciever = it.extensionArg
          if (reciever != null && it.regularArgs.size == 0) {
            Components2(it.symbol.owner.kotlinFqName.asString(), reciever)
          } else {
            null
          }
        }
    }

    context (CX.Scope) operator fun <AP : Pattern<IrCall>> get(x: AP): Pattern1<AP, IrCall, IrCall> =
      customPattern1("Ir.Call", x) { it: IrCall ->
        Components1(it)
      }

    // TODO get rid of this in favor of FunctionMem
    object FunctionRec {
      context (CX.Scope) operator fun <AP : Pattern<List<IrExpression>>> get(x: AP): Pattern1<AP, List<IrExpression>, IrCall> =
        customPattern1("Ir.Call.FunctionRec", x) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.all { it != null }) {
            Components1(it.regularArgs.requireNoNulls())
          } else {
            null
          }
        }
    }

    // TODO get rid of this in favor of FunctionMem1
    object FunctionRec1 {
      // context (CX.Scope) operator fun <AP: Pattern<A>, BP: Pattern<B>, A: IrExpression, B: IrExpression> get(x: AP, y: BP) =
      context (CX.Scope) operator fun <AP : Pattern<A>, A : IrExpression, BP : Pattern<B>, B : IrExpression> get(x: AP, y: BP): Pattern2<AP, BP, A, B, IrCall> =
        customPattern2("Ir.Call.FunctionRec1", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
            Components2(reciever, it.regularArgs.first())
          } else {
            null
          }
        }
    }

    // TODO get rid of this in favor of FunctionMem1
    object FunctionRec0 {
      // context (CX.Scope) operator fun <AP: Pattern<A>, BP: Pattern<B>, A: IrExpression, B: IrExpression> get(x: AP, y: BP) =
      context (CX.Scope) operator fun <AP : Pattern<A>, A : IrExpression> get(x: AP): Pattern1<AP, A, IrCall> =
        customPattern1("Ir.Call.FunctionRec0", x) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.size == 0 && it.regularArgs.all { it != null }) {
            Components1(reciever)
          } else {
            null
          }
        }
    }

    // Would like to have a list on the generic L here but that seems to slow down kotlin pattern match to a crawl
    object FunctionMemN {
      object Caller {
        // Interesting here how we can have just AP/BP and not need the additional parameters A and B
        context (CX.Scope) operator fun <AP : Pattern<ReceiverCaller>, BP : Pattern<List<IrExpression>>> get(x: AP, y: BP): Pattern2<AP, BP, ReceiverCaller, List<IrExpression>, IrCall> =
          customPattern2("Ir.Call.FunctionMem", x, y) { it: IrCall ->
            val reciever = it.caller()
            if (reciever != null && it.regularArgs.all { it != null }) {
              Components2(reciever, it.regularArgs.requireNoNulls())
            } else {
              null
            }
          }
      }

      object NullableArgs {
        context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, BP : Pattern<List<IrExpression>>> get(x: AP, m: MP, y: BP): Pattern2<AP, BP, IrExpression, List<IrExpression>, IrCall> =
          customPattern2("Ir.Call.FunctionMemN", x, y) { it: IrCall ->
            val reciever = it.extensionArg ?: it.dispatchArg
            if (reciever != null && m.matchesAny(it.symbol.safeName)) {
              Components2(reciever, it.regularArgs)
            } else {
              null
            }
          }
      }


      context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, BP : Pattern<List<IrExpression>>> get(x: AP, m: MP, y: BP): Pattern2<AP, BP, IrExpression, List<IrExpression>, IrCall> =
        customPattern2("Ir.Call.FunctionMemN", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.safeName)) {
            Components2(reciever, it.regularArgs)
          } else {
            null
          }
        }
    }

    object FunctionMem2 {
      context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, BP : Pattern<Pair<IrExpression, IrExpression>>> get(
        x: AP,
        m: MP,
        y: BP
      ): Pattern2<AP, BP, IrExpression, Pair<IrExpression, IrExpression>, IrCall> =
        customPattern2("Ir.Call.FunctionMemN", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.size == 2 && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.safeName)) {
            Components2(reciever, it.regularArgs[0]!! to it.regularArgs[1]!!)
          } else {
            null
          }
        }

      object AllowNulls {
        context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, BP : Pattern<Pair<IrExpression?, IrExpression?>>> get(
          x: AP,
          m: MP,
          y: BP
        ): Pattern2<AP, BP, IrExpression, Pair<IrExpression?, IrExpression?>, IrCall> =
          customPattern2("Ir.Call.FunctionMemNAllowNulls", x, y) { it: IrCall ->
            val reciever = it.extensionArg ?: it.dispatchArg
            if (reciever != null && it.regularArgs.size == 2 && m.matchesAny(it.symbol.safeName)) {
              Components2(reciever, it.regularArgs[0] to it.regularArgs[1])
            } else {
              null
            }
          }
      }
    }

    object FunctionMemAllowNulls {
      // Interesting here how we can have just AP/BP and not need the additional parameters A and B
      context (CX.Scope) operator fun <AP : Pattern<ReceiverCaller>, BP : Pattern<List<IrExpression>>> get(x: AP, y: BP): Pattern2<AP, BP, ReceiverCaller, List<IrExpression?>, IrCall> =
        customPattern2("Ir.Call.FunctionMemAllowNulls", x, y) { it: IrCall ->
          val reciever = it.caller()
          if (reciever != null) {
            Components2(reciever, it.regularArgs)
          } else {
            null
          }
        }
    }

    // Member Function1
    object FunctionMem1 {

      context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, BP : Pattern<B>, B : IrExpression> get(x: AP, m: MP, y: BP): Pattern2<AP, BP, IrExpression, B, IrCall> =
        customPattern2("Ir.Call.FunctionMem1", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.size == 1 && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.safeName)) {
            Components2(reciever, it.regularArgs.first())
          } else {
            null
          }
        }

      object WithCaller {
        /**
         * This is an interesting pattern because we can match on a middle-component inside of the logic of the customPattern without actually needing to
         * pass it deep into the Then2 sets of functions. We can match the pattern in the business logic below and then return the components.
         * Since it is unlikely that we will want some kind of structure based decompositon of the name of the function (e.g. we won't want some
         * kind of Pattern2 that decomposes the string into multiple pieces). Therefore we can treat the middle-parameter as a simple Pattern argument
         * that has no effect on other side of the `case` expressions.
         */
        context (CX.Scope) operator fun <AP : Pattern<ReceiverCaller>, MP : Pattern<String>, BP : Pattern<IrExpression>> get(x: AP, m: MP, y: BP): Pattern2<AP, BP, ReceiverCaller, IrExpression, IrCall> =
          customPattern2("Ir.Call.FunctionMem1.WithCaller", x, y) { it: IrCall ->
            val reciever = it.caller()
            if (reciever != null && it.regularArgs.size == 1 && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.safeName)) {
              Components2(reciever, it.regularArgs.first())
            } else {
              null
            }
          }

        /**
         * This is a similar pattern to the one above but there is one more insight. Namely that so long as you don't care about structurally decomposing the middle-parameter
         * (i.e. since it's a string that  we assume we don't care about breaking down into component parts ...since it's just being treated as a function-name match)
         * then we can not only match on the the middle paramter in the customPattern logic, but we can also return the string itself on the other side of the `case`  logic
         * using the Pattern2M family of functions. This gives us both the ability to match on the middle parameter and return it as a component, so long as
         * we don't care about passing it to further Pattern___ functions that would deconstruct them (because they would not turn into nested Component(T) instances
         * on the other side of the `case` function.
         */
        object Named {
          // context (CX.Scope) operator fun <AP: Pattern<A>, BP: Pattern<B>, A: IrExpression, B: IrExpression> get(x: AP, y: BP) =
          context (CX.Scope) operator fun <AP : Pattern<ReceiverCaller>, MP : Pattern<String>, BP : Pattern<IrExpression>> get(x: AP, m: MP, y: BP): Pattern2M<AP, String, BP, ReceiverCaller, IrExpression, IrCall> =
            customPattern2M("Ir.Call.FunctionMem1.WithCaller.Named", x, y) { it: IrCall ->
              val reciever = it.caller()
              if (reciever != null && it.regularArgs.size == 1 && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.safeName)) {
                Components2M(reciever, it.symbol.safeName, it.regularArgs.first())
              } else {
                null
              }
            }
        }
      }
    }

    object FunctionMem0 {
      object WithCaller {
        // context (CX.Scope) operator fun <AP: Pattern<A>, BP: Pattern<B>, A: IrExpression, B: IrExpression> get(x: AP, y: BP) =
        context(CX.Scope) operator fun <AP : Pattern<A>, MP : Pattern<String>, A : ReceiverCaller> get(x: AP, y: MP): Pattern1<AP, A, IrCall> =
          customPattern1("Ir.Call.FunctionMem0.WithCaller", x) { it: IrCall ->
            val reciever = it.caller()
            if (reciever != null && it.regularArgs.size == 0 && y.matchesAny(it.symbol.safeName)) {
              Components1(reciever)
            } else {
              null
            }
          }
      }

      context(CX.Scope) operator fun <AP : Pattern<IrExpression>, BP : Pattern<String>> get(x: AP, y: BP): Pattern2<AP, BP, IrExpression, String, IrCall> =
        customPattern2("Ir.Call.FunctionMem0", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null && it.regularArgs.size == 0) {
            Components2(reciever, it.symbol.safeName)
          } else {
            null
          }
        }
    }

    object FunctionUntethered0 {
      context (CX.Scope) operator fun <AP : Pattern<String>> get(x: AP) =
        customPattern1("Ir.Call.FunctionUntethered0", x) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever == null && it.regularArgs.size == 0) {
            Components1(it.symbol.fullName)
          } else {
            null
          }
        }
    }

    object FunctionMemVararg {
      context (CX.Scope) operator fun <AP : Pattern<IrExpression>, MP : Pattern<String>, TP : Pattern<IrType>, BP : Pattern<List<IrExpression>>> get(
        x: AP,
        m: MP,
        yTpe: TP,
        y: BP
      ): Pattern2<AP, BP, IrExpression, List<IrExpression>, IrCall> =
        customPattern2("Ir.Call.FunctionMemVararg", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever != null
            && it.regularArgs.size == 1
            && it.regularArgs.first() != null
            && (it.regularArgs.first() as? IrVararg)?.let { varg -> varg.elements.all { it is IrExpression } && yTpe.matchesAny(varg.varargElementType) } ?: false
            && m.matchesAny(it.symbol.safeName)
          ) {
            val varargElem = it.regularArgs.first() as IrVararg
            Components2(reciever, varargElem.elements.map { it as IrExpression }.toList())
          } else {
            null
          }
        }
    }

    // not a function on an object or class i.e. top-level
    object FunctionUntethered1 {
      object Arg {
        /*context (CX.Scope) operator fun <AP: Pattern<IrExpression>> get(x: AP) =
          customPattern1(x) { it: IrCall ->
            val reciever = it.extensionArg ?: it.dispatchArg
            if (reciever == null && it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
              Components1(it.regularArgs.first())
            } else {
              null
            }
          }*/

        context (CX.Scope) operator fun <AP : Pattern<E>, E : IrExpression> get(x: AP): Pattern1<AP, E, IrCall> =
          customPattern1("Ir.Call.FunctionUntethered1.Arg", x) { it: IrCall ->
            val reciever = it.extensionArg ?: it.dispatchArg
            if (reciever == null && it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
              Components1(it.regularArgs.first())
            } else {
              null
            }
          }
      }

      context (CX.Scope) operator fun <AP : Pattern<String>, BP : Pattern<IrExpression>> get(x: AP, y: BP): Pattern2<AP, BP, String, IrExpression, IrCall> =
        customPattern2("Ir.Call.FunctionUntethered1", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever == null && it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
            Components2(it.symbol.fullName, it.regularArgs.first() ?: error("Expected non-null value"))
          } else {
            null
          }
        }
    }

    object FunctionUntetheredN {
      context (CX.Scope) operator fun <AP : Pattern<String>, BP : Pattern<List<IrExpression?>>> get(x: AP, y: BP): Pattern2<AP, BP, String, List<IrExpression?>, IrCall> =
        customPattern2("Ir.Call.FunctionUntetheredN", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever == null) {
            Components2(it.symbol.fullName, it.regularArgs)
          } else {
            null
          }
        }
    }

    object FunctionUntethered2 {
      context (CX.Scope) operator fun <AP : Pattern<A>, MP : Pattern<String>, BP : Pattern<B>, A : IrExpression, B : IrExpression> get(m: MP, x: AP, y: BP): Pattern2<AP, BP, A, B, IrCall> =
        customPattern2("Ir.Call.FunctionUntethered2", x, y) { it: IrCall ->
          val reciever = it.extensionArg ?: it.dispatchArg
          if (reciever == null && it.regularArgs.size == 2 && it.regularArgs.all { it != null } && m.matchesAny(it.symbol.fullName)) {
            Components2(it.regularArgs.first(), it.regularArgs.get(1))
          } else {
            null
          }
        }
    }

    object Property {
      sealed interface PropertyKind {
        data class Named(val name: String, val isRenamed: Boolean) : PropertyKind
        data class Component(val index: Int) : PropertyKind
      }

      context (CX.Scope) operator fun <AP : Pattern<IrExpression>, BP : Pattern<PropertyKind>> get(host: AP, name: BP) =
        customPattern2("Ir.Call.Property", host, name) { it: IrCall ->
          // if there exists both a dispatch reciever and an extension reciever it's an extension
          // of some class defined inside of some other class, in that case we only care about the extension reciever
          val reciever = it.extensionArg ?: it.dispatchArg
          val isProperty =
            when (it.origin) {
              IrStatementOrigin.GET_PROPERTY -> true
              IrStatementOrigin.GET_LOCAL_PROPERTY -> true
              else -> false
            }

          fun isComponent() = it.regularArgs.size == 0 && it.symbol.safeName.matches(Regex("component[0-9]+"))

          fun exoFieldArgValue() =
            it.getPropertyAnnotationArgs<ExoField>().firstConstStringOrNull()

          fun serialNameArgValue() =
            it.getPropertyAnnotationArgs<SerialName>().firstConstStringOrNull()

          // if there is a reciever and a single value property then this is a property call and we return it, otherwise it is not
          when {
            isProperty && reciever != null && it.regularArgs.all { it != null } -> {
              // @ExoField name takes priority, then serialNameArgValue, then use the property name
              // In the future should add support for class-level naming schemes e.g. @ExoNaming(UnderScore), @ExoNaming(UnderScoreCapitalized)
              val (argValue, isRenamed) =
                exoFieldArgValue()?.let { it to true }
                  ?: serialNameArgValue()?.let { it to true }
                  ?: it.symbol.sanitizedSymbolName().let { it to false }

              Components2(reciever, PropertyKind.Named(argValue, isRenamed))
            }

            isComponent() ->
              // Note that component is actually a 1-based index so need to subtract 1 to make it line up XRType field names (which we choose from based on it)
              Components2(reciever, PropertyKind.Component(it.symbol.safeName.removePrefix("component").toInt() - 1))

            else -> null
          }
        }
    }
  }


  object BlockBody {
    // Need to use type-parameter like this or matching (e.g. in SimpleBlockBody) won't type correctly
    operator fun <AP : Pattern<A>, A : List<S>, S : IrStatement> get(statements: AP) =
      customPattern1("BlockBody", statements) { it: IrBlockBody ->
        Components1(it.statements.toList())
      }

    // Single element returning block body
    object ReturnOnly {
      operator fun <AP : Pattern<IrExpression>> get(statements: AP) =
        customPattern1("BlockBody.ReturnOnly", statements) { it: IrBlockBody ->
          on(it).match(
            case(BlockBody[List1[Is<IrReturn>()]]).then { (irReturn) -> Components1(irReturn.value) }
          )
        }
    }

    object StatementsWithReturn {
      operator fun <AP : Pattern<List<IrStatement>>, BP : Pattern<IrExpression>> get(statements: AP, ret: BP) =
        customPattern2("BlockBody.StatementsWithReturn", statements, ret) { it: IrBlockBody ->

          if (it.statements.size > 0 && it.statements.last() is IrReturn) {
            Components2(it.statements.dropLast(1), (it.statements.last() as IrReturn).value)
          } else {
            null
          }
        }
    }

    object Return {
      operator fun <AP : Pattern<A>, A : IrExpression> get(statements: AP) =
        customPattern1("BlockBody.Return", statements) { it: IrBlockBody ->
          on(it).match(
            case(BlockBody[Is()]).then { statements ->
              statements.find { it is IrReturn }?.let { Components1((it as IrReturn).value) }
            }
          )
        }
    }

    object Statements {
      operator fun <AP : Pattern<A>, A : List<IrStatement>> get(statements: AP) =
        customPattern1("BlockBody.Statements", statements) { it: IrBlockBody ->
          on(it).match(
            case(BlockBody[Is()]).then { statements -> Components1(statements) }
          )
        }
    }
  }

  object Return {
    operator fun <AP : Pattern<A>, A : IrExpression> get(statements: AP) =
      customPattern1("Return", statements) { it: IrReturn ->
        Components1(it.value)
      }
  }

  object ReturnBlockInto {
    operator fun <AP : Pattern<A>, A : IrExpression> get(statements: AP) =
      customPattern1("ReturnBlockInto", statements) { it: IrBlockBody ->
        it.match(
          case(Ir.BlockBody[List1[Ir.Return[Is()]]])
            .then { (irReturn) ->
              Components1(irReturn.value)
            }
        )
      }
  }

  /** I.e. a Lambda! */
  object FunctionExpression {
    operator fun <AP : Pattern<A>, A : IrSimpleFunction> get(body: AP)  /*: Pattern1<AP, A, IrFunctionExpression>*/ =
      // Note, each one of these in here needs to be it:IrExression, not it:IrFunctionExpression or they will never match arbitrary IrExpression instances
      customPattern1("FunctionExpression", body) { it: IrFunctionExpression ->
        Components1(it.function)
      }

    object withReturnOnlyBlock {
      operator fun <AP : Pattern<A>, A : IrExpression> get(body: AP) =
        customPattern1("FunctionExpression.withReturnOnlyBlock", body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withReturnOnlyExpression[Is()]]).then { (expr) ->
              Components1(expr)
            }
          )
        }
    }

    object withReturnOnlyBlockAndArgs {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<IrExpression>> get(params: AP, body: BP) =
        customPattern2("FunctionExpression.withBlock", params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withReturnOnlyExpressionAndArgs[Is(), Is()]])
              .then { (params, body) -> Components2(params, body) }
          )
        }
    }

    object withBlock {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<IrBlockBody>> get(params: AP, body: BP) =
        customPattern2("FunctionExpression.withBlock", params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withBlock[Is(), Is()]])
              .then { (params, body) -> Components2(params, body) }
          )
        }
    }

    object withBlockStatements {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<List<IrStatement>>> get(params: AP, body: BP) =
        customPattern2("FunctionExpression.withBlockStatements", params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withBlockStatements[Is(), Is()]])
              .then { (params, statements) -> Components2(params, statements) }
          )
        }
    }

    object withBlockStatementsAndReturn {
      data class Output(val statements: List<IrStatement>, val ret: IrExpression)

      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<Output>> get(params: AP, body: BP) =
        customPattern2("FunctionExpression.withBlockStatementsAndReturn", params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withBlockStatements[Is(), Is()]])
              .thenIf { (params, statements) -> statements.lastOrNull() is IrReturn }
              .then { (params, statements) -> Components2(params, Output(statements.dropLast(1), (statements.last() as IrReturn).value)) }
          )
        }
    }
  }

  object FunctionExpression1 {
    operator fun <AP : Pattern<A>, A : IrSimpleFunction> get(body: AP)  /*: Pattern1<AP, A, IrFunctionExpression>*/ =
      // Note, each one of these in here needs to be it:IrExression, not it:IrFunctionExpression or they will never match arbitrary IrExpression instances
      customPattern1("FunctionExpression", body) { it: IrFunctionExpression ->
        Components1(it.function)
      }

    object withBlock {
      operator fun <AP : Pattern<IrValueParameter>, BP : Pattern<IrBlockBody>> get(params: AP, body: BP) =
        customPattern2("FunctionExpression.withBlock", params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression1[SimpleFunction.withBlock[Is(), Is()]])
              .thenIf { (params, _) -> params.size == 1 }
              .then { (params, body) -> Components2(params.first(), body) }
          )
        }
    }
  }

  object SimpleFunction {

    sealed interface FunBody {
      // Describes a situation where there is an IrBlockBody with multiple statements and possibly a return at the end
      data class Complex(val statements: List<IrStatement>, val returnExpr: IrExpression?): FunBody
      // Describes a situation where there is an IrExpressionBody or an IrBlockBody with one single return
      data class Simple(val expr: IrExpression): FunBody
    }

    operator fun <AP : Pattern<A>, BP : Pattern<B>, A : List<IrValueParameter>, B : FunBody> get(args: AP, body: BP): Pattern2<AP, BP, A, B, IrSimpleFunction> =
      customPattern2("Ir.SimpleFunction", args, body) { it: IrSimpleFunction ->
        it.body?.let { bodyVal ->
          when {
            bodyVal.statements.size == 1 && bodyVal.statements.first() is IrExpression -> {
              when (val singleVal = bodyVal.statements.first()) {
                is IrReturn -> Components2(it.regularParams, FunBody.Simple(singleVal.value))
                else -> Components2(it.regularParams, FunBody.Simple(singleVal as IrExpression))
              }
            }
            bodyVal.statements.size > 1 -> {
              when (val lastVal = bodyVal.statements.last()) {
                is IrReturn -> Components2(it.regularParams, FunBody.Complex(bodyVal.statements.dropLast(1), lastVal.value))
                else -> Components2(it.regularParams, FunBody.Complex(bodyVal.statements, null))
              }
            }
            else -> parseError("The function ${it.name} body was not a blockBody")
          }
        }
      }

    object withReturnOnlyExpression {
      operator fun <AP : Pattern<A>, A : IrExpression> get(body: AP) =
        customPattern1("Ir.SimpleFunction.withReturnOnlyExpression", body) { it: IrSimpleFunction ->
          on(it).match(
            case(SimpleFunction[Is(), Is()]).then { _, b ->
              when (b) {
                is FunBody.Simple -> Components1(b.expr)
                else -> null
              }
            }
          )
        }
    }

    // Matches any kind of IrSimpleFunction including the kind that is a declaration-stub
    object anyKind {
      operator fun <AP : Pattern<A>, A : IrSimpleFunction> get(body: AP) =
        customPattern1("Ir.SimpleFunction.anyKind", body) { it: IrSimpleFunction ->
          Components1(it)
        }
    }

    object withReturnOnlyExpressionAndArgs {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<B>, B : IrExpression> get(args: AP, body: BP) =
        customPattern2("Ir.SimpleFunction.withReturnOnlyExpressionAndArgs", args, body) { it: IrSimpleFunction ->
          on(it).match(
            case(SimpleFunction[Is(), Is<FunBody.Simple>()]).then { args, b ->
              Components2(args, b.expr)
            }
          )
        }
    }

    object withBlock {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<IrBlockBody>> get(params: AP, body: BP) =
        customPattern2("Ir.SimpleFunction.withBlock", params, body) { it: IrSimpleFunction ->
          it.body?.let { bodyVal ->
            when (bodyVal) {
              is IrBlockBody -> Components2(it.regularParams, bodyVal)
              else -> null
            }
          }
        }
    }

    object withBlockStatements {
      operator fun <AP : Pattern<List<IrValueParameter>>, BP : Pattern<List<IrStatement>>> get(params: AP, body: BP) =
        customPattern2("Ir.SimpleFunction.withBlockStatements", params, body) { it: IrSimpleFunction ->
          it.body?.let { bodyVal ->
            when (bodyVal) {
              is IrBlockBody -> Components2(it.regularParams, bodyVal.statements)
              else -> null
            }
          }
        }
    }
  }

}
