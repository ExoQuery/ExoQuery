package io.exoquery.plugin.transform

import io.decomat.*
import io.exoquery.plugin.trees.CrossFile
import io.exoquery.plugin.trees.Ir
import io.exoquery.plugin.trees.OwnerChain
import io.exoquery.plugin.trees.SqlActionExpr
import io.exoquery.plugin.trees.SqlBatchActionExpr
import io.exoquery.plugin.trees.SqlExpressionExpr
import io.exoquery.plugin.trees.SqlQueryExpr
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.statements

object VisitCrossFileFunctionHelper {
  context(scope: CX.Scope)
  fun putUprootableIfCrossFile(output: IrStatement) {
    if (output is IrFunction && output.isInline && output.body != null && output.body?.statements?.size == 1 && output.body?.statements?.first() is IrReturn) {
      val body = output.body!!
      val expr = (body.statements.first() as IrReturn).value
      on(expr).match(
        case(SqlQueryExpr.Uprootable[Is()]).then { uprootable ->
          scope.storedXRsScope.scoped {
            CrossFile.putUprootableIfCrossFile(output, OwnerChain.ContainerType.Query, uprootable)
          }
        },
        case(SqlExpressionExpr.Uprootable[Is()]).then { uprootable ->
          scope.storedXRsScope.scoped {
            CrossFile.putUprootableIfCrossFile(output, OwnerChain.ContainerType.Expr, uprootable)
          }
        },
        case(SqlActionExpr.Uprootable[Is()]).then { uprootable ->
          scope.storedXRsScope.scoped {
            CrossFile.putUprootableIfCrossFile(output, OwnerChain.ContainerType.Action, uprootable)
          }
        },
        case(SqlBatchActionExpr.Uprootable[Is()]).then { uprootable ->
          scope.storedXRsScope.scoped {
            CrossFile.putUprootableIfCrossFile(output, OwnerChain.ContainerType.ActionBatch, uprootable)
          }
        }
      )
    }
  }
}
