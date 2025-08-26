package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.generation.encode
import io.exoquery.parseError
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.regularArgs
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.Unlifter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import io.exoquery.plugin.trees.ExtractorsDomain.Call.CaptureGenerate.CallType
import io.exoquery.plugin.trees.PT
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irUnit

class TransformCaptureCodegenRead(val codegenAccum: FileCodegenAccum) : Transformer<IrCall>() {

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureGenerate[Is(), Is()].matchesAny(expression)

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun transform(expression: IrCall): IrExpression {
    val (caseClassCodegen, callType) =
      on(expression).match(
        case(ExtractorsDomain.Call.CaptureGenerate[Is(), Is()]).then { codeDataClassConstructor, callType ->
          val arg = codeDataClassConstructor.regularArgs[0]
          Unlifter.unliftCodeEntities(arg ?: parseError("case class codegen argument was null")) to callType
        }
      ) ?: parseError(
        Messages.UnexpectedCodegenCall,
        expression
      )

    if (callType != CallType.JustReturn)
      codegenAccum.addItem(caseClassCodegen)

    return when (callType) {
      CallType.Gen -> {
        // If it's just a generation call we don't return anything (it's generated at compile time so just as a side effect) so just return Unit
        builder.irUnit()
      }
      CallType.GenAndReturn, CallType.JustReturn -> {
        // if it's a generateAndReturn call, we need to return the generated code. Pack it up and prep it for unpacking on the client side
        val packedCodeEntities = caseClassCodegen.encode()
        call(PT.io_exoquery_unpackCodeEntities)(builder.irString(packedCodeEntities))
      }
    }
  }
}
