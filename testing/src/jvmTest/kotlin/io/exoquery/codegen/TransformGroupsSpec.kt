package io.exoquery.codegen

import io.exoquery.codegen.util.transformGroups
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TransformGroupsSpec: FreeSpec({
  "should transform capturing groups" - {
    "with chars in between" {
      transformGroups("fooxBaryBazz", Regex("""(foo)x(Bar)y(Baz)z"""), { it.uppercase() }) shouldBe "FOOxBARyBAZz"
    }
    "with some chars in between some" {
      transformGroups("fooxBarBazz", Regex("""(foo)x(Bar)(Baz)z"""), { it.uppercase() }) shouldBe "FOOxBARBAZz"
    }
    "adjacent with prefix/suffix" {
      transformGroups("xfooBarBazx", Regex("""(foo)(Bar)(Baz)"""), { it.uppercase() }) shouldBe "xFOOBARBAZx"
    }
  }

})
