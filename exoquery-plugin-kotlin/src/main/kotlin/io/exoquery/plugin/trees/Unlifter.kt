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
  fun DatabaseDriver.Companion.unlift(expr: IrExpression): DatabaseDriver =
    on(expr).match(
      case(Ir.GetObjectValue<DatabaseDriver.Postgres>()).then { DatabaseDriver.Postgres },
      case(Ir.ConstructorCall1.of<DatabaseDriver.Custom>()[Is()]).then { DatabaseDriver.Custom(unliftString(it)) }
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
