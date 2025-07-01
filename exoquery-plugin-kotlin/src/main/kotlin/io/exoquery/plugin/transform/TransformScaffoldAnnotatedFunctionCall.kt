package io.exoquery.plugin.transform

import io.decomat.Is
import io.exoquery.annotation.CapturedFunction
import io.exoquery.fansi.nullableAsList
import io.exoquery.parseError
import io.exoquery.plugin.extensionArgWithParamKinds
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.regularArgsWithParamKinds
import io.exoquery.plugin.safeName
import io.exoquery.plugin.source
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.OwnerChain
import io.exoquery.plugin.trees.PT.io_exoquery_util_scaffoldCapFunctionQuery
import io.exoquery.plugin.trees.SqlQueryExpr
import io.exoquery.plugin.trees.extractCapturedFunctionParamKinds
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.statements


context(CX.Scope, CX.Builder)
fun IrCall.zeroisedArgs(): IrCall {
  val call = this
  return with(builder) {
    val newCall = irCall(call.symbol)

    // The dispatch-receiver to a annotated function remains, the extension receiver gets dropped and used like a variable
    call.dispatchReceiver?.let { newCall.dispatchReceiver = it }

    newCall.typeArguments.withIndex().forEach { (i, tpe) -> typeArguments[i] = tpe }
    // no value arguments, should not have any since they are added to the scaffolding
    newCall
  }
}

context(CX.Scope, CX.Builder)
fun buildScaffolding(zeroisedCall: IrExpression, scaffoldType: IrType, originalArgs: List<IrExpression?>): IrExpression {
  var argsAsVararg = builder.irVararg(pluginCtx.symbols.any.defaultType.makeNullable(), originalArgs.map { it ?: builder.irNull() })
  val args = listOf(zeroisedCall) + argsAsVararg
  return callWithParamsAndOutput(io_exoquery_util_scaffoldCapFunctionQuery, listOf(scaffoldType), scaffoldType).invoke(*args.toTypedArray())
}


class TransformScaffoldAnnotatedFunctionCall(val superTransformer: VisitTransformExpressions, val sourceLabel: String) : Transformer<IrCall>() {
  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun matches(call: IrCall): Boolean =
    call.symbol.owner.hasAnnotation<CapturedFunction>()



  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun transform(call: IrCall): IrExpression {

    val paramKinds =
      // if we can get the param-kinds then return it, otherwise try to parse the parent captured-function
      call.extractCapturedFunctionParamKinds() ?: run {
        val owner = call.symbol.owner

        // Need to keep track of the previous scope which is something like:
        // 'public final fun people (filter: kotlin.String): @[Captured] io.exoquery.SqlQuery<io.exoquery.MyPerson> declared in io.exoquery.MyCaptureAheadObject'
        val prevScope = ((call.symbol.owner.body!!.statements[0]) as IrReturn).returnTargetSymbol
        val transformer = TransformAnnotatedFunction(superTransformer)
        //logger.error("------------------ Going Into Owner (${sourceLabel}) ------------------\n${owner.dumpKotlinLike().prepareForPrintingAdHoc()}")
        transformer.transform(owner)
        //logger.error("------------------ Completed Owner (${sourceLabel}) ------------------\n${owner.dumpKotlinLike().prepareForPrintingAdHoc()}")
        // Then set back the scope since now it will be the wrong one (based on the current scope of the transformer)
        // which would be: 'local final fun <anonymous> ($this$select: io.exoquery.SelectClauseCapturedBlock): io.exoquery.MyPerson declared in io.exoquery.q'
        // This would compile just fine but at runtime yield the error: Exception in thread "main" java.lang.ClassFormatError: Illegal method name "<anonymous>" in class io/exoquery/MyCaptureAheadObject
        ((call.symbol.owner.body!!.statements[0]) as IrReturn).returnTargetSymbol = prevScope

        // TODO Maybe we should do this automatically in all situations in the TransformAnnotatedFunction


        // Transform the owner, then try getting the catpuredFunctionParamKinds again
        call.extractCapturedFunctionParamKinds()
      } ?: run {
        // Otherwise we attempted to transform the parent captured function but could not to throw an error
        parseError(Messages.cannotUseForwardReferenceCapturedFunction(call.symbol.safeName, call.symbol.owner.source() ?: call.symbol.owner.dumpKotlinLike()), call)
      }


    // CANNOT invoke call.regularArgs here because we have blanked-out the arguments in the TransformAnnotationFunction stage. That is why we saved
    // the original arguments a CapturedFunctionParamKinds annotation that we then use to infer the original argument types.
    val originalRegularArgs = call.regularArgsWithParamKinds(paramKinds)

    //logger.warn("""
    //  |--------------------------- From Scaffolded call: ---------------------------
    //  |${call.dumpSimple()}
    //  |Param Kinds:
    //  |${paramKinds}
    //  |Args:
    //  |${(call.arguments.map { it?.dumpKotlinLike() })}
    //  |Args With Param Kinds:
    //  |${(call.arguments zip paramKinds).withIndex().map { (i, argAndParam) ->
    //    val (it, pk) = argAndParam
    //    "$i-${pk}) " +  it?.dumpKotlinLike() ?: "null" }.joinToString("\n") { "  $it" }
    //  }
    //""".trimMargin())

    val extensionReceiverArg = call.extensionArgWithParamKinds(paramKinds)

    val zeroizedCallRaw = call.zeroisedArgs()

    // Need to project the call to make it uprootable in the paresr in later stages.
    // For example
    // @CapturedFunction fun joes(people: SqlQuery<Person>) = capture { people.filter { p -> p.name == "Joe" } }
    // IN the TransformAnnotatedFunction turns into:
    //     fun joes() = SqlQuery(xr = XR.Function(args=[people], body=(people.filter { p -> p.name == "Joe" })))
    //   Or more simply put:
    //     fun joes() = SqlQuery(xr=(people)=>people.filter { p -> p.name == "Joe" })
    //   Or more simply put
    //     fun joes() = SqlQuery((people)=>people.filterJoe)
    // Then when the function is called like so:
    //      val drivingJoes = captured { joes(Table<Person>().filter { p -> p.age > 18 }) }
    //    Or more simply put:
    //      val drivingJoes = captured { joes(People.filterAge) }     // a.k.a. SqlQuery(xr=joes(People.filterAge))
    // The call `joes(People.filterAge)` first needs to be "zeroed out" into just `joes()`
    // Then, if `joes` it is uprootable it needs to be projected into:
    //     SqlQuery((people)=>people.filterJoe <- i.e. `joes`)      // #Projected Stage
    //   (Note that if joes() had any parameters this would be:)
    //     SqlQuery((people)=>people.filterJoe, params=joes().params)
    // Then when actually applied (later in the parser when processing drivingJoes) it will should like this:
    //     val drivingJoes = SqlQuery(Apply((people)=>people.filterJoe, listOf(People.filterAge)))   // with a possible parameter: params=joes().params
    // The only problem is, how do we still know at that point that the argument is People.filterAge?
    // we see above in #ProjectedStage that this information was dropped when we dropped the arguments of `joes`.
    // That means we need to introduce some "scaffolding to keep this information i.e. return the following:
    //   val drivingJoes = scaffoldCapFunctionQuery(SqlQuery((people)=>people.filterJoe <- i.e. `joes`), args=[People.filterAge])
    // Then finally in the parser when we process `drivingJoes` we can see that the argument was `People.filterAge` and create the expression:
    //   val drivingJoes = SqlQuery(Apply((people)=>people.filterJoe, listOf(People.filterAge)))

    // Also note that if joes() is not uprootable then it will be pluckable and the call will look like this:
    //   val drivingJoes = scaffoldCapFunctionQuery(joes, args=[People.filterAge])
    // And the parser will know that `joes` is a pluckable function and create the following:
    //   val drivingJoes = SqlQuery(Apply(Tag(123), listOf(People.filterAge)), runtimes={Tag(123)->joes})

    // Also note that if there is a receiver e.g. @CapturedFunction Person.joinAddress(street: String) = capture { flatJoin(Table<Address>().filter { street == ... }) { ... } }
    // and the it used as capture { val p = from(people); val a = from(a.joinAddresses("123 someplace"))
    // we need the scaffold to have the receiver-position element i.e. `a` to be the 1st argument
    // p.e. scaffoldCapFunctionQuery((Person, String) -> SqlQuery<Address> i.e. joinAddresses, args=[a, "123 someplace"])

    val zeroizedCall = run {
      //TransformProjectCapture(superTransformer).transform(zeroizedCallRaw)
      val ownershipChain = TransformProjectCapture2.buildOwnerChain(zeroizedCallRaw)

      val ownershipRoot = ownershipChain.root
      if (ownershipRoot == OwnerChain.Root.Unknown)
        parseError("Invalid ownership chain constructed when attempting to scaffold the captured-function call of ${call.symbol.safeName} (source was Unknown).\n----------- Ownership trace was the following: -----------\n${ownershipChain.show()}", zeroizedCallRaw)
      if (ownershipRoot is OwnerChain.Root.Virgin)
        parseError("Invalid ownership chain constructed when attempting to scaffold the captured-function call of ${call.symbol.safeName} (source was a Unprocessed Captured Expression).\n----------- Ownership trace was the following: -----------\n${ownershipChain.show()}", zeroizedCallRaw)

      TransformProjectCapture2.processOwnerChain(ownershipChain, superTransformer, true)
        ?: parseError("Could not process the ownership chain of the captured-function call of ${call.symbol.safeName}.\n----------- Ownership trace was the following: -----------\n${ownershipChain.show()}", zeroizedCallRaw)
    }


    // Note that the one case that we haven't considered aboive is where the argument to the function call i.e. People.filterAge
    // itself is a uprootable variable for example:
    //   val drivingPeople = captured { Table<Person>().filter { p -> p.age > 18 } }    // a.k.a. SqlQuery(xr=People.filterAge)
    //   @CapturedFunction fun joes(people: SqlQuery<Person>) = capture { people.filter { p -> p.name == "Joe" } }
    //   val drivingJoes = captured { joes(drivingPeople) }
    // IN that situation the scaffolding would look like so:
    //   val drivingJoes = scaffoldCapFunctionQuery(SqlQuery((people)=>people.filterJoe <- i.e. `joes`), args=[drivingPeople] <- NOTE Here!)
    // That means that the argument drivingPeople itself needs to be projected into a uprootable form.
    // So again, we call TransformProjectCapture on the arguments to the function call so that the scaffolding becomes:
    //   val drivingJoes = scaffoldCapFunctionQuery(SqlQuery((people)=>people.filterJoe <- i.e. `joes`), args=[SqlQuery(xr=People.filterAge)])
    //   (and if there are any parameters it it the argument becomes:
    //    args=[SqlQuery(xr=People.filterAge), params=drivingPeople.params])
    val projectedArgs = (extensionReceiverArg.nullableAsList() + originalRegularArgs).map { arg -> arg?.let { superTransformer.recurse(it) ?: it } }

    //val zeroizedCall = zeroizedCallRaw as IrCall

    val scaffoldedCall = buildScaffolding(zeroizedCall, call.type, projectedArgs)
    //throw IllegalStateException("------------------- Scaffolding ------------------\n${scaffoldedCall.dumpKotlinLike()}")

    //logger.error("------------------- SCAFFOLDED (${sourceLabel}) ------------------\n------------------- Original ---------------------------\n${call.dumpKotlinLike().prepareForPrintingAdHoc()}-----------------------------\n------------------------------- New --------------------------\n${scaffoldedCall.dumpKotlinLike().prepareForPrintingAdHoc()}\n--------------------Owner--------------------\n${call.symbol.owner.dumpKotlinLike().prepareForPrintingAdHoc()}\n----------------------from parent------------------:\n${call.symbol.owner.parent.dumpKotlinLike().prepareForPrintingAdHoc()}")

    return scaffoldedCall
  }

}
