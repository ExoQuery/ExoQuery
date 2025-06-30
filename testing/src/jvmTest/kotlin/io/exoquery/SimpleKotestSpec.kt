package io.exoquery

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SimpleKotestSpec : FunSpec({
    test("a simple test") {
        println("[DEBUG_LOG] Running simple test")
        1 + 1 shouldBe 2
    }

    test("another simple test") {
        println("[DEBUG_LOG] Running another simple test")
        2 * 2 shouldBe 4
    }
})
