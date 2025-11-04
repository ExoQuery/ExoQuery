package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt
import io.exoquery.printing.pl

object PluginCompileReqGoldenDynamic: MessageSpecFile {
  override val messages = mapOf<String, GoldenResult>(
    "should report error parsing external function" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the expression
      It looks like you are attempting to call the external function `externalFunction` in a captured block
      only functions specifically made to be interpreted by the ExoQuery system are allowed inside
      of captured blocks. If you are trying to use a runtime-value of a primitive, you need to bring
      it into the captured block by using `param(myCall(...))`. 
      
      For example:
      fun myCall(value: String): Int = runtimeComputation(value)
      val myQuery = sql { Table<Person>().filter { p -> p.id == param(myCall("someValue")) }
      
      If this function is supposed to do something that becomes part of the generated SQL you need to 
      annotate it as @SqlFragment and make it return an SqlExpression<T> (or SqlQuery<T>) value. 
      The use the use the `use` function to splice the value.
      
      For example:
      @SqlFragment
      fun myCall(value: String): SqlExpression<String> = sql.expression { value + "_suffix" }
      val myQuery = sql { Table<Person>().filter { p -> p.name == myCall("someValue").use }.
      ------------ Source ------------
      externalFunction(p.name)
      ------------ Raw Expression ------------
      externalFunction(name = p.<get-name>())
      
      """
    ),
    "should report error parsing external function - returning query" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the Query.
      It looks like you are attempting to call the external function `withOrder` in a captured block
      only functions specifically made to be interpreted by the ExoQuery system are allowed inside
      of captured blocks. If this function does something that is supposed to become part of the generated SQL
      you need to annotate it as @SqlFragment and make it return a SqlQuery<T> (or SqlExpression<T>) value.
      
      For example:
      @SqlFragment fun withOrders(customers: SqlQuery<Customer>) = sql.select {
        val c = from(customers)
        val o = join(Table<Order>()) { o -> c.id == o.customerId }
        c to o
      }
      val myQuery = sql { withOrders(Table<Customer>()) }
      
      Where Customer and Order are:
      data class Customer(val id: Int, val name: String)
      data class Order(val id: Int, val customerId: Int,  val total: Double).
      ------------ Source ------------
      withOrder(Table<Customer>())
      ------------ Raw Expression ------------
      withOrder(customers = ${'$'}this${'$'}sql.Table<Customer>())
      
      """
    ),
    "should report error parsing external function - error details enabled" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the expression
      It looks like you are attempting to call the external function `externalFunction` in a captured block
      only functions specifically made to be interpreted by the ExoQuery system are allowed inside
      of captured blocks. If you are trying to use a runtime-value of a primitive, you need to bring
      it into the captured block by using `param(myCall(...))`. 
      
      For example:
      fun myCall(value: String): Int = runtimeComputation(value)
      val myQuery = sql { Table<Person>().filter { p -> p.id == param(myCall("someValue")) }
      
      If this function is supposed to do something that becomes part of the generated SQL you need to 
      annotate it as @SqlFragment and make it return an SqlExpression<T> (or SqlQuery<T>) value. 
      The use the use the `use` function to splice the value.
      
      For example:
      @SqlFragment
      fun myCall(value: String): SqlExpression<String> = sql.expression { value + "_suffix" }
      val myQuery = sql { Table<Person>().filter { p -> p.name == myCall("someValue").use }.
      ------------ Source ------------
      externalFunction(p.name)
      ------------ Raw Expression ------------
      externalFunction(name = p.<get-name>())
      ------------ Raw Expression Tree ------------
      [IrCall] public final fun externalFunction (name: kotlin.String): kotlin.String declared in sample
        name: [IrCall] public final fun <get-name> (): kotlin.String declared in sample.MyPerson
          <this>: [IrGetValue] 'p: sample.MyPerson declared in sample.run.<anonymous>.<anonymous>' type=sample.MyPerson origin=null
      
      ----------------- Stack Trace: -----------------
      [Excluding 10 lines]
      ... (truncated)
      
      """
    ),
    "should report error parsing external function - error details enabled, custom stack count" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the expression
      It looks like you are attempting to call the external function `externalFunction` in a captured block
      only functions specifically made to be interpreted by the ExoQuery system are allowed inside
      of captured blocks. If you are trying to use a runtime-value of a primitive, you need to bring
      it into the captured block by using `param(myCall(...))`. 
      
      For example:
      fun myCall(value: String): Int = runtimeComputation(value)
      val myQuery = sql { Table<Person>().filter { p -> p.id == param(myCall("someValue")) }
      
      If this function is supposed to do something that becomes part of the generated SQL you need to 
      annotate it as @SqlFragment and make it return an SqlExpression<T> (or SqlQuery<T>) value. 
      The use the use the `use` function to splice the value.
      
      For example:
      @SqlFragment
      fun myCall(value: String): SqlExpression<String> = sql.expression { value + "_suffix" }
      val myQuery = sql { Table<Person>().filter { p -> p.name == myCall("someValue").use }.
      ------------ Source ------------
      externalFunction(p.name)
      ------------ Raw Expression ------------
      externalFunction(name = p.<get-name>())
      ------------ Raw Expression Tree ------------
      [IrCall] public final fun externalFunction (name: kotlin.String): kotlin.String declared in sample
        name: [IrCall] public final fun <get-name> (): kotlin.String declared in sample.MyPerson
          <this>: [IrGetValue] 'p: sample.MyPerson declared in sample.run.<anonymous>.<anonymous>' type=sample.MyPerson origin=null
      
      ----------------- Stack Trace: -----------------
      [Excluding 5 lines]
      ... (truncated)
      
      """
    ),
  )
}
