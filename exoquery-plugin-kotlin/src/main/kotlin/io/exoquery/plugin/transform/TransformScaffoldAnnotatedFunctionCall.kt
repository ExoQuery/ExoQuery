package io.exoquery.plugin.transform

import io.exoquery.annotation.CapturedFunction
import io.exoquery.annotation.CapturedFunctionSketch
import io.exoquery.annotation.paramKindParsed
import io.exoquery.fansi.nullableAsList
import io.exoquery.parseError
import io.exoquery.plugin.findExtensionArgBasedOnParamKinds
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.ownerFunction
import io.exoquery.plugin.refinedStableIdentifier
import io.exoquery.plugin.regularArgsWithParamKinds
import io.exoquery.plugin.safeName
import io.exoquery.plugin.source
import io.exoquery.plugin.stableIdentifier
import io.exoquery.plugin.trees.CrossFile
import io.exoquery.plugin.trees.OwnerChain
import io.exoquery.plugin.trees.PT.io_exoquery_util_scaffoldCapFunctionQuery
import io.exoquery.plugin.trees.extractCapturedFunctionParamSketches
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


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
    val paramSketches =
      // if we can get the param-kinds then return it, otherwise try to parse the parent captured-function
      call.extractCapturedFunctionParamSketches() ?: run {
        val owner = call.symbol.owner

        // =========== 9/7/2025 Since we're replacing the output with just a IrExpression (not something with a body) we don't care about the scope ===========
        // Need to keep track of the previous scope which is something like:
        // 'public final fun people (filter: kotlin.String): @[Captured] io.exoquery.SqlQuery<io.exoquery.MyPerson> declared in io.exoquery.MyCaptureAheadObject'
        // val prevScope = ((call.symbol.owner.body!!.statements[0]) as IrReturn).returnTargetSymbol


        val transformer = TransformAnnotatedFunction(superTransformer)
        //logger.error("------------------ Going Into Owner (${sourceLabel}) ------------------\n${owner.dumpKotlinLike().prepareForPrintingAdHoc()}")
        transformer.transform(owner)
        //logger.error("------------------ Completed Owner (${sourceLabel}) ------------------\n${owner.dumpKotlinLike().prepareForPrintingAdHoc()}")

        // Then set back the scope since now it will be the wrong one (based on the current scope of the transformer)
        // which would be: 'local final fun <anonymous> ($this$select: io.exoquery.SelectClauseCapturedBlock): io.exoquery.MyPerson declared in io.exoquery.q'
        // This would compile just fine but at runtime yield the error: Exception in thread "main" java.lang.ClassFormatError: Illegal method name "<anonymous>" in class io/exoquery/MyCaptureAheadObject

        // =========== 9/7/2025 Since we're replacing the output with just a IrExpression (not something with a body) we don't care about the scope ===========
        //((call.symbol.owner.body!!.statements[0]) as IrReturn).returnTargetSymbol = prevScope

        // TODO Maybe we should do this automatically in all situations in the TransformAnnotatedFunction

        // Transform the owner, then try getting the catpuredFunctionParamKinds again
        call.extractCapturedFunctionParamSketches()
      } ?: run {
        // Otherwise we attempted to transform the parent captured function but could not to throw an error
        parseError(Messages.cannotUseForwardReferenceCapturedFunction(call.symbol.safeName, call.symbol.owner.source() ?: call.symbol.owner.dumpKotlinLike()), call)
      }

    val paramKinds = paramSketches.map { it.paramKindParsed() ?: parseError("Could not parse the param-kind '${it}' in captured-function `${call.symbol.safeName}`", call) }

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

    val extensionReceiverArg = call.findExtensionArgBasedOnParamKinds(paramKinds)

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

    fun makeTag() =
      if (CrossFile.isCrossFile(zeroizedCallRaw.ownerFunction)) {
        run {
          val presentTag = run {
            OwnerChain.ContainerType.identify(zeroizedCallRaw.ownerFunction.returnType)?.let { containerType ->
              if (CrossFile.hasUprootableInStoreIndependent(zeroizedCallRaw.ownerFunction, containerType)) "present" else "absent"
            } ?: "unknown-type"
          }
          " (cross-file/${presentTag})"
        }
      } else ""

    val zeroizedCall = run {
      //TransformProjectCapture(superTransformer).transform(zeroizedCallRaw)
      val ownershipChain = TransformProjectCapture2.buildOwnerChain(zeroizedCallRaw)

      if (ownershipChain.root is OwnerChain.Root.Unknown)
        parseError("Invalid ownership chain constructed when attempting to scaffold the${makeTag()} captured-function call of ${call.symbol.safeName} (source was Unknown).\n----------- Ownership trace was the following: -----------\n${ownershipChain.show()}\n----------- Stable Identifier: -----------\n${zeroizedCallRaw.ownerFunction.refinedStableIdentifier()}", zeroizedCallRaw, true)
      if (ownershipChain.root is OwnerChain.Root.Virgin)
        parseError(
          "Invalid ownership chain constructed when attempting to scaffold the${makeTag()} captured-function call of ${call.symbol.safeName} (source was a Unprocessed Captured Expression).\n----------- Ownership trace was the following: -----------\n${ownershipChain.show()}\n----------- Stable Identifier: -----------\n${zeroizedCallRaw.ownerFunction.refinedStableIdentifier()}",
          zeroizedCallRaw,
          true
        )

      when (val rootNode = ownershipChain.root) {
        is OwnerChain.Root.SourcedPreviouslyCompiledFunction -> {
          // If we're in the case where the captured function was compiled in a previous compilation-unit (i.e. cross-file) then the TransformAnnotatedFunction
          // phase has not been applied to it hence it's inputs have not been removed (a.k.a. sterilized yet). This is a function of the fact that
          // the backend IR is ephemeral and each time gets reconstructed directly from the frontend-IR. That means that if the captured function is coming from an IrExternalPackageFragment
          // then it has not been transformed yet. So we need to sterilize the inputs here. This is called "Ad-Hoc serialization" because we are sterilizing the captured-function not
          // when we visit it in a transformer but rather when we see a call-site of it.
          // (Note that if the TransformScaffoldAnnotatedFunctionCall is being called on multiple call-sites of the captured-function then we need to make sure that we do not
          // ad-hoc serialize it multiple times hence the check for wasSterilizedAdHoc)
          val wasSterilizedAdHoc = rootNode.origin.wasSterilizedAdHoc()
          val hasSketch = rootNode.origin.hasAnnotation<CapturedFunctionSketch>()
          when {
            !wasSterilizedAdHoc && hasSketch ->
              // In some odd incremental-recompile cases the input-sketch has already been added but the function arguments have not been removed (`sterilizeInputs` is done in the TransformAnnotatedFunction phase)
              // then we remove them here i.e. sterilize the function. We add the annotation @WasSterilizedAdHoc to indicate that we processed the function this way.
              // This is indeed an odd situations because nowhere in the TransformAnnotatedFunction phase do we add the sketch without also sterilizing the inputs. My guess is that during some incremental recompile
              // situations, the TransformAnnotatedFunction is called on a function adding the CapturedFunctionSketch but the original arguments are being read in from the IR.
              TransformAnnotatedFunction.sterilizeInputs(rootNode.origin)
            !wasSterilizedAdHoc -> {
              TransformAnnotatedFunction.addInputSketch(rootNode.origin)
              TransformAnnotatedFunction.sterilizeInputs(rootNode.origin)
            }
            else -> {} // No need to do anything, it was already sterilized and has a sketch
          }
          rootNode.origin.markSterilizedAdHoc()
        }
        else -> {}
      }

      // Continue to process the rest of the chain i.e. all child calls that happened to the annotated function
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
