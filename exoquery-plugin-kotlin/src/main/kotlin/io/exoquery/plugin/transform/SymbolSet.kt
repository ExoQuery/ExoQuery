package io.exoquery.plugin.transform

import io.exoquery.plugin.extensionParam
import io.exoquery.plugin.logging.Location
import io.exoquery.plugin.regularParams
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol

data class PrintableQuery(val query: String, val xr: XR, val location: Location, val queryOutput: XR.ClassId, val label: String? = null) {
  override fun toString(): String = query
}
