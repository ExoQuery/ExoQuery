package io.exoquery.plugin.printing

import org.jetbrains.annotations.Contract
import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

interface Segment {
  fun getStartOffset(): Int
  fun getEndOffset(): Int
}


open class TextRangeSimple protected constructor(private val myStartOffset: Int, private val myEndOffset: Int, checkForProperTextRange: Boolean): Segment, Serializable {

  constructor(startOffset: Int, endOffset: Int): this(startOffset, endOffset, true)

  init {
    if (checkForProperTextRange) {
      assertProperRange(this)
    }
  }

  override fun getStartOffset(): Int {
    return this.myStartOffset
  }

  override fun getEndOffset(): Int {
    return this.myEndOffset
  }

  val length: Int
    get() = this.myEndOffset - this.myStartOffset

  override fun equals(obj: Any?): Boolean {
    if (obj !is TextRangeSimple) {
      return false
    } else {
      val range = obj
      return this.myStartOffset == range.myStartOffset && this.myEndOffset == range.myEndOffset
    }
  }

  override fun hashCode(): Int {
    return this.myStartOffset + this.myEndOffset
  }


  fun contains(range: TextRangeSimple): Boolean {
    return this.contains(range as Segment)
  }


  fun contains(range: Segment): Boolean {
    return this.containsRange(range.getStartOffset(), range.getEndOffset())
  }


  fun containsRange(startOffset: Int, endOffset: Int): Boolean {
    return this.getStartOffset() <= startOffset && endOffset <= this.getEndOffset()
  }


  fun containsOffset(offset: Int): Boolean {
    return this.myStartOffset <= offset && offset <= this.myEndOffset
  }

  override fun toString(): String {
    return "(" + this.myStartOffset + "," + this.myEndOffset + ")"
  }


  fun contains(offset: Int): Boolean {
    return this.myStartOffset <= offset && offset < this.myEndOffset
  }


  fun substring(str: String): String {
    return str.substring(this.myStartOffset, this.myEndOffset)
  }


  fun subSequence(str: CharSequence): CharSequence {
    return str.subSequence(this.myStartOffset, this.myEndOffset)
  }


  fun cutOut(subRange: TextRangeSimple): TextRangeSimple {
    require(subRange.getStartOffset() <= this.length) { "SubRange: " + subRange + "; this=" + this }
    require(subRange.getEndOffset() <= this.length) { "SubRange: " + subRange + "; this=" + this }
    assertProperRange(subRange)
    return TextRangeSimple(this.myStartOffset + subRange.getStartOffset(), min(this.myEndOffset, this.myStartOffset + subRange.getEndOffset()))
  }


  open fun shiftRight(delta: Int): TextRangeSimple {
    return if (delta == 0) this else TextRangeSimple(this.myStartOffset + delta, this.myEndOffset + delta)
  }


  fun shiftLeft(delta: Int): TextRangeSimple {
    return if (delta == 0) this else TextRangeSimple(this.myStartOffset - delta, this.myEndOffset - delta)
  }


  open fun grown(lengthDelta: Int): TextRangeSimple {
    return if (lengthDelta == 0) this else from(this.myStartOffset, this.length + lengthDelta)
  }


  fun replace(original: String, replacement: String): String {
    val beginning = original.substring(0, this.getStartOffset())
    val ending = original.substring(this.getEndOffset())
    return beginning + replacement + ending
  }


  fun intersects(textRange: TextRangeSimple): Boolean {
    return this.intersects(textRange as Segment)
  }


  fun intersects(textRange: Segment): Boolean {
    return this.intersects(textRange.getStartOffset(), textRange.getEndOffset())
  }


  fun intersects(startOffset: Int, endOffset: Int): Boolean {
    return max(this.myStartOffset, startOffset) <= min(this.myEndOffset, endOffset)
  }


  fun intersectsStrict(textRange: TextRangeSimple): Boolean {
    return this.intersectsStrict(textRange.getStartOffset(), textRange.getEndOffset())
  }


  fun intersectsStrict(startOffset: Int, endOffset: Int): Boolean {
    return max(this.myStartOffset, startOffset) < min(this.myEndOffset, endOffset)
  }


  fun intersection(range: TextRangeSimple): TextRangeSimple? {
    if (this == range) {
      return this
    } else {
      val newStart = max(this.myStartOffset, range.getStartOffset())
      val newEnd = min(this.myEndOffset, range.getEndOffset())
      return if (isProperRange(newStart, newEnd)) TextRangeSimple(newStart, newEnd) else null
    }
  }

  @get:Contract(pure = true)
  val isEmpty: Boolean
    get() = this.myStartOffset >= this.myEndOffset


  fun union(textRange: TextRangeSimple): TextRangeSimple {
    return if (this == textRange) this else TextRangeSimple(min(this.myStartOffset, textRange.getStartOffset()), max(this.myEndOffset, textRange.getEndOffset()))
  }


  fun equalsToRange(startOffset: Int, endOffset: Int): Boolean {
    return startOffset == this.myStartOffset && endOffset == this.myEndOffset
  }

  companion object {
    private val serialVersionUID = -670091356599757430L
    val EMPTY_RANGE: TextRangeSimple = TextRangeSimple(0, 0)
    val EMPTY_ARRAY: Array<TextRangeSimple?> = arrayOfNulls<TextRangeSimple>(0)


    fun containsRange(outer: Segment, inner: Segment): Boolean {
      return outer.getStartOffset() <= inner.getStartOffset() && inner.getEndOffset() <= outer.getEndOffset()
    }


    fun from(offset: Int, length: Int): TextRangeSimple {
      return create(offset, offset + length)
    }


    fun create(startOffset: Int, endOffset: Int): TextRangeSimple {
      return TextRangeSimple(startOffset, endOffset)
    }


    fun create(segment: Segment): TextRangeSimple {
      return create(segment.getStartOffset(), segment.getEndOffset())
    }


    fun areSegmentsEqual(segment1: Segment, segment2: Segment): Boolean {
      return segment1.getStartOffset() == segment2.getStartOffset() && segment1.getEndOffset() == segment2.getEndOffset()
    }


    fun allOf(s: String): TextRangeSimple {
      return TextRangeSimple(0, s.length)
    }

    @JvmOverloads
    @Throws(AssertionError::class)
    fun assertProperRange(range: Segment, message: Any = "") {
      assertProperRange(range.getStartOffset(), range.getEndOffset(), message)
    }

    fun assertProperRange(startOffset: Int, endOffset: Int, message: Any) {
      require(isProperRange(startOffset, endOffset)) { "Invalid range specified: (" + startOffset + ", " + endOffset + "); " + message }
    }

    fun isProperRange(startOffset: Int, endOffset: Int): Boolean {
      return startOffset <= endOffset && startOffset >= 0
    }
  }
}
