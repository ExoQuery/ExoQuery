package io.exoquery.plugin.transform

import io.exoquery.annotation.ChangeReciever
import io.exoquery.plugin.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class CallMethod(private val callerRaw: Caller, private val replacementFun: ReplacementMethodToCall, private val types: List<IrType>, private val tpe: IrType?) {
  context(CX.Scope, CX.Builder) operator fun invoke(vararg args: IrExpression?): IrExpression {
    val caller =
      when (replacementFun.callerType) {
        ChangeReciever.ToDispatch -> callerRaw.toDispatch()
        ChangeReciever.ToExtension -> callerRaw.toExtension()
        ChangeReciever.DoNothing -> callerRaw
      }

    val funName = replacementFun.methodToCall

    val invokeMethod =
      when (caller) {
        is Caller.Dispatch -> caller.reciver.type.findMethodOrFail(funName, args.size)
        is Caller.Extension -> caller.reciver.type.findExtensionMethodOrFail(funName, args.size)
        is Caller.TopLevelMethod ->
          // If there are overloads check that the number of arguments matches what we expect, no other checks for now
          pluginCtx.referenceFunctions(CallableId(FqName(caller.packageName), Name.identifier(funName))).find { it.owner.regularParams.size == args.size }?.let { MethodType.Method(it) }
            ?: throw IllegalArgumentException("Cannot find method `${funName}` in the package `${caller.packageName}`")
      }



    return when (invokeMethod) {
      is MethodType.Getter -> {
        if (args.isNotEmpty()) {
          throw IllegalArgumentException("Cannot call a getter with arguments but tried to call `${invokeMethod.sym.safeName}` with arguments: ${args.joinToString(", ") { it?.dumpKotlinLike() ?: "<NULL>" }}")
        }
        with(builder) {
          when (caller) {
            is Caller.Dispatch -> {
              irCall(invokeMethod.sym, invokeMethod.sym.owner.returnType).apply {
                origin = IrStatementOrigin.GET_PROPERTY
                dispatchReceiver = caller.reciver
              }
            }
            else -> {
              error("Cannot call a getter from a non-dispatch receiver")
            }
          }
        }
      }
      is MethodType.Method -> {
        val invoke = invokeMethod.sym
        with(builder) {
          val invocation = if (tpe != null) irCall(invoke, tpe) else irCall(invoke)
          invocation.apply {


            val callerPart =
              when (caller) {
                // If we are calling a function with a receiver it needs to be the first argument
                // we don't support both dispatch and extension receivers at the same time
                // and we also don't support context-parameters for now
                is Caller.Dispatch -> {
                  if (invokeMethod.sym.owner.parameters[0].kind != IrParameterKind.DispatchReceiver) {
                    throw IllegalArgumentException("Expected a dispatch-receiver on the function ${invokeMethod.sym.safeName} for the argument ${caller.reciver.dumpKotlinLike()}")
                  }
                  listOf(caller.reciver)
                }
                is Caller.Extension -> {
                  if (invokeMethod.sym.owner.parameters[0].kind != IrParameterKind.ExtensionReceiver) {
                    throw IllegalArgumentException("Expected a extension-receiver on the function ${invokeMethod.sym.safeName} for the argument ${caller.reciver.dumpKotlinLike()}")
                  }
                  listOf(caller.reciver)
                }
                is Caller.TopLevelMethod -> emptyList()
              }

            for ((index, tpe) in types.withIndex()) {
              typeArguments[index] = tpe
            }
            for ((index, expr) in (callerPart + args).withIndex()) {
              arguments[index] = expr
            }
          }
        }
      }
    }
  }
}

// TODO these should be implemented on Caller, not IrExpression

fun ReceiverCaller.call(name: ReplacementMethodToCall) = CallMethod(this, name, listOf(), null)
fun ReceiverCaller.callWithOutput(name: ReplacementMethodToCall, fullOutputType: IrType) = CallMethod(this, name, listOf(), fullOutputType)

fun ReceiverCaller.callWithParams(name: ReplacementMethodToCall, typeParams: List<IrType>): CallMethod = CallMethod(this, name, typeParams, null)
fun ReceiverCaller.callWithParamsAndOutput(name: ReplacementMethodToCall, typeParams: List<IrType>, fullOutputType: IrType): CallMethod = CallMethod(this, name, typeParams, fullOutputType)

fun ReceiverCaller.call(method: String) = CallMethod(this, ReplacementMethodToCall(method), listOf(), null)
fun ReceiverCaller.callWithOutput(method: String, fullOutputType: IrType) = CallMethod(this, ReplacementMethodToCall(method), listOf(), fullOutputType)

fun ReceiverCaller.callWithParams(method: String, typeParams: List<IrType>): CallMethod = CallMethod(this, ReplacementMethodToCall(method), typeParams, null)
fun ReceiverCaller.callWithParamsAndOutput(method: String, typeParams: List<IrType>, fullOutputType: IrType): CallMethod = CallMethod(this, ReplacementMethodToCall(method), typeParams, fullOutputType)

fun String.splitMethodPath(): Pair<String, String> {
  val split = this.split('.')
  val method = split.takeLast(1).firstOrNull() ?: throw IllegalArgumentException("No method to call in the path: $this")
  val pack = split.dropLast(1)
  return pack.joinToString(".") to method
}

fun call(fullPathMethod: String): CallMethod =
  fullPathMethod.splitMethodPath().let { (pack, method) ->
    call(pack, method)
  }

fun call(packageName: String, method: String) = CallMethod(Caller.TopLevelMethod(packageName), ReplacementMethodToCall(method), listOf(), null)
fun callWithOutput(packageName: String, method: String, tpe: IrType) = CallMethod(Caller.TopLevelMethod(packageName), ReplacementMethodToCall(method), listOf(), tpe)

fun callWithParams(packageName: String, method: String, typeParams: List<IrType>) = CallMethod(Caller.TopLevelMethod(packageName), ReplacementMethodToCall(method), typeParams, null)
fun callWithParamsAndOutput(packageName: String, method: String, typeParams: List<IrType>, tpe: IrType) = CallMethod(Caller.TopLevelMethod(packageName), ReplacementMethodToCall(method), typeParams, tpe)
fun callWithParamsAndOutput(fullPathMethod: String, typeParams: List<IrType>, tpe: IrType) =
  fullPathMethod.splitMethodPath().let { (pack, method) ->
    callWithParamsAndOutput(pack, method, typeParams, tpe)
  }


fun IrExpression.callDispatch(method: String) = CallMethod(Caller.Dispatch(this), ReplacementMethodToCall(method), listOf(), null)
fun IrExpression.callDispatchWithOutput(method: String, fullOutputType: IrType) = CallMethod(Caller.Dispatch(this), ReplacementMethodToCall(method), listOf(), fullOutputType)
fun IrExpression.callDispatchWithParams(method: String, typeParams: List<IrType>): CallMethod = CallMethod(Caller.Dispatch(this), ReplacementMethodToCall(method), typeParams, null)
fun IrExpression.callDispatchWithParamsAndOutput(method: String, typeParams: List<IrType>, fullOutputType: IrType): CallMethod =
  CallMethod(Caller.Dispatch(this), ReplacementMethodToCall(method), typeParams, fullOutputType)


context (CX.Scope, CX.Builder) fun createLambda0(functionBody: IrExpression, functionParent: IrDeclarationParent, otherStatements: List<IrStatement> = listOf()): IrFunctionExpression =
  with(builder) {
    val functionClosure = createLambda0Closure(functionBody, functionParent, otherStatements)
    val functionType = pluginCtx.symbols.functionN(0).typeWith(functionClosure.returnType)
    IrFunctionExpressionImpl(startOffset, endOffset, functionType, functionClosure, IrStatementOrigin.LAMBDA)
  }

context (CX.Scope, CX.Builder) fun createLambda1(functionBody: IrExpression, param: IrValueParameter, functionParent: IrDeclarationParent): IrFunctionExpression =
  createLambdaN(functionBody, listOf(param), functionParent)

context (CX.Scope, CX.Builder) fun createLambdaN(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrFunctionExpression =
  with(builder) {
    val functionClosure = createLambdaClosure(functionBody, params, functionParent)

    params.forEach { it.parent = functionClosure }

    val typeWith = params.map { it.type } + functionClosure.returnType
    val functionType =
      pluginCtx.symbols.functionN(params.size)
        // Remember this is FunctionN<InputA, InputB, ... Output> so these input/output args need to be both specified here
        .typeWith(typeWith)

    IrFunctionExpressionImpl(startOffset, endOffset, functionType, functionClosure, IrStatementOrigin.LAMBDA)
  }

context (CX.Scope, CX.Builder) fun createLambda0Closure(functionBody: IrExpression, functionParent: IrDeclarationParent, otherStatements: List<IrStatement> = listOf()): IrSimpleFunction {
  return with(pluginCtx) {
    irFactory.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION
      name = SpecialNames.NO_NAME_PROVIDED
      visibility = DescriptorVisibilities.LOCAL
      returnType = functionBody.type
      modality = Modality.FINAL
      isSuspend = false
    }.apply {
      parent = functionParent
      /*
      VERY important here to create a new irBuilder from the symbol i.e. createIrBuilder because
      the return-point needs to be the caller-function (which kotlin gets from the irBuilder).
      If the builder in the BuilderContext is used it will return back to whatever context the
      TransformInterpolatorInvoke IrCall expression is coming from (and this will be a non-local return)
      and since the return-type is wrong it will fail with a very large error that ultimately says:
      RETURN: Incompatible return type
       */
      body = pluginCtx.createIrBuilder(symbol).run {
        // don't use expr body, coroutine codegen can't generate for it.
        irBlockBody {
          otherStatements.forEach { +it }
          +irReturn(functionBody)
        }
      }
    }
  }
}

context (CX.Scope, CX.Builder) fun createLambdaClosure(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrSimpleFunction {
  return with(pluginCtx) {
    irFactory.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION
      name = SpecialNames.NO_NAME_PROVIDED
      visibility = DescriptorVisibilities.LOCAL
      returnType = functionBody.type
      modality = Modality.FINAL
      isSuspend = false
    }.apply {
      parent = functionParent

      // No receivers or context-parameters supported for now
      if (params.size > 0) {
        parameters = params
      }
      /*
      VERY important here to create a new irBuilder from the symbol i.e. createIrBuilder because
      the return-point needs to be the caller-function (which kotlin gets from the irBuilder).
      If the builder in the BuilderContext is used it will return back to whatever context the
      TransformInterpolatorInvoke IrCall expression is coming from (and this will be a non-local return)
      and since the return-type is wrong it will fail with a very large error that ultimately says:
      RETURN: Incompatible return type
       */
      body = pluginCtx.createIrBuilder(symbol).run {
        // don't use expr body, coroutine codegen can't generate for it.
        irBlockBody {
          +irReturn(functionBody)
        }
      }
    }
  }
}

fun IrPluginContext.createIrBuilder(
  symbol: IrSymbol,
  startOffset: Int = UNDEFINED_OFFSET,
  endOffset: Int = UNDEFINED_OFFSET,
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)
