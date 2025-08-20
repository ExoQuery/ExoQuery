package io.exoquery.plugin.trees

import io.decomat.Is
import io.decomat.Is.Companion.invoke
import io.decomat.case
import io.decomat.on
import io.exoquery.generation.typemap.ClassOf
import io.exoquery.generation.typemap.From
import io.exoquery.generation.typemap.TypeMap
import io.exoquery.generation.typemap.TypeMap.Companion.invoke
import io.exoquery.parseError
import io.exoquery.plugin.classId
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.trees.Unlifter.withIndexAndLookup
import io.exoquery.plugin.trees.UnlifterBasics.orFail
import io.exoquery.plugin.trees.UnlifterBasics.unliftString
import io.exoquery.plugin.trees.UnlifterBasics.unliftStringIfNotNull
import io.exoquery.plugin.trees.UnlifterBasics.unliftInt
import io.exoquery.plugin.trees.UnlifterBasics.unliftBoolean
import io.exoquery.plugin.trees.UnlifterBasics.unliftIntIfNotNull
import io.exoquery.plugin.trees.UnlifterBasics.unliftBooleanIfNotNull
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


object UnlifterTypeMap {
  context (CX.Scope)
  fun TypeMap.Companion.unlift(expr: IrExpression): TypeMap =
    on(expr).match(
      case(Ir.Call.FunctionMem1[Ir.GetObjectValue<TypeMap.Companion>(), Is("invoke"), Ir.Vararg[Is()]]).then { _, (args) ->
        TypeMap(*args.map { unliftTypeMapEntry(it) }.toTypedArray())
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple TypeMap(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like Code.TypeMap can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        TypeMap(
          From(columnName = "my_column"),
          ClassOf<MyType>()
        )
      """.trimIndent()
    )

  context (CX.Scope)
  fun unliftTypeMapEntry(expr: IrExpression): Pair<From, ClassOf> =
    on(expr).match(
      case(ExtractorsDomain.Call.`x to y`[Is(), Is()]).then { x, y ->
        From.unlift(x) to ClassOf.unlift(y)
      },
      case(Ir.ConstructorCallNullableN.of<Pair<*, *>>()[Is()]).then { args ->
        val x = args[0] ?: parseError("Expected a non-null first argument for TypeMap entry", expr)
        val y = args[1] ?: parseError("Expected a non-null second argument for TypeMap entry", expr)
        From.unlift(x) to ClassOf.unlift(y)
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple TypeMap(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like Code.TypeMapEntry can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        TypeMap(
          From(columnName = "my_column"),
          ClassOf<MyType>()
        )
      """.trimIndent()
    )

  context (CX.Scope)
  fun From.Companion.unlift(expr: IrExpression): From =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<From>()[Is()]).then { args ->
        val idx = args.withIndexAndLookup()

        From(
          unliftStringIfNotNull(idx.next()),
          unliftStringIfNotNull(idx.next()),
          unliftStringIfNotNull(idx.next()),
          unliftStringIfNotNull(idx.next()),
          unliftIntIfNotNull(idx.next()),
          unliftBooleanIfNotNull(idx.next()) ?: From.DefaultMatchCaseSensitive
        )
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple From(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like From can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        From(columnName = "my_column")
      """.trimIndent()
    )

  context (CX.Scope)
  fun ClassOf.Companion.unlift(expr: IrExpression) =
    on(expr).match(
      // The ClassOf("MyType") constructor
      case(Ir.ConstructorCallNullableN.of<ClassOf>()[Is()]).then { args ->
        ClassOf(unliftStringIfNotNull(args[0]) ?: parseError("Expected a non-null class string", expr))
      },
      // The ClassOf<MyType>() constructor
      case(Ir.Call.FunctionMem0[Ir.GetObjectValue<ClassOf.Companion>(), Is("invoke")]).thenThis { _, _ ->
        val type = this.typeArguments[0]
        if (type == null) parseError("Expected a non-null type argument for ClassOf", expr)
        val typeClassId = type.classId() ?: parseError("Expected a class-type argument for ClassOf but was ${type.dumpKotlinLike()}", expr)
        ClassOf(typeClassId.asFqNameString())
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple To(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like To can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        To(columnName = "my_column")
      """.trimIndent()
    )
}
