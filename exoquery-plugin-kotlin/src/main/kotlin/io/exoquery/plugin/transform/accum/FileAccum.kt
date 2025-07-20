package io.exoquery.plugin.transform

import org.jetbrains.kotlin.ir.declarations.IrFile

data class FileAccum<Content>(private val state: AccumState<Content>) {
  companion object {
    fun <Content> empty(): FileAccum<Content> = FileAccum<Content>(AccumState.Empty)
    fun <Content> emptyWithFile(file: IrFile): FileAccum<Content> = FileAccum<Content>(AccumState.RealFile<Content>(file))
  }

  fun hasQueries(): Boolean =
    when (val fileQueryAccum = this.state) {
      is AccumState.Empty ->
        false
      is AccumState.RealFile<*> ->
        fileQueryAccum.items.isNotEmpty()
    }

  fun currentQueries(): List<Content> =
    when (val fileQueryAccum = this.state) {
      is AccumState.Empty ->
        emptyList<Content>()
      is AccumState.RealFile<Content> ->
        fileQueryAccum.items
    }

  fun addItem(item: Content) {
    when (val fileQueryAccum = this.state) {
      is AccumState.Empty ->
        error("------- Cannot add item to empty file: -------\n${item}")
      is AccumState.RealFile<Content> ->
        fileQueryAccum.addItem(item)
    }
  }
}
