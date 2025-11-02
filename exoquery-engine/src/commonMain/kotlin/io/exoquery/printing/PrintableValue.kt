package io.exoquery.printing

import io.exoquery.xr.XR

data class PrintableValue(val value: String, val type: Type, val queryOutputType: XR.ClassId, val label: String? = null, val params: List<Param> = emptyList()) {
  data class Param(val id: String, val value: String)

  sealed interface Type {
    val interpolatorPrefix: String

    data class SqlQuery(val xr: XR) : Type {
      override val interpolatorPrefix = "cr"
    }

    object SqlGeneric : Type {
      override val interpolatorPrefix = "cr"
    }

    object KotlinCode : Type {
      override val interpolatorPrefix = "kt"
    }

    object PlainText : Type {
      override val interpolatorPrefix = "pl"
    }
  }
}
