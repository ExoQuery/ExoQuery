package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt
import io.exoquery.printing.pl

object InvalidCapturedFunctionReqGoldenDynamic: MessageSpecFile {
  override val messages = mapOf<String, GoldenResult>(
    "error if captured function is not a SqlQuery<T> or SqlExpression<T>" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: The SqlFragment had the wrong kind of return type. A function annotated with @SqlFunction must return a single, single SqlQuery<T> or SqlExpression<T> instance
      ------------ Source ------------
      fun externalFunction(name: String) = name + "_suffix"
      ------------ Raw Expression ------------
      @SqlFragment
      fun externalFunction(name: String): String {
        return name.plus(other = "_suffix")
      }
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the expression
      ------------ Source ------------
      externalFunction(p.name)
      ------------ Raw Expression ------------
      externalFunction(name = p.<get-name>())
      
      """
    ),
    "error if captured function is not a SqlQuery<T> or SqlExpression<T> - with details" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: The SqlFragment had the wrong kind of return type. A function annotated with @SqlFunction must return a single, single SqlQuery<T> or SqlExpression<T> instance
      ------------ Source ------------
      fun externalFunction(name: String) = name + "_suffix"
      ------------ Raw Expression ------------
      @SqlFragment
      fun externalFunction(name: String): String {
        return name.plus(other = "_suffix")
      }
      ------------ Raw Expression Tree ------------
      [IrSimpleFunction] externalFunction ret:kotlin.String
        [IrValueParameter] kind:Regular name:name index:0 type:kotlin.String
        annotations:
          SqlFragment
        [IrBlockBody]
          [IrReturn] type=kotlin.Nothing from='public final fun externalFunction (name: kotlin.String): kotlin.String declared in <root>'
            [IrCall] public final fun plus (other: kotlin.Any?): kotlin.String [operator] declared in kotlin.String
              <this>: [IrGetValue] 'name: kotlin.String declared in <root>.externalFunction' type=kotlin.String origin=null
              other: [IrConst] String type=kotlin.String value="_"
      
      ----------------- Stack Trace: -----------------
      [Excluding 10 lines]
      ... (truncated)
      [ExoQuery] Could not understand an expression or query due to an error: The SqlFragment had the wrong kind of return type. A function annotated with @SqlFunction must return a single, single SqlQuery<T> or SqlExpression<T> instance
      ------------ Source ------------
      fun externalFunction(name: String) = name + "_suffix"
      ------------ Raw Expression ------------
      @SqlFragment
      fun externalFunction(name: String): String {
        return name.plus(other = "_suffix")
      }
      ------------ Raw Expression Tree ------------
      [IrSimpleFunction] externalFunction ret:kotlin.String
        [IrValueParameter] kind:Regular name:name index:0 type:kotlin.String
        annotations:
          SqlFragment
        [IrBlockBody]
          [IrReturn] type=kotlin.Nothing from='public final fun externalFunction (name: kotlin.String): kotlin.String declared in <root>'
            [IrCall] public final fun plus (other: kotlin.Any?): kotlin.String [operator] declared in kotlin.String
              <this>: [IrGetValue] 'name: kotlin.String declared in <root>.externalFunction' type=kotlin.String origin=null
              other: [IrConst] String type=kotlin.String value="_"
      
      ----------------- Stack Trace: -----------------
      [Excluding 10 lines]
      ... (truncated)
      [ExoQuery] Could not understand an expression or query due to an error: Could not parse the expression
      ------------ Source ------------
      externalFunction(p.name)
      ------------ Raw Expression ------------
      externalFunction(name = p.<get-name>())
      ------------ Raw Expression Tree ------------
      [IrCall] public final fun externalFunction (name: kotlin.String): kotlin.String declared in <root>
        name: [IrCall] public final fun <get-name> (): kotlin.String declared in <root>.MyPerson
          <this>: [IrGetValue] 'p: <root>.MyPerson declared in <root>.run.<anonymous>.<anonymous>' type=<root>.MyPerson origin=null
      
      ----------------- Stack Trace: -----------------
      [Excluding 10 lines]
      ... (truncated)
      
      """
    ),
  )
}
