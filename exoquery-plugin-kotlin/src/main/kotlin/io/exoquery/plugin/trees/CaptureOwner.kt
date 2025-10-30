package io.exoquery.plugin.trees

import io.decomat.Is
import io.decomat.case
import io.decomat.caseEarly
import io.decomat.match
import io.exoquery.SqlAction
import io.exoquery.SqlBatchAction
import io.exoquery.SqlExpression
import io.exoquery.SqlQuery
import io.exoquery.annotation.SqlDynamic
import io.exoquery.annotation.SqlFragment
import io.exoquery.config.enableCrossFileStoreOrDefault
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.isClass
import io.exoquery.plugin.transform.CX
import io.exoquery.pprint.PPrinterConfig
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


// In situations where you've got
//   fun foo() = sql { Table<Person>() }
// and then later you use it in a sql expression
//   fun bar() = sql { foo().filter { p -> p.name == "Joe" } }
// The TransformCaptureQuery will change the `fun foo() = ...` part to
//   fun foo() = SqlQuery(...)
// Which will hopefully come first (we have a trick if it doesn't...)
// so in the happy case we'll come to the `fun bar() = ...` part later. Before we get there though
// we need to change the `foo()` invocation (in `foo().filter...`) to SqlQuery(..., foo().runtimes, foo().params)
// so that when we're doing TransformCaptureQuery on `bar()` the `foo()` part will already be a Uprootable and we'll know it's a static query.
// Note that this chain can have multiple steps, for example we could have:
//   fun foo() = sql { Table<Person>() }
//   fun bar() = sql { foo().filter { p -> p.name == "Joe" } }
//   fun baz() = sql { bar().filter { p -> p.age > 20 } }
// Stuff in the middle can be a variable for example:
//   fun foo() = sql { Table<Person>() }
//   val bar = sql { foo().filter { p -> p.name == "Joe" } }
//   fun baz() = sql { bar.filter { p -> p.age > 20 } }
// Now when these come one after another for each one we'll first do a TransformCaptureQuery on the `foo() = (..here..)` part, then a TransformProjectCapture on the
// `foo()` invocation in `val bar` and then call TransformProjectCapture on `val bar = (..here..)` part then repeat the process for `baz()`.
//
// Now, the tricky part comes when these are out of order which they can be for example of they are object members, let's say we have an object
//  object Foo {
//    fun baz() = sql { bar().filter { p -> p.age > 20 } }
//    fun bar() = sql { foo().filter { p -> p.name == "Joe" } }
//    fun foo() = sql { Table<Person>() }
//  }
// That's a problem because we'll come to TransformProjectCapture on `baz()` before we've even made `bar()` into a SqlQuery(...) uprootable.
// TODO describe this process
sealed interface OwnerChain {
  val root get(): Root = when (this) {
    is Root -> this
    is SourcedFunction -> parent.root
    is SourcedField -> parent.root
    is SourcedVariable -> parent.root
  }

  val irElement get() =
    when (this) {
      is SourcedFunction -> call
      is SourcedField -> call
      is SourcedVariable -> call
      is Root.VirginQuery -> call
      is Root.VirginExpr -> call
      is Root.VirginAction -> call
      is Root.VirginActionBatch -> call
      is Root.UprootableQuery -> expr
      is Root.UprootableExpr -> expr
      is Root.UprootableAction -> expr
      is Root.UprootableActionBatch -> expr
      is Root.SourcedPreviouslyCompiledFunction -> call
      is Root.Unknown -> expr
    }

  fun show(config: PPrinterConfig = PPrinterConfig()) = CaptureOwnerPrint(config).invoke(this)

  context(CX.Scope)
  fun showWithCtx(config: PPrinterConfig = PPrinterConfig()) = CaptureOwnerPrint(config, this@Scope).invoke(this)

  data class SourcedFunction(val call: IrCall, val origin: IrSimpleFunction, val isCaptured: Boolean, val containerType: ContainerType, val parent: OwnerChain): OwnerChain
  data class SourcedField(val call: IrGetField, val origin: IrField, val containerType: ContainerType, val parent: OwnerChain): OwnerChain
  data class SourcedVariable(val call: IrGetValue, val origin: IrVariable, val containerType: ContainerType, val parent: OwnerChain): OwnerChain

  sealed interface Root: OwnerChain {
    sealed interface Virgin: Root

    sealed interface Uprootable: Root {
      val expr: IrExpression
      val uprootable: io.exoquery.plugin.trees.UprootableExpr

      /**
       * Replant the expression side the uprootable. Note that an uprootable without an Expr is really just
       * a encoded query. The only component that involves an expression is the source of the params.
       * That means that outside of this replanting, the uprootable is actually an invariant.
       * So in order to replant it in the sql-project process we just replace the expression.
       * Then we just bubble it up to the next level up. We could technically just take the root
       * expression of the entire sequence but that parameter might not actually be visible to the code
       * at hand so we need to do a game of propagation.
       */
      context(CX.Scope, CX.Builder)
      fun replantUprootableWith(expr: IrExpression): Uprootable =
        when (this) {
          is Root.UprootableQuery -> Root.UprootableQuery(uprootable.replant(expr), uprootable)
          is Root.UprootableExpr -> Root.UprootableExpr(uprootable.replant(expr), uprootable)
          is Root.UprootableAction -> Root.UprootableAction(uprootable.replant(expr), uprootable)
          is Root.UprootableActionBatch -> Root.UprootableActionBatch(uprootable.replant(expr), uprootable)
        }
    }

    data class UprootableQuery(override val expr: IrExpression, override val uprootable: SqlQueryExpr.Uprootable) : Root, Uprootable
    data class UprootableExpr(override val expr: IrExpression, override val uprootable: SqlExpressionExpr.Uprootable) : Root, Uprootable
    data class UprootableAction(override val expr: IrExpression, override val uprootable: SqlActionExpr.Uprootable) : Root, Uprootable
    data class UprootableActionBatch(override val expr: IrExpression, override val uprootable: SqlBatchActionExpr.Uprootable) : Root, Uprootable

    data class VirginQuery(val call: IrCall) : Virgin, Root
    data class VirginExpr(val call: IrCall) : Virgin, Root
    data class VirginAction(val call: IrCall) : Virgin, Root
    data class VirginActionBatch(val call: IrCall) : Virgin, Root

    // I.e. a dynamic query or otherwise unknown-at-runtime element
    data class Unknown(val expr: IrExpression) : Root

    /**
     * A function call referring to a SqlQuery/SqlExpression/SqlAction/SqlBatchAction function that was compiled in a previous compilation.
     * It's only .parent property will actually be a IrExternalPackageFragment. Everything else is just scaffolding to keep the types straight.
     * For example.
     * ```
     * ModuleA: foo.kt
     * inline fun getPeopleQuery(): SqlQuery<Person> = sql { Table<Person>() }
     *
     * ModuleB: bar.kt
     * fun getPeopleNamedJoe(): SqlQuery<Person> = sql { getPeopleQuery().filter { it.name == "Joe" } }
     *
     * Note that cross-file functions MUST be inline functions the reason for this is the following. The backend IR is ephemeral
     * therefore we cannot stored any kind of compile-time extracted IR in a persistent way inside of it. Normally that is okay
     * since Kotlin compiles a whole file at a time so we can just store it in the IR of the function body itself. However, when crossing file boundaries
     * we cannot do that because the other file might not be compiled at the same time. Therefore we need to store it in some
     * kind of persistent store which is the StoredXRs.db. However, if we were to allow non-inline functions to be cross-file then we would have no way
     * of updating a call-site of function (i.e. the `getPeopleQuery()` call in `getPeopleNamedJoe()`) to have the new version for the `getPeopleQuery()` function.
     * Therefore we force the user to make `getPeopleQuery()` an inline function which will (firstly update the StoredXRs.db with the new version)
     * and then use this StoreXRs.db in the same-time compilation of `getPeopleNamedJoe()`.
     *
     * Also note that it is okay of `getPeopleNamedJoe()` is compiled before `getPeopleQuery()` in subsequent compilations. The CaptureOwner pipeline
     * will travel up the `getPeopleQuery()` call and find the `inline fun getPeopleQuery()` declaration and reprocess it normally. In that situation
     * the StoredXRs.db will not be used at all! (It will only be updated with the new value of the XR coming from `inline fun getPeopleQuery()`)
     */
    data class SourcedPreviouslyCompiledFunction(val call: IrCall, val origin: IrSimpleFunction, val isCaptured: Boolean, val containerType: ContainerType, val parent: OwnerChain.Root.Uprootable): Root
  }

  sealed interface ContainerType {
    data object Expr : ContainerType
    data object Query : ContainerType
    data object Action : ContainerType
    data object ActionBatch : ContainerType

    companion object {
      context(CX.Scope)
      fun identify(expr: IrExpression): ContainerType? =
        identify(expr.type)

      context(CX.Scope)
      fun identify(expr: IrType): ContainerType? =
        when {
          expr.isClass<SqlExpression<*>>() -> ContainerType.Expr
          expr.isClass<SqlQuery<*>>() -> ContainerType.Query
          expr.isClass<SqlAction<*, *>>() -> ContainerType.Action
          expr.isClass<SqlBatchAction<*, *, *>>() -> ContainerType.ActionBatch // Don't have the uprootable yet
          //else -> parseError("The expression is not a SqlExpression or SqlQuery, (its type is ${this.type.dumpKotlinLike()} which cannot be projected)", this)
          else -> null
        }
    }
  }

  // TODO add morking as Seen to know if we need to continue or not

  companion object {
    context(CX.Scope)
    private fun IrExpression.isContainerOfXR(): Boolean =
      this.type.isClass<SqlExpression<*>>() || this.type.isClass<SqlQuery<*>>() || this.type.isClass<SqlAction<*, *>>() || this.type.isClass<SqlBatchAction<*, *, *>>()


    context(CX.Scope)
    fun buildFrom(expression: IrExpression): OwnerChain {
      val exprType = ContainerType.identify(expression) ?: return run {
        logger.warn("------------- Early Exit for -------------\n${expression.dumpKotlinLike()}\nOwner Is: ${(expression as? IrCall)?.let { it.symbol.owner.dumpKotlinLike() } ?: "<NOT A CALL>"}\n------------- No Match -------------")
        Root.Unknown(expression)
      }
      return expression.match(
        // Bubble up to the owner call, make sure to skip captured-functions for now since they need to be handled slightly differently (i.e. the call needs to be scaffolded)
        case(Ir.Call[Is()]).thenIf { call -> expression.isContainerOfXR() && call.symbol.owner is IrSimpleFunction && !call.symbol.owner.hasAnnotation<SqlDynamic>() }.then { call ->
          val owner = call.symbol.owner
          // TODO what about a class-member? (need to check the parent's parent?) Also need to do this for an IrField
          // ===== SPECIAL CASE: Specifically for IrFunctions and IrFields, need to check that if it is a cross-file-compatible function (i.e. it's an inline function) if not immediately need to throw an error =====
          CrossFile.validatePossibleCrossFileFunction(owner, expression)
          // ===== Continue with the normal processing =====
          val isCap = owner.hasAnnotation<SqlFragment>()


          //logger.warn("------------- IN OUTER MATCH FOR -------------\n${expression.dumpKotlinLike()}\nOwner Is: ${(expression as? IrCall)?.let { it.symbol.owner.dumpKotlinLike() } ?: "<NOT A CALL>"}")
          val out = owner.match(
            caseEarly(exprType == ContainerType.Query)(Ir.SimpleFunction.withReturnOnlyExpression[Is()]).then { nextStep ->
              //logger.warn("----------------------- OWNER CHAIN STEP for ${call.symbol.safeName} -----------------------\nCALL IS:\n${call.dumpKotlinLike()}\nCALL OWNER IS:\n${owner.dumpKotlinLike()}")
              nextStep.match(
                case (SqlQueryExpr.Uprootable[Is()]).then { SourcedFunction(call, owner, isCap, exprType, it.withReplantExpr(nextStep)) },
                case (ExtractorsDomain.Call.CaptureQueryOrSelect[Is()]).then { SourcedFunction(call, owner, isCap, exprType, Root.VirginQuery(it)) }
              )
              ?: SourcedFunction(call, owner, isCap, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Expr)(Ir.SimpleFunction.withReturnOnlyExpression[Is()]).then { nextStep ->
              nextStep.match(
                case (SqlExpressionExpr.Uprootable[Is()]).then { SourcedFunction(call, owner, isCap, exprType, it.withReplantExpr(nextStep)) },
                case (ExtractorsDomain.Call.CaptureExpression[Is()]).then { SourcedFunction(call, owner, isCap, exprType, Root.VirginExpr(it)) }
              )
              ?: SourcedFunction(call, owner, isCap, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Action)(Ir.SimpleFunction.withReturnOnlyExpression[Is()]).then { nextStep ->
              nextStep.match(
                case (SqlActionExpr.Uprootable[Is()]).then { SourcedFunction(call, owner, isCap, exprType, it.withReplantExpr(nextStep)) },
                case (ExtractorsDomain.Call.CaptureAction[Is()]).then { SourcedFunction(call, owner, isCap, exprType, Root.VirginAction(it)) }
              )
              ?: SourcedFunction(call, owner, isCap, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.ActionBatch)(Ir.SimpleFunction.withReturnOnlyExpression[Is()]).then { nextStep ->
              nextStep.match(
                case (SqlBatchActionExpr.Uprootable[Is()]).then { SourcedFunction(call, owner, isCap, exprType, it.withReplantExpr(nextStep)) },
                case (ExtractorsDomain.Call.CaptureBatchAction[Is()]).then { SourcedFunction(call, owner, isCap, exprType, Root.VirginActionBatch(it)) }
              )
              ?: SourcedFunction(call, owner, isCap, exprType, buildFrom(nextStep))
            },
            // If a "real" function could not be found (i.e. one in the current set of files being compiled) take a look if there is something in the store
            // that was put there from previous compiles. (This is intentionally last because we could have a new version of a function that is currently being stored
            caseEarly(options.enableCrossFileStoreOrDefault)(Ir.SimpleFunction.anyKind[Is()]).thenIf { func -> CrossFile.isCrossFile(func) }.then { function ->
              val outStored =
                storedXRsScope.scoped {
                  when (exprType) {
                    is ContainerType.Query -> {
                      CrossFile.getUprootableFromStore(function, exprType)?.let { uprootable ->
                        Root.SourcedPreviouslyCompiledFunction(call, owner, isCap, exprType, Root.UprootableQuery(expression, uprootable))
                      }
                    }
                    is ContainerType.Expr -> {
                      CrossFile.getUprootableFromStore(function, exprType)?.let { uprootable ->
                        Root.SourcedPreviouslyCompiledFunction(call, owner, isCap, exprType, Root.UprootableExpr(expression, uprootable))
                      }
                    }
                    is ContainerType.Action -> {
                      CrossFile.getUprootableFromStore(function, exprType)?.let { uprootable ->
                        Root.SourcedPreviouslyCompiledFunction(call, owner, isCap, exprType, Root.UprootableAction(expression, uprootable))
                      }
                    }
                    is ContainerType.ActionBatch -> {
                      CrossFile.getUprootableFromStore(function, exprType)?.let { uprootable ->
                        Root.SourcedPreviouslyCompiledFunction(call, owner, isCap, exprType, Root.UprootableActionBatch(expression, uprootable))
                      }
                    }
                  }
                }

              if (outStored != null) {
                //logger.warn("------------- YAY Getting Stored Return For -------------\n${expression.dumpKotlinLike()}\nValue is: ${outStored.show() ?: "<NOT A CALL>"}")
              } else {
                //logger.warn("------------- NO STORED Return For -------------\n${expression.dumpKotlinLike()}\nOwner Is: ${(expression as? IrCall)?.let { it.symbol.owner.dumpSimple() } ?: "<NOT A CALL>"}\n---------------- Values are --------------\n${CrossFile.printStoredValues()}}")
              }
              outStored
            },
          )
          if (out == null) {
            //logger.warn("------------- NULL OUTER MATCH FOR -------------\n${expression.dumpKotlinLike()}\nOwner Is: ${(expression as? IrCall)?.let { it.symbol.owner.dumpSimple() } ?: "<NOT A CALL>"}")
          }
          out
        },
        case(Ir.GetField[Is()]).thenIf { expression.isContainerOfXR() }.thenThis { field ->
          //val newRecieiver = superTransformer.visitExpression(this.receiver)
          val owner = field.symbol.owner
          // ===== SPECIAL CASE: check if the field shares an SqlQuery/SqlExpression/SqlAction/SqlBatchAction across files and tell the user it needs to be changed to an inline function =====
          CrossFile.validatePossibleCrossFileField(owner, expression)
          // ===== Continue with the normal processing =====
          owner.match(
            // E.g. a class field used in a lift
            //   class Foo { val x = sql { 123 } } which should have become:
            //   val foo = Foo()
            //   sql { 2 + foo.x.use }
            // Which will become:
            //   SqlExpression(Int(2) + Int(123), lifts=foo.x.lifts)
            caseEarly(exprType == ContainerType.Query)(Ir.Field[Is(), Is()]).then { _, nextStep ->
              nextStep.match(
                case(SqlQueryExpr.Uprootable[Is()]).then { SourcedField(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureQueryOrSelect[Is()]).then { SourcedField(field, owner, exprType, Root.VirginQuery(it)) }
              )
                ?: SourcedField(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Expr)(Ir.Field[Is(), Is()]).then { _, nextStep ->
              nextStep.match(
                case(SqlExpressionExpr.Uprootable[Is()]).then { SourcedField(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureExpression[Is()]).then { SourcedField(field, owner, exprType, Root.VirginExpr(it)) }
              )
                ?: SourcedField(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Action)(Ir.Field[Is(), Is()]).then { _, nextStep ->
              nextStep.match(
                case(SqlActionExpr.Uprootable[Is()]).then { SourcedField(field, owner, exprType,  it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureAction[Is()]).then { SourcedField(field, owner, exprType, Root.VirginAction(it)) }
              )
                ?: SourcedField(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.ActionBatch)(Ir.Field[Is(), Is()]).then { _, nextStep ->
              nextStep.match(
                case(SqlBatchActionExpr.Uprootable[Is()]).then { SourcedField(field, owner, exprType,  it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureBatchAction[Is()]).then { SourcedField(field, owner, exprType, Root.VirginActionBatch(it)) }
              )
                ?: SourcedField(field, owner, exprType, buildFrom(nextStep))
            }
          )
        },
        // E.g. something like
        //   val x = sql { 123 + lift(456) }` // i.e. SqlExpression(XR.Int(123), ...)  // (actually it will be serialized so: `val x = SqlExpression(unpackExpr("jksnfksjdnf"), ...)`)
        //   sql { 2 + x.use }
        // Which should become:
        //   SqlExpression(..., lifts=x.lifts)
        //   in more detail:
        //   SqlExpression(Int(2) + Int(123) + ScalarTag(UUID), lifts=x.lifts) // Where x.lifts is ScalarLift(UUID, x) which lives behind the IrVariable("x")
        // so effectively:
        //   sql { 2 + x.use } ->                                                                   // can be thought of something like:
        //     sql { 2 + sql { 123 + lift(x.lifts[UUID_A]) } } ->                               // which is actually:
        //     sql { 2 + SqlExpression(Int(123) + ScalarTag(UUID_A), lifts=ScalarLift(UUID, x)) }   // which becomes:
        //     SqlExpression(Int(2) + SqlExpression(Int(123) + ScalarTag(UUID_A), lifts=ScalarLift(x))) // which then becomes:
        //     SqlExpression(Int(2) + Int(123) + ScalarTag(UUID_A), lifts=ScalarLift(x))
        case(Ir.GetValue[Is()]).thenIf { expression.isContainerOfXR() }.then { field ->
          val decl = field.symbol.owner
          decl.match(
            caseEarly(exprType == ContainerType.Query)(Ir.Variable[Is(), Is()]).thenThis { _, nextStep ->
              val owner = this
              nextStep.match(
                case(SqlQueryExpr.Uprootable[Is()]).then { SourcedVariable(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureQueryOrSelect[Is()]).then { SourcedVariable(field, owner, exprType, Root.VirginQuery(it)) }
              )
              ?: SourcedVariable(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Expr)(Ir.Variable[Is(), Is()]).thenThis { _, nextStep ->
              val owner = this
              nextStep.match(
                case(SqlExpressionExpr.Uprootable[Is()]).then { SourcedVariable(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureExpression[Is()]).then { SourcedVariable(field, owner, exprType, Root.VirginExpr(it)) }
              )
              ?: SourcedVariable(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.Action)(Ir.Variable[Is(), Is()]).thenThis { _, nextStep ->
              val owner = this
              nextStep.match(
                case(SqlActionExpr.Uprootable[Is()]).then { SourcedVariable(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureAction[Is()]).then { SourcedVariable(field, owner, exprType, Root.VirginAction(it)) }
              )
              ?: SourcedVariable(field, owner, exprType, buildFrom(nextStep))
            },
            caseEarly(exprType == ContainerType.ActionBatch)(Ir.Variable[Is(), Is()]).thenThis { _, nextStep ->
              val owner = this
              nextStep.match(
                case(SqlBatchActionExpr.Uprootable[Is()]).then { SourcedVariable(field, owner, exprType, it.withReplantExpr(nextStep)) },
                case(ExtractorsDomain.Call.CaptureBatchAction[Is()]).then { SourcedVariable(field, owner, exprType, Root.VirginActionBatch(it)) }
              )
              ?: SourcedVariable(field, owner, exprType, buildFrom(nextStep))
            }
          )
        }
      ) ?: run {
//        if (expression.dumpSimple(false).contains("[IrField] PROPERTY_BACKING_FIELD name:people type:@[Captured] io.exoquery.SqlQuery<io.exoquery.LimitedContainer.Person>")) {
//          parseError(
//            """|----------------------- NULL HERE -----------------------
//               |${expression.dumpSimple()}
//               |----------------------- Match On field -----------------------
//               |${Ir.GetField[Is()].matchesAny(expression)}
//               |----------------------- Match On owner -----------------------
//               |${Ir.Field[Is(), Is()].matchesAny((expression as IrGetField).symbol.owner)}
//               |----------------------- Match On owner uprootable -----------------------
//               |${Ir.Field[Is(), SqlQueryExpr.Uprootable[Is()]].matchesAny((expression as IrGetField).symbol.owner)}
//               |----------------------- Owner's Owner's field -----------------------
//               |${(expression as IrGetField).symbol.owner.dumpSimple()}
//               |-----------------------""", expression)
//        }

        //logger.warn("------------- Late Exit for -------------\n${expression.dumpKotlinLike()}\nOwner Is: ${(expression as? IrCall)?.let { it.symbol.owner.dumpKotlinLike() } ?: "<NOT A CALL>"}\n------------- No Match -------------")
        Root.Unknown(expression)
      }
    }
  }
}

// Root.UprootableQuery(nextStep, it)
fun SqlQueryExpr.Uprootable.withReplantExpr(newExpr: IrExpression): OwnerChain.Root.UprootableQuery =
  OwnerChain.Root.UprootableQuery(newExpr, this)
fun SqlExpressionExpr.Uprootable.withReplantExpr(newExpr: IrExpression): OwnerChain.Root.UprootableExpr =
  OwnerChain.Root.UprootableExpr(newExpr, this)
fun SqlActionExpr.Uprootable.withReplantExpr(newExpr: IrExpression): OwnerChain.Root.UprootableAction =
  OwnerChain.Root.UprootableAction(newExpr, this)
fun SqlBatchActionExpr.Uprootable.withReplantExpr(newExpr: IrExpression): OwnerChain.Root.UprootableActionBatch =
  OwnerChain.Root.UprootableActionBatch(newExpr, this)

fun UprootableExpr.withReplantExprAny(newExpr: IrExpression) =
  when (this) {
    is SqlQueryExpr.Uprootable -> this.withReplantExpr(newExpr)
    is SqlExpressionExpr.Uprootable -> this.withReplantExpr(newExpr)
    is SqlActionExpr.Uprootable -> this.withReplantExpr(newExpr)
    is SqlBatchActionExpr.Uprootable -> this.withReplantExpr(newExpr)
  }
