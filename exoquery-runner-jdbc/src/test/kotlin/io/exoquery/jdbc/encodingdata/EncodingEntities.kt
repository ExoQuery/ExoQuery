package io.exoquery.jdbc.encodingdata

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals

@Serializable
data class EncodingTestEntity(
  val stringMan: String,
  val booleanMan: Boolean,
  val byteMan: Byte,
  val shortMan: Short,
  val intMan: Int,
  val longMan: Long,
  val floatMan: Float,
  val doubleMan: Double,
  val byteArrayMan: ByteArray,
  val customMan: String,
  val stringOpt: String?,
  val booleanOpt: Boolean?,
  val byteOpt: Byte?,
  val shortOpt: Short?,
  val intOpt: Int?,
  val longOpt: Long?,
  val floatOpt: Float?,
  val doubleOpt: Double?,
  val byteArrayOpt: ByteArray?,
  val customOpt: String?
) {
  companion object {
    val regular = EncodingTestEntity(
      stringMan = "hello",
      booleanMan = true,
      byteMan = 1,
      shortMan = 2,
      intMan = 3,
      longMan = 4L,
      floatMan = 5.5f,
      doubleMan = 6.6,
      byteArrayMan = byteArrayOf(1, 2, 3),
      customMan = "custom",
      stringOpt = "opt",
      booleanOpt = false,
      byteOpt = 7,
      shortOpt = 8,
      intOpt = 9,
      longOpt = 10L,
      floatOpt = 11.1f,
      doubleOpt = 12.2,
      byteArrayOpt = byteArrayOf(9, 8, 7),
      customOpt = "customOpt"
    )

    val empty = EncodingTestEntity(
      stringMan = "",
      booleanMan = false,
      byteMan = 0,
      shortMan = 0,
      intMan = 0,
      longMan = 0L,
      floatMan = 0.0f,
      doubleMan = 0.0,
      byteArrayMan = byteArrayOf(),
      customMan = "",
      stringOpt = null,
      booleanOpt = null,
      byteOpt = null,
      shortOpt = null,
      intOpt = null,
      longOpt = null,
      floatOpt = null,
      doubleOpt = null,
      byteArrayOpt = null,
      customOpt = null
    )
  }
}

fun verify(e: EncodingTestEntity, expected: EncodingTestEntity) {
  assertEquals(expected.stringMan, e.stringMan, "stringMan")
  assertEquals(expected.booleanMan, e.booleanMan, "booleanMan")
  assertEquals(expected.byteMan, e.byteMan, "byteMan")
  assertEquals(expected.shortMan, e.shortMan, "shortMan")
  assertEquals(expected.intMan, e.intMan, "intMan")
  assertEquals(expected.longMan, e.longMan, "longMan")
  assertEquals(expected.floatMan, e.floatMan, "floatMan")
  assertEquals(expected.doubleMan, e.doubleMan, "doubleMan")
  assertArrayEquals(expected.byteArrayMan, e.byteArrayMan, "byteArrayMan")
  assertEquals(expected.customMan, e.customMan, "customMan")

  assertEquals(expected.stringOpt, e.stringOpt, "stringOpt")
  assertEquals(expected.booleanOpt, e.booleanOpt, "booleanOpt")
  assertEquals(expected.byteOpt, e.byteOpt, "byteOpt")
  assertEquals(expected.shortOpt, e.shortOpt, "shortOpt")
  assertEquals(expected.intOpt, e.intOpt, "intOpt")
  assertEquals(expected.longOpt, e.longOpt, "longOpt")
  assertEquals(expected.floatOpt, e.floatOpt, "floatOpt")
  assertEquals(expected.doubleOpt, e.doubleOpt, "doubleOpt")
  when {
    e.byteArrayOpt == null && expected.byteArrayOpt == null -> Unit
    e.byteArrayOpt == null || expected.byteArrayOpt == null -> assertEquals(expected.byteArrayOpt, e.byteArrayOpt, "byteArrayOpt")
    else -> assertArrayEquals(expected.byteArrayOpt, e.byteArrayOpt, "byteArrayOpt")
  }
  assertEquals(expected.customOpt, e.customOpt, "customOpt")
}
