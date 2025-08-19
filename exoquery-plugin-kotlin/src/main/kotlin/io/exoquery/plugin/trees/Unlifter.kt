package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.annotation.ExoInternal
import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NameProcessorLLM
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.TableGrouping
import io.exoquery.parseError
import io.exoquery.plugin.source
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.varargValues
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg

@OptIn(ExoInternal::class)
object Unlifter {

  context (CX.Scope)
  private fun orFail(expr: IrExpression, additionalMsg: String? = null): Nothing =
    parseError("Failed to read the compile time construct. ${additionalMsg}", expr) // TODO need a MUCH BETTER error here

  context (CX.Scope)
  fun unliftString(expr: IrExpression): String =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.String) ?: parseError("Expected a constant string", expr)
        it.value as? String ?: parseError("Constant value was not a string", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant (i.e. compile-time) string but it was not"
      )

  context (CX.Scope)
  fun unliftInt(expr: IrExpression): Int =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.Int) ?: parseError("Expected a constant int", expr)
        it.value as? Int ?: parseError("Constant value was not an int", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant int (e.g. the value 18) but it was not"
      )

  context (CX.Scope)
  fun unliftBoolean(expr: IrExpression): Boolean =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.Boolean) ?: parseError("Expected a constant boolean", expr)
        it.value as? Boolean ?: parseError("Constant value was not a boolean", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant boolean (i.e. the value 'true' or 'false') but it was not"
      )

  context (CX.Scope)
  fun unliftStringIfNotNull(expr: IrExpression?) : String? =
    expr?.let { unliftString(it) }

  context (CX.Scope)
  fun DatabaseDriver.Companion.unlift(expr: IrExpression): DatabaseDriver =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.Postgres>()[Is()]).then { args ->
        DatabaseDriver.Postgres(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.Postgres.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.MySQL>()[Is()]).then { args ->
        DatabaseDriver.MySQL(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.MySQL.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.SQLite>()[Is()]).then { args ->
        DatabaseDriver.SQLite(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.SQLite.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.H2>()[Is()]).then { args ->
        DatabaseDriver.H2(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.H2.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.Oracle>()[Is()]).then { args ->
        DatabaseDriver.Oracle(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.Oracle.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.SqlServer>()[Is()]).then { args ->
        DatabaseDriver.SqlServer(unliftStringIfNotNull(args[0].lookupIfVar()) ?: DatabaseDriver.SqlServer.DefaultUrl)
      },
      case(Ir.ConstructorCall2.of<DatabaseDriver.Custom>()[Is(), Is()]).then { a, b ->
        DatabaseDriver.Custom(unliftString(a), unliftString(b))
      }
    ) ?:
    orFail(
      expr,
      """
        The expression was not a simple DatabaseDriver construct with zero variables or substitutions.
        Compile-time code-generation constructs like DatabaseDriver can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        Code.DataClasses(
          ...,
          DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/mydb")
        )
      """.trimIndent()
    )

  context (CX.Scope)
  fun TableGrouping.Companion.unlift(expr: IrExpression): TableGrouping =
    on(expr).match(
      case(Ir.GetObjectValue<TableGrouping.SchemaPerObject>()).then { TableGrouping.SchemaPerObject },
      case(Ir.GetObjectValue<TableGrouping.SchemaPerPackage>()).then { TableGrouping.SchemaPerPackage }
    ) ?:
    orFail(
      expr,
      """
        The expression was not a simple TableGrouping construct with zero variables or substitutions.
        Compile-time code-generation constructs like TableGrouping can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        Code.DataClasses(
          ...,
          TableGrouping.SchemaPerPackage
        )
      """.trimIndent()
    )

  @OptIn(ExoInternal::class)
  context (CX.Scope)
  fun LLM.Companion.unlift(expr: IrExpression): LLM =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<LLM.Ollama>()[Is()]).then { args ->
        LLM.Ollama(
          model = unliftStringIfNotNull(args[0].lookupIfVar()) ?: LLM.Ollama.DefaultModel,
          url = unliftStringIfNotNull(args[1].lookupIfVar()) ?: LLM.Ollama.DefaultUrl
        )
      },
      case(Ir.ConstructorCallNullableN.of<LLM.OpenAI>()[Is()]).then { args ->
        LLM.OpenAI(
          model = unliftStringIfNotNull(args[0].lookupIfVar()) ?: LLM.OpenAI.DefaultModel
        )
      }
    ) ?:
    orFail(
      expr,
      """
        The expression was not a simple LLM.Ollama/LLM.OpenAPI call with zero variables or substitutions.
        Compile-time code-generation constructs like LLM.___ can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        NameParser.UsingLLM(LLM.Ollama("llama2", "http://localhost:11434"))
      """.trimIndent()
    )

  context (CX.Scope)
  fun NameParser.UsingLLM.Companion.unlift(expr: IrExpression): NameParser.UsingLLM =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<NameParser.UsingLLM>()[Is()]).then { args ->
        val idx = args.withIndexAndLookup()
        NameParser.UsingLLM(
          idx.next()?.let { LLM.unlift(it) } ?: parseError("TypeOfLLM needs to be specified.", expr),
          idx.next()?.let { unliftInt(it) } ?: NameParser.UsingLLM.DefaultMaxTablesPerCall,
          idx.next()?.let { unliftInt(it) } ?: NameParser.UsingLLM.DefaultMaxColumnsPerCall,
          idx.next()?.let { unliftStringIfNotNull(it) } ?: NameParser.UsingLLM.DefaultSystemPromptTables,
          idx.next()?.let { unliftStringIfNotNull(it) } ?: NameParser.UsingLLM.DefaultSystemPromptColumns,
          if (idx.next() == null)
            NameProcessorLLM.CompileTimeProvided
          else
            parseError("The NameProcessorLLM is a construct that is supplied by the compiler plugin. Most of the time it should be left to the default value.", expr)
        )
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun NameParser.Target.Companion.unlift(expr: IrExpression): NameParser.Target =
    on(expr).match(
      case(Ir.GetObjectValue<NameParser.Target.Table>()).then { NameParser.Target.Table },
      case(Ir.GetObjectValue<NameParser.Target.Column>()).then { NameParser.Target.Column },
      case(Ir.GetObjectValue<NameParser.Target.Both>()).then { NameParser.Target.Both }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple NameParser.Target/Column/Both call with zero variables or substitutions.
        Compile-time code-generation constructs like NameParser.Target.___ can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        NameParser.UsingRegex(..., target = NameParser.Target.Both)
      """.trimIndent()
    )

  context (CX.Scope)
  fun NameParser.UsingRegex.Companion.unlift(expr: IrExpression): NameParser.UsingRegex =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<NameParser.UsingRegex>()[Is()]).then { args ->
        val idx = args.withIndexAndLookup()
        NameParser.UsingRegex(
          idx.next()?.let { unliftString(it) },
          idx.next()?.let { unliftString(it) },
          idx.next()?.let { NameParser.Target.unlift(it) } ?: NameParser.UsingRegex.DefaultTarget,
        )
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple NameParser.UsingRegex(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like NameParser.UsingRegex can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        NameParser.UsingRegex("regex", "replace", target = NameParser.Target.Both)
      """.trimIndent()
    )

  context (CX.Scope)
  fun NameParser.Composite.Companion.unlift(expr: IrExpression): NameParser.Composite =
    on(expr).match(
      case(Ir.Call.FunctionMem2[Ir.GetObjectValue<NameParser.Composite.Companion>(), Is("invoke"), Is()]).then { _, (nameParser, otherNameParsers) ->
        val parser = NameParser.unlift(nameParser)
        val otherParsers = ((otherNameParsers as? IrVararg)?.let{ it.varargValues().map { NameParser.unlift(it) } } ?: emptyList())
        NameParser.Composite(parser, *otherParsers.toTypedArray())
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple NameParser.Composite(...) call with zero variables or substitutions.
        Compile-time code-generation constructs like NameParser.Composite can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        NameParser.Composite(NameParser.Literal, NameParser.UsingRegex("regex", "replace"))
      """.trimIndent()
    )

  context (CX.Scope)
  fun NameParser.Companion.unlift(expr: IrExpression): NameParser =
    on(expr).match(
      case(Ir.Expr.ClassOf<NameParser.UsingLLM>()).then { _ -> NameParser.UsingLLM.unlift(expr) },
      case(Ir.Expr.ClassOf<NameParser.Literal>()).then { _ -> NameParser.Literal },
      case(Ir.Expr.ClassOf<NameParser.SnakeCase>()).then { _ -> NameParser.SnakeCase },
      case(Ir.Expr.ClassOf<NameParser.UsingRegex>()).then { _ -> NameParser.UsingRegex.unlift(expr) },
      case(Ir.Expr.ClassOf<NameParser.Composite>()).then { _ -> NameParser.Composite.unlift(expr) },
      case(Ir.Expr.ClassOf<NameParser.CapitalizeColumns>()).then { _ -> NameParser.CapitalizeColumns },
      case(Ir.Expr.ClassOf<NameParser.UncapitalizeColumns>()).then { _ -> NameParser.UncapitalizeColumns },
      case(Ir.Expr.ClassOf<NameParser.CapitalizeTables>()).then { _ -> NameParser.CapitalizeTables },
      case(Ir.Expr.ClassOf<NameParser.UncapitalizeTables>()).then { _ -> NameParser.UncapitalizeTables }
    ) ?: orFail(
        expr,
        """
          The expression was not a simple NameParser construct with zero variables or substitutions.
          Compile-time code-generation constructs like NameParser can only use constant values, no variables, functions, branching, or other logic is allowed.
          A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
          code generation, use the runtime code generation technique instead.
        """.trimIndent()
      )

  context (CX.Scope)
  fun CodeVersion.Companion.unlift(expr: IrExpression): CodeVersion =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<CodeVersion.Fixed>()[Is()]).then { args -> CodeVersion.Fixed(unliftString(args[0].lookupIfVar() ?: parseError("Expected a non-null code version", expr))) },
      case(Ir.GetObjectValue<CodeVersion.Floating>()).then { CodeVersion.Floating }
    ) ?:
    orFail(
      expr,
      """
        The expression was not a simple CodeVersion.Fixed(...)/CodeVersion.Floating construct with zero variables or substitutions.
        Compile-time code-generation constructs like CodeVersion can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"),
          ...
        )
      """.trimIndent()
    )

  @OptIn(ExoInternal::class)
  context (CX.Scope)
  fun UnrecognizedTypeStrategy.Companion.unlift(expr: IrExpression): UnrecognizedTypeStrategy =
    on(expr).match(
      case(Ir.GetObjectValue<UnrecognizedTypeStrategy.AssumeString>()).then { UnrecognizedTypeStrategy.AssumeString },
      case(Ir.GetObjectValue<UnrecognizedTypeStrategy.SkipColumn>()).then { UnrecognizedTypeStrategy.SkipColumn },
      case(Ir.GetObjectValue<UnrecognizedTypeStrategy.ThrowTypingError>()).then { UnrecognizedTypeStrategy.ThrowTypingError }
    ) ?:
    orFail(
      expr,
      """
        The expression was not a simple UnrecognizedTypeStrategy.AssumeString/UnrecognizedTypeStrategy.SkipColumn/UnrecognizedTypeStrategy.ThrowTypingError call with zero variables or substitutions.
        Compile-time code-generation constructs like UnrecognizedTypeStrategy can only use constant values, no variables, functions, branching, or other logic is allowed.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        Code.DataClasses(
          ...,
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
        )
      """.trimIndent()
    )

  context (CX.Scope)
  fun IrExpression?.lookupIfVar() =
    if (this == null) null
    else
      on(this).match(
        case(Ir.GetValue[Is()]).then {
          on(it.symbol.owner).match(
            case(Ir.Variable[Is(), Is()]).then { _, expr -> expr }
          )
        },
      ) ?: this

  context (CX.Scope)
  fun Code.DataClasses.Companion.unlift(expr: IrExpression): Code.DataClasses = run {
    fun process(args: Ir.ConstructorCallNullableN.Args) = run {
      val idx = args.withIndexAndLookup()
      Code.DataClasses(
        idx.next()?.let { CodeVersion.unlift(it) } ?: parseError("Expected a non-null CodeVersion", expr),
        idx.next()?.let { DatabaseDriver.unlift(it) } ?: parseError("Expected a non-null DatabaseDriver", expr),
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) } ?: Code.DataClasses.DefaultPropertiesFile,
        idx.next()?.let { NameParser.unlift(it) } ?: Code.DataClasses.DefaultNameParser,
        idx.next()?.let { TableGrouping.unlift(it) } ?: Code.DataClasses.DefaultTableGrouping,
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { unliftString(it) },
        idx.next()?.let { UnrecognizedTypeStrategy.unlift(it) } ?: Code.DataClasses.DefaultUnrecognizedTypeStrategy,
        idx.next()?.let { unliftBoolean(it) } ?: Code.DataClasses.DefaultDryRun,
        idx.next()?.let { unliftBoolean(it) } ?: Code.DataClasses.DefaultDetailedLogs
      )
    }

    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<Code.DataClasses>()[Is()]).then { args ->
        process(args)
      },
      /*
      When there's a clause out of order e.g. DataClasses(
        CodeVersion.Fixed("1.0.0"),
        DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent"),
        schemaFilter = "purposely_inconsistent",
        packagePrefix = "io.exoquery.example.schemaexample.content",
      )
      It shows up like this:
      { // BLOCK
        val tmp0_codeVersion: Fixed = Fixed(version = "1.0.0")
        val tmp1_driver: Postgres = Postgres(jdbcUrl = "jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent")
        DataClasses(codeVersion = tmp0_codeVersion, driver = tmp1_driver, packagePrefix = "io.exoquery.example.schemaexample.content", schemaFilter = "purposely_inconsistent")
      }
      We need to account for this structure by skipping the variables in the block and then looking them up later
       */
      case(Ir.Block[Is(), Ir.ConstructorCallNullableN.of<Code.DataClasses>()[Is()]]).thenIf { stmts, _ -> stmts.all { it is IrVariable } }.then { stmts, (args) ->
        process(args)
      }
    ) ?: orFail(
      expr,
      """
        The expression was not a simple Code.DataClasses(CodeVersion, DatabaseDriver, ...) construct with zero variables or substitutions.
        Compile-time code-generation constructs like Code.DataClasses can only use constant values, no variables, functions, branching, or other logic is allowed.
        Nullable values can be implicitly skipped if not used.
        A compile-time code generation construct is a essentially a configuration embedded within Kotlin code. For more flexible
        code generation, use the runtime code generation technique instead.
        ============ For example, a valid value is: ============
        Code.DataClasses(
          CodeVersion.Fixed("0.1.0"),
          DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/mydb"),
          "my.package",
          usernameEnvVar = "DB_USERNAME",
          passwordEnvVar = "DB_PASSWORD",
          ...
        )
      """.trimIndent()
    )
  }

  context (CX.Scope)
  fun unliftCodeDataClasses(expr: IrExpression): Code.DataClasses =
    Code.DataClasses.unlift(expr)


  class ArgsWithIndexAndLookup(val args: Ir.ConstructorCallNullableN.Args) {
    private var index = 0
    context (CX.Scope)
    fun next() = run {
      val argsCurr = args[index]
      index += 1
      argsCurr.lookupIfVar()
    }
  }
  fun Ir.ConstructorCallNullableN.Args.withIndexAndLookup() =
    ArgsWithIndexAndLookup(this)
}
