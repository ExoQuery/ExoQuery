package io.exoquery.xr

import io.decomat.HasProductClass
import io.decomat.*
import io.decomat.productComponentsOf
import io.exoquery.xr.XR.FunctionApply
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import io.decomat.Matchable as Mat
import io.decomat.Component as Slot
import io.decomat.MiddleComponent as MSlot
import io.decomat.ConstructorComponent as CS
import io.decomat.HasProductClass as PC
import io.decomat.productComponentsOf as productOf

@Serializable
@Mat
data class MyTest(@Slot val name: String, @Slot val age: Int): PC<MyTest> {
  @Transient
  override val productComponents = productOf(this, name, age)

  override fun toString(): String = "MyTest(name=$name, age=$age)"

  companion object {
  }
}

//fun foo() {
//  val x = MyTest("John", 30)
//
//  on(x).match(
//    MyTest.get
//  )
//}
