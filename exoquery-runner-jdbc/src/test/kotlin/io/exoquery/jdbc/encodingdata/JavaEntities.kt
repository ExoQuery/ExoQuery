package io.exoquery.jdbc.encodingdata

import io.exoquery.annotation.ExoValue
import io.exoquery.controller.ControllerAction
import io.kotest.matchers.bigdecimal.shouldBeEqualIgnoringScale
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.AssertionError
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Serializable
data class JavaTestEntity(
  @Contextual val bigDecimalMan: BigDecimal,
  @Contextual val javaUtilDateMan: Date,
  @Contextual val uuidMan: UUID,
  @Contextual val bigDecimalOpt: BigDecimal?,
  @Contextual val javaUtilDateOpt: Date?,
  @Contextual val uuidOpt: UUID?
) {
  companion object {
    val regular =
      JavaTestEntity(
        BigDecimal("1.1"),
        Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
        UUID.randomUUID(),
        BigDecimal("1.1"),
        Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
        UUID.randomUUID()
      )

    val empty =
      JavaTestEntity(
        BigDecimal.ZERO,
        Date(0),
        UUID(0, 0),
        null,
        null,
        null
      )
  }
}

fun verify(e: JavaTestEntity, expected: JavaTestEntity) {
  fun catchRewrapAssert(msg: String, assertFun: () -> Unit) =
    try { assertFun() } catch (e: AssertionError) {
      throw AssertionError(msg, e)
    }

  catchRewrapAssert("Error Comparing: bigDecimalMan") { e.bigDecimalMan shouldBeEqualIgnoringScale expected.bigDecimalMan }
  catchRewrapAssert("Error Comparing: javaUtilDateMan") { e.javaUtilDateMan shouldBeEqual expected.javaUtilDateMan }
  catchRewrapAssert("Error Comparing: uuidMan") { e.uuidMan shouldBeEqual expected.uuidMan }
  catchRewrapAssert("Error Comparing: bigDecimalOpt") { e.bigDecimalOpt shouldBeEqualIgnoringScaleNullable expected.bigDecimalOpt }
  catchRewrapAssert("Error Comparing: javaUtilDateOpt") { e.javaUtilDateOpt shouldBeEqualNullable expected.javaUtilDateOpt }
  catchRewrapAssert("Error Comparing: uuidOpt") { e.uuidOpt shouldBeEqualNullable expected.uuidOpt }
}

public infix fun BigDecimal?.shouldBeEqualIgnoringScaleNullable(expected: BigDecimal?) =
  if (this == null && expected == null) Unit
  else if (this == null || expected == null) assertEquals(this, expected) // i.e. will always be false
  else this.shouldBeEqualIgnoringScale(expected) // otherwise they are both not null and we compare by scale

public infix fun <A> A?.shouldBeEqualNullable(expected: A?) =
  assertEquals(expected, this)
