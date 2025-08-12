package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NameProcessorLLM
import io.exoquery.generation.Code
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.FetchPolicy
import io.exoquery.generation.TableGrouping
import io.exoquery.parseError
import io.exoquery.plugin.isClass
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.varargValues
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg

object Unlifter {

  context (CX.Scope)
  private fun orFail(expr: IrExpression): Nothing =
    parseError("Failed to unlift the construct", expr) // TODO need a MUCH BETTER error here

  context (CX.Scope)
  fun unliftString(expr: IrExpression): String =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.String) ?: parseError("Expected a constant string", expr)
        it.value as? String ?: parseError("Constant value was not a string", expr)
      }
      ?: orFail(expr)

  context (CX.Scope)
  fun unliftBoolean(expr: IrExpression): Boolean =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.Boolean) ?: parseError("Expected a constant boolean", expr)
        it.value as? Boolean ?: parseError("Constant value was not a boolean", expr)
      }
      ?: orFail(expr)

  context (CX.Scope)
  fun unliftStringIfNotNull(expr: IrExpression?) : String? =
    expr?.let { unliftString(it) }

  context (CX.Scope)
  fun DatabaseDriver.Companion.unlift(expr: IrExpression): DatabaseDriver =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.Postgres>()[Is()]).then { args ->
        DatabaseDriver.Postgres(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.Postgres.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.MySQL>()[Is()]).then { args ->
        DatabaseDriver.MySQL(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.MySQL.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.SQLite>()[Is()]).then { args ->
        DatabaseDriver.SQLite(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.SQLite.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.H2>()[Is()]).then { args ->
        DatabaseDriver.H2(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.H2.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.Oracle>()[Is()]).then { args ->
        DatabaseDriver.Oracle(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.Oracle.DefaultUrl)
      },
      case(Ir.ConstructorCallNullableN.of<DatabaseDriver.SqlServer>()[Is()]).then { args ->
        DatabaseDriver.SqlServer(unliftStringIfNotNull(args[0]) ?: DatabaseDriver.SqlServer.DefaultUrl)
      },
      case(Ir.ConstructorCall2.of<DatabaseDriver.Custom>()[Is(), Is()]).then { a, b ->
        DatabaseDriver.Custom(unliftString(a), unliftString(b))
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun TableGrouping.Companion.unlift(expr: IrExpression): TableGrouping =
    on(expr).match(
      case(Ir.GetObjectValue<TableGrouping.SchemaPerObject>()).then { TableGrouping.SchemaPerObject },
      case(Ir.GetObjectValue<TableGrouping.SchemaPerPackage>()).then { TableGrouping.SchemaPerPackage }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun FetchPolicy.Companion.unlift(expr: IrExpression): FetchPolicy =
    on(expr).match(
      case(Ir.GetObjectValue<FetchPolicy.OnVersionChange>()).then { FetchPolicy.OnVersionChange },
      case(Ir.GetObjectValue<FetchPolicy.Always>()).then { FetchPolicy.Always }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun NameParser.TypeOfLLM.Companion.unlift(expr: IrExpression): NameParser.TypeOfLLM =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<NameParser.TypeOfLLM.Ollama>()[Is()]).then { args ->
        NameParser.TypeOfLLM.Ollama(
          model = unliftStringIfNotNull(args[0]) ?: NameParser.TypeOfLLM.Ollama.DefaultModel,
          url = unliftStringIfNotNull(args[1]) ?: NameParser.TypeOfLLM.Ollama.DefaultUrl
        )
      },
      case(Ir.ConstructorCallNullableN.of<NameParser.TypeOfLLM.OpenAI>()[Is()]).then { args ->
        NameParser.TypeOfLLM.OpenAI(
          model = unliftStringIfNotNull(args[0]) ?: NameParser.TypeOfLLM.OpenAI.DefaultModel
        )
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun NameParser.UsingLLM.Companion.unlift(expr: IrExpression): NameParser.UsingLLM =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<NameParser.UsingLLM>()[Is()]).then { args ->
        NameParser.UsingLLM(
          type = NameParser.TypeOfLLM.unlift(args[0] ?: parseError("TypeOfLLM needs to be specified.", expr)),
          maxTablesPerCall = args[1]?.let { unliftString(it).toInt() } ?: NameParser.UsingLLM.DefaultMaxTablesPerCall,
          maxColumnsPerCall = args[2]?.let { unliftString(it).toInt() } ?: NameParser.UsingLLM.DefaultMaxColumnsPerCall,
          systemPromptTables = unliftStringIfNotNull(args[3]) ?: NameParser.UsingLLM.DefaultSystemPromptTables,
          systemPromptColumns = unliftStringIfNotNull(args[4]) ?: NameParser.UsingLLM.DefaultSystemPromptColumns,
          processor =
            if (args[5] == null)
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
    ) ?: orFail(expr)

  context (CX.Scope)
  fun NameParser.UsingRegex.Companion.unlift(expr: IrExpression): NameParser.UsingRegex =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<NameParser.UsingRegex>()[Is()]).then { args ->
        NameParser.UsingRegex(
          regex = args[0]?.let { unliftString(it) },
          replace = args[1]?.let { unliftString(it) },
          target = args[2]?.let { NameParser.Target.unlift(it) } ?: NameParser.UsingRegex.DefaultTarget,
        )
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun NameParser.Composite.Companion.unlift(expr: IrExpression): NameParser.Composite =
    on(expr).match(
      case(Ir.Call.FunctionMem2[Ir.GetObjectValue<NameParser.Composite.Companion>(), Is("invoke"), Is()]).then { _, (nameParser, otherNameParsers) ->
        val parser = NameParser.unlift(nameParser)
        val otherParsers = ((otherNameParsers as? IrVararg)?.let{ it.varargValues().map { NameParser.unlift(it) } } ?: emptyList())
        NameParser.Composite(parser, *otherParsers.toTypedArray())
      }
    ) ?: orFail(expr)

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
    ) ?: orFail(expr)

  context (CX.Scope)
  fun Code.DataClasses.Companion.unlift(expr: IrExpression): Code.DataClasses =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<Code.DataClasses>()[Is()]).then { args ->
        Code.DataClasses(
          args[0]?.let { unliftString(it) } ?: parseError("Expected a non-null string for codeVersion", expr),
          args[1]?.let { DatabaseDriver.unlift(it) } ?: parseError("Expected a non-null DatabaseDriver", expr),
          args[2]?.let { FetchPolicy.unlift(it) } ?: Code.DataClasses.DefaultFetchPolicy,
          args[3]?.let { unliftString(it) },
          args[4]?.let { unliftString(it) },
          args[5]?.let { unliftString(it) },
          args[6]?.let { unliftString(it) },
          args[7]?.let { unliftString(it) },
          args[8]?.let { unliftString(it) } ?: Code.DataClasses.DefaultPropertiesFile,
          args[9]?.let { NameParser.unlift(it) } ?: Code.DataClasses.DefaultNameParser,
          args[10]?.let { TableGrouping.unlift(it) } ?: Code.DataClasses.DefaultTableGrouping,
          args[11]?.let { unliftString(it) },
          args[12]?.let { unliftString(it) },
          args[13]?.let { unliftBoolean(it) } ?: Code.DataClasses.DefaultDryRun
        )
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun unliftCodeDataClasses(expr: IrExpression): Code.DataClasses =
    Code.DataClasses.unlift(expr)
}
