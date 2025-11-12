package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.xr.XR

@ExoInternal
sealed interface ActionReturningKind {
  data object None : ActionReturningKind
  data class Keys(val columns: List<String>) : ActionReturningKind
  data object ClauseInQuery : ActionReturningKind

  companion object {
    fun fromActionXR(action: XR.Action): ActionReturningKind = when {
      action is XR.Returning && action.kind is XR.Returning.Kind.Expression ->
        ClauseInQuery
      action is XR.Returning && action.kind is XR.Returning.Kind.Keys ->
        // for properties e.g. name.first etc... we only care about the outermost property name because nested objects are ignored in the functioning of the actual SQL
        Keys(action.kind.keys.map { it.name })
      else ->
        None
    }
  }
}
