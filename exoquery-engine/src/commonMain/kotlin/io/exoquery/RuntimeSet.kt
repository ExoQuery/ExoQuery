package io.exoquery

import io.exoquery.annotation.ExoInternal

// Create a wrapper class for runtimes for easy lifting/unlifting
@ExoInternal
data class RuntimeSet(val runtimes: List<Pair<BID, ContainerOfXR>>) {
  companion object {
    // When this container is spliced, the ExprModel will look for this actual value in the Ir to tell if there are
    // runtime XR Containers (hence the dynamic-path needs to be followed).
    val Empty = RuntimeSet(emptyList())
    fun of(vararg runtimes: Pair<BID, ContainerOfXR>) = RuntimeSet(runtimes.toList())
  }

  operator fun plus(other: RuntimeSet): RuntimeSet = RuntimeSet(runtimes + other.runtimes)
}
