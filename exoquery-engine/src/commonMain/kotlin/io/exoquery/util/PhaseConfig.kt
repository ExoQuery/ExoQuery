package io.exoquery.util

import kotlin.reflect.KClass

sealed interface DisableablePhase {
  val value: String

  object ApplyMap : DisableablePhase {
    override val value = "applymap"
  }

  object CrunchFlatJoins : DisableablePhase {
    override val value = "crunch"
  }

  object SymbolicReduction : DisableablePhase {
    override val value = "symbolic"
  }

  object PushAlias : DisableablePhase {
    override val value = "pushalias"
  }

  object Dealias : DisableablePhase {
    override val value = "dealias"
  }

  companion object {
    fun fromClassStr(classStr: String) =
      values.find { it::class.simpleName == classStr ?: false }

    val values: List<DisableablePhase> = listOf(
      ApplyMap,
      CrunchFlatJoins,
      SymbolicReduction,
      PushAlias,
      Dealias
    )
  }
}


data class PhaseConfig(val disabledPhases: List<DisableablePhase>) {
  companion object {
    val empty = PhaseConfig(listOf())
  }

  fun isDisabled(phase: DisableablePhase): Boolean =
    disabledPhases.contains(phase)
}
