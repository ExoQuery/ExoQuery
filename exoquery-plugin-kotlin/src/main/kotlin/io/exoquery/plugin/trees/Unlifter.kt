package io.exoquery.plugin.trees

import io.decomat.*
import io.exoquery.generation.Code
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.FetchPolicy
import io.exoquery.generation.PropertiesFile
import io.exoquery.generation.TableGrouping
import io.exoquery.parseError
import io.exoquery.plugin.transform.CX
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue

object Unlifter {

  context (CX.Scope)
  private fun orFail(expr: IrExpression): Nothing =
    parseError("Failed to unlift the construct", expr)

  context (CX.Scope)
  fun unliftString(expr: IrExpression): String =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.String) ?: parseError("Expected a constant string", expr)
        it.value as? String ?: parseError("Constant value was not a string", expr)
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
  fun PropertiesFile.Companion.unlift(expr: IrExpression): PropertiesFile =
    on(expr).match(
      case(Ir.GetObjectValue<PropertiesFile.Default>()).then { PropertiesFile.Default },
      case(Ir.ConstructorCall1.of<PropertiesFile.Custom>()[Is()]).then { PropertiesFile.Custom(unliftString(it)) }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun Code.DataClasses.Companion.unlift(expr: IrExpression): Code.DataClasses =
    on(expr).match(
      case(Ir.ConstructorCallNullableN.of<Code.DataClasses>()[Is()]).then { args ->
        Code.DataClasses(
          codeVersion = args[0]?.let { unliftString(it) } ?: parseError("Expected a non-null string for codeVersion", expr),
          driver = args[1]?.let { DatabaseDriver.unlift(it) } ?: parseError("Expected a non-null DatabaseDriver", expr),
          fetchPolicy = args[2]?.let { FetchPolicy.unlift(it) } ?: Code.DataClasses.DefaultFetchPolicy,
          packagePrefix = args[3]?.let { unliftString(it) },
          username = args[4]?.let { unliftString(it) },
          password = args[5]?.let { unliftString(it) },
          usernameEnvVar = args[6]?.let { unliftString(it) },
          passwordEnvVar = args[7]?.let { unliftString(it) },
          propertiesFile = args[8]?.let { PropertiesFile.unlift(it) },
          tableGrouping = args[9]?.let { TableGrouping.unlift(it) } ?: Code.DataClasses.DefaultTableGrouping,
        )
      }
    ) ?: orFail(expr)

  context (CX.Scope)
  fun unliftCodeDataClasses(expr: IrExpression): Code.DataClasses =
    Code.DataClasses.unlift(expr)
}
