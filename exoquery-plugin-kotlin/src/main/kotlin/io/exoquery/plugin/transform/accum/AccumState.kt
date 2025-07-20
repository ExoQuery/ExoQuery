package io.exoquery.plugin.transform

import org.jetbrains.kotlin.ir.declarations.IrFile

sealed interface AccumState<out Content> {
  fun isEmpty(): Boolean
  fun nonEmpty(): Boolean = !isEmpty()

  data object Empty : AccumState<Nothing> {
    override fun isEmpty(): Boolean = true
  }

  data class RealFile<Content>(val file: IrFile, val items: MutableList<Content> = mutableListOf()) : AccumState<Content> {
    override fun isEmpty(): Boolean = items.isEmpty()

    fun addItem(printableQuery: Content) {
      items.add(printableQuery)
    }
  }

  sealed interface PathBehavior {
    data object IncludePaths : PathBehavior
    data object NoIncludePaths : PathBehavior
  }

  sealed interface LabelBehavior {
    data object IncludeOnlyLabeled : LabelBehavior
    data object IncludeAll : LabelBehavior
  }
}
