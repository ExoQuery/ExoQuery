package io.exoquery.plugin.trees

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

object IrTraversals {
  class CollectGetValue : IrVisitorVoid() {
    val values = mutableListOf<IrGetValue>()

    // Need to override the visitor at the visitElement function because the visitor bubbles up instead of down
    // i.e. if a generality of IrGetElement is used (e.g. the actual instance of the thing that is a IrGetValue)
    // is stored in an IrExpression variable etc... then the visitGetValue case will not be called.
    override fun visitElement(element: IrElement) {
      if (element is IrGetValue)
        values.add(element)
      else
        element.acceptChildren(this, null)
    }
  }

  fun collectGetValue(expression: IrExpression): List<IrGetValue> {
    val collector = CollectGetValue()
    collector.visitElement(expression)
    return collector.values
  }

  class CollectGetValueAndCalls : IrVisitorVoid() {
    val getValues = mutableListOf<IrGetValue>()
    val getIrCalls = mutableListOf<IrCall>()

    // Need to override the visitor at the visitElement function because the visitor bubbles up instead of down
    // i.e. if a generality of IrGetElement is used (e.g. the actual instance of the thing that is a IrGetValue)
    // is stored in an IrExpression variable etc... then the visitGetValue case will not be called.
    override fun visitElement(element: IrElement) {
      if (element is IrGetValue)
        getValues.add(element)
      else {
        if (element is IrCall) getIrCalls.add(element)
        element.acceptChildren(this, null)
      }
    }
  }

  fun collectGetValuesAndCalls(expression: IrExpression): Pair<List<IrGetValue>, List<IrCall>> {
    val collector = CollectGetValueAndCalls()
    collector.visitElement(expression)
    return collector.getValues to collector.getIrCalls
  }


}
