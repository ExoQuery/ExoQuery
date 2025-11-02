package io.exoquery

import io.exoquery.printing.GoldenResult

interface MessageSpecFile {
  val messages: Map<String, GoldenResult> get() = emptyMap()

  companion object {
    val Empty = object: MessageSpecFile {}
  }
}
