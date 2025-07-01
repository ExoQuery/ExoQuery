package io.exoquery.plugin.trees

import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.prepareForPrinting
import io.exoquery.plugin.transform.prepareForPrintingAdHoc
import io.exoquery.pprint.PPrinter
import io.exoquery.pprint.PPrinterConfig
import io.exoquery.pprint.Tree
import io.exoquery.unpackQuery
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class CaptureOwnerPrint(config: PPrinterConfig = PPrinterConfig(), val scope: CX.Scope? = null): PPrinter(config) {
  open override fun treeify(x: Any?, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when {
      x is IrElement -> {
        Tree.Literal(x.dumpKotlinLike().prepareForPrintingAdHoc().replace("\n", "; "))
      }
      x is SqlQueryExpr.Uprootable -> Tree.Literal(unpackQuery(x.packedXR).show())

      //x is IrElement -> Tree.Literal(x.dumpKotlinLike().replace("\n", "; "))
      //x is IrElement -> Tree.Literal(x.dumpSimple(false)) // doing a color-print within this outer context (which also yields a color-print) causes ascii overflow (i.e. IllegalArgumentException: end:375 must be less than or equal to length:355)
      else -> super.treeify(x, elementName, escapeUnicode, showFieldNames)
    }
}
