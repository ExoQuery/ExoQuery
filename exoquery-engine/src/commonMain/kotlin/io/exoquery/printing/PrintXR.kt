package io.exoquery.printing

import io.exoquery.ParamBatchRefiner
import io.exoquery.ParamMulti
import io.exoquery.ParamSet
import io.exoquery.ParamSingle
import io.exoquery.RuntimeSet
import io.exoquery.SqlAction
import io.exoquery.SqlBatchAction
import io.exoquery.SqlCompiledAction
import io.exoquery.SqlCompiledBatchAction
import io.exoquery.SqlExpression
import io.exoquery.SqlQuery
import io.exoquery.fansi.Attrs
import io.exoquery.kmp.pprint.PPrinter
import io.exoquery.kmp.pprint.PPrinterManual
import io.exoquery.pprint.PPrinterConfig
import io.exoquery.pprint.Tree
import io.exoquery.lang.ParamBatchToken
import io.exoquery.lang.ParamBatchTokenRealized
import io.exoquery.lang.ParamMultiToken
import io.exoquery.lang.ParamMultiTokenRealized
import io.exoquery.lang.ParamSingleToken
import io.exoquery.lang.ParamSingleTokenRealized
import io.exoquery.lang.SetContainsToken
import io.exoquery.lang.Statement
import io.exoquery.lang.StringToken
import io.exoquery.lang.Token
import io.exoquery.lang.TokenContext
import io.exoquery.util.ShowTree
import io.exoquery.xr.*
import kotlinx.serialization.SerializationStrategy

fun <T> iteratorOf(vararg elements: T): Iterator<T> = elements.iterator()

object PrintXRType {
  val BlackWhite = PrintXR(XRType.serializer(), PrintXR.defaultConfig)
}

fun qprint(xr: XR) = PrintXR.Color.invoke(xr)
fun pprintMisc(any: Any?) = PrintMisc().invoke(any)

class PrintToken(config: PPrinterConfig = PPrinterConfig()) : PPrinterManual<Token>(config) {
  fun treeifyThis(x: Token, elementName: String?) = treeify(x, elementName, escapeUnicode = config.defaultEscapeUnicode, showFieldNames = config.defaultShowFieldNames)

  override fun treeify(x: Token, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when (x) {
      is ParamMultiToken -> Tree.Apply("ParamMultiToken", iteratorOf(Tree.Literal(x.bid.value, "bid")))
      is ParamMultiTokenRealized -> Tree.Apply(
        "ParamMultiTokenRealized",
        iteratorOf(Tree.Literal(x.bid.value, "bid"), x.param?.let { Tree.Apply("List", iteratorOf(Tree.Literal(it.value.toString()))) } ?: Tree.Literal("null", "param")))
      is ParamSingleToken -> Tree.Apply("ParamSingleToken", iteratorOf(Tree.Literal(x.bid.value, "bid")))
      is ParamSingleTokenRealized -> Tree.Apply(
        "ParamSingleTokenRealized",
        iteratorOf(Tree.Literal(x.bid.value, "bid"), x.param?.let { Tree.Apply("List", iteratorOf(Tree.Literal(it.value.toString()))) } ?: Tree.Literal("null", "param")))
      is ParamBatchToken -> Tree.Apply("ParamBatchToken", iteratorOf(Tree.Literal(x.bid.value, "bid")))
      is ParamBatchTokenRealized -> Tree.Apply(
        "ParamBatchTokenRealized",
        iteratorOf(
          Tree.Literal(x.bid.value, "bid"),
          Tree.Literal(x.chunkIndex.toString(), "chunkIndex"),
          x.param?.let { Tree.Apply("List", iteratorOf(Tree.Literal(it.showValue().toString()))) } ?: Tree.Literal("null", "param")))
      is SetContainsToken -> Tree.Apply("SetContainsToken", iteratorOf(treeifyThis(x.a, "a"), treeifyThis(x.op, "op"), treeifyThis(x.b, "b")))
      is Statement -> Tree.Apply("Statement", x.tokens.map { treeifyThis(it, null) }.iterator())
      is StringToken -> Tree.Apply("StringToken", iteratorOf(Tree.Literal(x.string, "string")))
      is TokenContext -> Tree.Apply("TokenContext", iteratorOf(treeifyThis(x.content, "content"), Tree.Literal(x.kind.toString(), "kind")))
    }
}

class PrintMisc(config: PPrinterConfig = PPrinterConfig()) : PPrinterManual<Any?>(config) {
  fun treeifyThis(x: Any?, elementName: String?) = treeify(x, elementName, escapeUnicode = config.defaultEscapeUnicode, showFieldNames = config.defaultShowFieldNames)

  override fun treeify(x: Any?, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    when (x) {
      is XR -> PrintXR(XR.serializer(), config.copy(defaultShowFieldNames = false)).treeifyThis(x, elementName)
      is XRType -> PrintXR(XRType.serializer(), config).treeifyThis(x, elementName)
      is io.exoquery.lang.SqlQueryModel -> PrintXR(io.exoquery.lang.SqlQueryModel.serializer(), config).treeifyThis(x, elementName)
      is SqlExpression<*> -> Tree.Apply("SqlExpression", iteratorOf(treeifyThis(x.xr, "xr"), treeifyThis(x.runtimes, "runtimes"), treeifyThis(x.params, "params")))
      is SqlQuery<*> -> Tree.Apply("SqlQuery", iteratorOf(treeifyThis(x.xr, "xr"), treeifyThis(x.runtimes, "runtimes"), treeifyThis(x.params, "params")))
      is SqlAction<*, *> -> Tree.Apply("SqlAction", iteratorOf(treeifyThis(x.xr, "xr"), treeifyThis(x.runtimes, "runtimes"), treeifyThis(x.params, "params")))
      is SqlBatchAction<*, *, *> -> Tree.Apply("SqlActionBatch", iteratorOf(treeifyThis(x.xr, "xr"), treeifyThis(x.batchParam, "batchParam"), treeifyThis(x.runtimes, "runtimes"), treeifyThis(x.params, "params")))
      //is ParamSet -> Tree.Apply("ParamSet", x.lifts.map { l -> Tree.KeyValue(l.id.value, Tree.Literal(l.showValue().toString())) }.iterator(
      // ))
      is ParamSet -> Tree.Apply("ParamSet", x.lifts.map { l -> treeifyThis(l, null) }.iterator())

      is RuntimeSet -> Tree.Apply("RuntimeSet", x.runtimes.map { (id, xr) -> Tree.KeyValue(id.value, treeifyThis(xr, null)) }.iterator())
      is ShowTree -> x.showTree(config)
      is ParamSingle<*> -> Tree.Apply(
        "ParamSingle",
        iteratorOf(Tree.Literal(x.id.value), Tree.Literal(x.value.toString()), Tree.Literal(x.serial.serializer.descriptor.kind.toString()), Tree.Literal(x.description.toString()))
      )
      is ParamMulti<*> -> Tree.Apply(
        "ParamMulti",
        iteratorOf(Tree.Literal(x.id.value), Tree.Literal(x.value.toString()), Tree.Literal(x.serial.serializer.descriptor.kind.toString()), Tree.Literal(x.description.toString()))
      )
      is ParamBatchRefiner<*, *> -> Tree.Apply(
        "ParamBatchRefiner",
        iteratorOf(Tree.Literal(x.id.value), Tree.Literal(x.serial.serializer.descriptor.kind.toString()), Tree.Literal(x.description.toString().replace("\n", "")))
      )

      // Treeify the following: val value: String, override val token: Token, val needsTokenization: Boolean, val returningType: ReturningType, val label: String?, val phase: Phase
      is SqlCompiledAction<*, *> -> Tree.Apply(
        "SqlCompiledAction", iteratorOf(
          Tree.KeyValue("value", Tree.Literal(x.value)),
          Tree.KeyValue("token", PrintToken().treeifyThis(x.token, "token")),
          Tree.KeyValue("params", Tree.Apply("List", x.params.map { treeifyThis(it, null) }.iterator())),
          //Tree.KeyValue("needsTokenization", Tree.Literal("${x.needsTokenization}")),
          Tree.KeyValue("returningType", Tree.Literal(x.actionReturningKind.toString())),
          Tree.KeyValue("label", Tree.Literal(x.label ?: "null")),
          Tree.KeyValue("phase", Tree.Literal(x.debugData.phase.toString())),
          Tree.KeyValue("originalXR", treeifyThis(x.debugData.originalXR(), "originalXR"))
        )
      )

      is SqlCompiledBatchAction<*, *, *> -> Tree.Apply(
        "SqlCompiledBatchAction", iteratorOf(
          Tree.KeyValue("value", Tree.Literal(x.value)),
          Tree.KeyValue("token", PrintToken().treeifyThis(x.token, "token")),
          Tree.KeyValue("params", Tree.Apply("List", x.params.map { treeifyThis(it, null) }.iterator())),
          //Tree.KeyValue("needsTokenization", Tree.Literal("${x.needsTokenization}")),
          Tree.KeyValue("returningType", Tree.Literal(x.actionReturningKind.toString())),
          Tree.KeyValue("batchParam", Tree.Apply("List", x.batchParam.map { Tree.Literal(it.toString()) }.iterator())),
          Tree.KeyValue("label", Tree.Literal(x.label ?: "null")),
          Tree.KeyValue("phase", Tree.Literal(x.debugData.phase.toString())),
          Tree.KeyValue("originalXR", treeifyThis(x.debugData.originalXR(), "originalXR"))
        )
      )

      else -> super.treeify(x, elementName, escapeUnicode, showFieldNames)
    }
}

// Simple printer for things like SelectClause that skip the loc/file fields. In the future may want to make skippable fields configurable in the constructor
class PrintSkipLoc<T>(serializer: SerializationStrategy<T>, config: PPrinterConfig = PrintXR.defaultConfig) : PPrinter<T>(serializer, config) {
  override fun <E> treeifyComposite(elem: Treeifyable.Elem<E>, elementName: String?, showFieldNames: Boolean): Tree =
    when (val x = elem.value) {
      is XR -> PrintXR(XR.serializer(), config).treeifyThis(x, elementName)
      else ->
        when (val tree = super.treeifyComposite(elem, elementName, showFieldNames)) {
          is Tree.Apply -> {
            val superNodes = tree.body.asSequence()
              .filterNot { it.elementName == "loc" }
              .filterNot { it.elementName == "file" }
            Tree.Apply(tree.prefix, superNodes.iterator(), elementName)
          }
          else -> tree
        }
    }
}

class PrintXR<T>(serializer: SerializationStrategy<T>, config: PPrinterConfig = defaultConfig) : PPrinter<T>(serializer, config) {

  fun treeifyThis(x: T, elementName: String?) =
    treeify(x, elementName, escapeUnicode = config.defaultEscapeUnicode, showFieldNames = defaultConfig.defaultShowFieldNames)

  override fun <E> treeifyComposite(elem: Treeifyable.Elem<E>, elementName: String?, showFieldNames: Boolean): Tree =
    when (val x = elem.value) {
      is XR ->
        // NEVER to super.treeifyComposite(Treeifyable.Elem(x, serializer<XR>()), ...) as that will cause infinite recursion
        // that is because For a polymorphic hierarchy such as:
        // sealed interface Colors { object Red: Colors; data class Custom(val hex: String): Colors }
        // the json-serialized form of Colors.Red will be:
        // { "type": "Red", "value": {} } which in pprint will appear as Red(type="package.Red", value=Red)
        // For the class `Custom` it will be:
        // { "type": "Custom", "value": { "hex": "FF0000" } } which in pprint will appear as Custom(type="package.Custom", value=Custom(hex="FF0000"))
        // now when it is recursing inside the outer object the serializer it will call treeifyElement which will recurse inside here.
        // Then the `serializer<XR>` will create the same outer-serializer again and the cycle will go on forever.
        when (val tree = super.treeifyComposite(elem, elementName, config.defaultShowFieldNames)) {
          is Tree.Apply -> {
            val superNodes = tree.body.asSequence()
              .filterNot { it.elementName == "loc" }
              .filterNot { it.elementName == "file" }
            Tree.Apply(tree.prefix, superNodes.iterator(), elementName)
          }
          else -> tree
        }
      is ShowTree -> x.showTree(config)

      // A bunch of FlattenSqlQuery child-elements could be null, don't print them. Also because it has so many different kinds of things in it it really helps to know the headings
      // (There seems to be a bug in pprint where the headings of a null values are not printed when showFieldNames is true. Need to look into that.)
      is io.exoquery.lang.FlattenSqlQuery ->
        when (val treet = super.treeifyComposite(elem, elementName, true)) {
          is Tree.Apply -> {
            val superNodes = treet.body.asSequence()
              .filterNot { it is Tree.KeyValue && it.value is Tree.Literal && it.value.elementName == "null" }
            Tree.Apply(treet.prefix, superNodes.iterator(), elementName)
          }
          else -> treet
        }

      is io.exoquery.lang.SqlQueryModel -> super.treeifyComposite(elem, elementName, true)

      else -> super.treeifyComposite(elem, elementName, showFieldNames)
    }


  private fun String.toLit() = Tree.Literal(this, null)


  override fun <R> treeifyValueOrNull(x: R, elementName: String?, escapeUnicode: Boolean, showFieldNames: Boolean): Tree? = run {
    when (x) {
      is XRType ->
        when (x) {
          is XRType.Product -> Tree.Literal("${x.name.takeLastWhile { it != '.' }}(...)", elementName)
          else -> Tree.Literal(x.shortString(), elementName)
        }
      //is XR.Product -> Tree.Apply("Product", (listOf(treeifyThis(x.name.takeLastWhile { it != '.' })) + x.fields.map { treeifyThis(it) }).iterator())
      //is XR.Free -> Tree.Apply("Free", iteratorOf(treeifySuper(x.parts), treeifySuper(x.params)))
      is XR.Ident -> Tree.Apply("Id", iteratorOf(Tree.Literal(x.name, "name"), PrintXR(XRType.serializer(), config).treeifyThis(x.type, "type")), elementName)
      is XR.Location.File -> Tree.Literal("<Location:${x.path}:${x.row}:${x.col}>", elementName)
      is XR.Location.Synth -> Tree.Literal("<Location:Synthetic>", elementName)

      is XR.Const.String -> Tree.Apply("String", iteratorOf(Tree.Literal(x.value, null)), elementName)
      is XR.Const.Int -> Tree.Apply("Int", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Double -> Tree.Apply("Double", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Boolean -> Tree.Apply("Bool", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Null -> Tree.Apply("Null", iteratorOf(), elementName)
      is XR.Const.Char -> Tree.Apply("Char", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Byte -> Tree.Apply("Byte", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Short -> Tree.Apply("Short", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Long -> Tree.Apply("Long", iteratorOf(Tree.Literal("${x.value}", null)), elementName)
      is XR.Const.Float -> Tree.Apply("Float", iteratorOf(Tree.Literal("${x.value}", null)), elementName)

      is XR.ClassId -> Tree.Apply("ClassId", iteratorOf(x.packageFqName.path.joinToString(".").toLit(), x.relativeClassName.path.joinToString(".").toLit()), elementName)

      //is DistinctKind -> Tree.Literal(x::class.simpleName ?: "BinaryOp?")
      is OP -> Tree.Literal(x.symbol, elementName)
      //is PC<*> -> Tree.Apply(x::class.simpleName ?: "PC?", run {
      //  when (val pc = x.productComponents) {
      //    is ProductClass0 -> iteratorOf()
      //    is ProductClass1<*, *> -> iteratorOf(treeifyThis(pc.a))
      //    is ProductClass2<*, *, *> -> iteratorOf(treeifyThis(pc.a), treeifyThis(pc.b))
      //    is ProductClass2M<*, *, *, *> -> iteratorOf(treeifyThis(pc.a), treeifyThis(pc.m), treeifyThis(pc.b))
      //    else -> iteratorOf()
      //  }
      //})
      else -> null
    }
  }

  companion object {
    val defaultConfig = PPrinterConfig(defaultWidth = 200, defaultShowFieldNames = false)
    val BlackWhite =
      PrintXR(
        XR.serializer(), PPrinterConfig().copy(
          defaultShowFieldNames = false,
          colorLiteral = Attrs.Empty,
          colorApplyPrefix = Attrs.Empty
        )
      )
    val Color =
      PrintXR(
        XR.serializer(), PPrinterConfig().copy(
          defaultShowFieldNames = false
        )
      )
  }
}
