package io.exoquery.lang

import io.exoquery.xr.XRType

sealed interface QueryLevel {
  val isTop: Boolean
  fun withoutTopQuat() =
    when (this) {
      is QueryLevel.Top -> QueryLevel.TopUnwrapped
      QueryLevel.TopUnwrapped -> QueryLevel.TopUnwrapped
      QueryLevel.Inner -> QueryLevel.Inner
    }

  /** Top-level externally-facing query */
  data class Top(val topLevelQuat: XRType) : QueryLevel {
    override val isTop = true
  }

  /**
   * Top-level query that is not externally facing e.g. it's an unwrapped case
   * class AST
   */
  object TopUnwrapped : QueryLevel {
    override val isTop = true
  }

  /** Not a Top-level query */
  object Inner : QueryLevel {
    override val isTop = false
  }
}
