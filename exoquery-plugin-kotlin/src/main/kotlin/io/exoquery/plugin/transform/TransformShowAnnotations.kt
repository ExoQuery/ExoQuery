package io.exoquery.plugin.transform

import io.exoquery.plugin.regularArgs
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName


class TransformShowAnnotations(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {


  private val fqn: String = "io.exoquery.showAnnotations"

  context(CX.Scope, CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    expression.symbol.owner.kotlinFqName.asString().let { it == fqn }

  // parent symbols are collected in the parent context
  context(CX.Scope, CX.Builder)
  override fun transform(expression: IrCall): IrExpression {
    val newExpression = superTransformer.recurse(expression)
    val sqlExpressionType = newExpression.type

    val capturedAnnot = sqlExpressionType.getAnnotation(FqName("io.exoquery.Captured")) ?: throw IllegalStateException(
      "Cannot find Captured annotation from type: ${sqlExpressionType.dumpKotlinLike()} in the expression:\n${newExpression.dumpKotlinLike()}\nCurrent Annotations are:\n${
        sqlExpressionType.annotations.map { it.dumpKotlinLike() }.joinToString("\n", "[", "]")
      }"
    )
    logger.warn("========= Found constructor annot with value: ${capturedAnnot.regularArgs[0]?.dumpKotlinLike()}\nThe full type is:\n${newExpression.type.dumpKotlinLike()}\nOf the expression:\n${newExpression.dumpKotlinLike()}")

    return newExpression
  }
}
