package io.exoquery.lang

import io.exoquery.BID
import io.exoquery.Param

data class Renderer(val showBids: Boolean = true, val showValues: Boolean = true, val truncateBids: Int? = 6) {
  private fun BID.trunc() =
    if (truncateBids != null)
      this.value.toString().takeLast(truncateBids)
    else
      this.value.toString()

  private fun Param<*>?.showParam() =
    this?.let { param -> param.showValue() } ?: "<NOT_FOUND>"

  operator fun invoke(bid: BID, value: Param<*>?, isRealized: Boolean): String {
    val string = when {
      showBids && showValues -> "{${bid.trunc()}:${if (!isRealized) "UNR" else value.showParam()}}"
      showBids -> "{${bid.trunc()}${if (value != null) "" else ":!!"}}"
      showValues -> value.showParam()
      else -> "?"
    }
    return string
  }
}
