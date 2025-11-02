package io.exoquery.plugin.printing

import io.exoquery.parseError
import io.exoquery.xr.XRType
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

// @formatter:off

object Messages {

fun NoDispatchRecieverFoundForSqlVarCall(sqlVar: IrExpression) =
"""
No dispatch-reciver was found for the SqlVariable:
${sqlVar.dumpKotlinLike()}

This should be impossible.
""".trimIndent()

fun NotProductTypeParsedFromType(xrType: XRType, type: IrType) =
"""
A XRType.Product could not be parsed from the type:
${type.dumpKotlinLike()}

Instead we parsed the type: ${xrType}
""".trimIndent()

}
// @formatter:on
