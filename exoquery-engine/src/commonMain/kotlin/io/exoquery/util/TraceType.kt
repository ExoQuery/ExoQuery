package io.exoquery.util

sealed interface TraceType {
  val value: String

  // Note: We creates types as well as traits so these can be referred to easily by type-name (i.e. without .type)
  //       this is useful when using the EnableTrace implicit pattern i.e:
  //       implicit val t = new EnableTrace {  override type Trace = TraceType.Normalizations :: HNil }
  //       Otherwise it would have to be override type Trace = TraceType.Normalizations.type

  // Specifically for situations where what needs to be printed is a type of warning to the user as opposed to an expansion
  // This kind of trace is always on by default and does not need to be enabled by the user.
  object Warning : TraceType {
    override val value = "warning"
  }

  object SqlNormalizations : TraceType {
    override val value = "sql"
  }

  object ExpandDistinct : TraceType {
    override val value = "distinct"
  }

  object Normalizations : TraceType {
    override val value = "norm"
  }

  object Standard : TraceType {
    override val value = "standard"
  }

  object NestedQueryExpansion : TraceType {
    override val value = "nest"
  }

  object AvoidAliasConflict : TraceType {
    override val value = "alias"
  }

  object ShealthLeaf : TraceType {
    override val value = "sheath"
  }

  object ReifyLiftings : TraceType {
    override val value = "reify"
  }

  object PatMatch : TraceType {
    override val value = "patmatch"
  }

  object Quotation : TraceType {
    override val value = "quote"
  }

  object RepropagateTypes : TraceType {
    override val value = "reprop"
  }

  object RenameProperties : TraceType {
    override val value = "rename"
  }

  object ApplyMap : TraceType {
    override val value = "applymap"
  }

  object ExprModel : TraceType {
    override val value = "exprmodel"
  }

  object Meta : TraceType {
    override val value = "meta"
  }

  object Execution : TraceType {
    override val value = "exec"
  }

  object DynamicExecution : TraceType {
    override val value = "dynamicexec"
  }

  object Elaboration : TraceType {
    override val value = "elab"
  }

  object SqlQueryConstruct : TraceType {
    override val value = "sqlquery"
  }

  object FlattenOptionOperation : TraceType {
    override val value = "option"
  }

  object Particularization : TraceType {
    override val value = "parti"
  }

  object CrunchFlatJoins : TraceType {
    override val value = "crunch"
  }

  companion object {
    fun fromClassStr(classStr: String) =
      values.find { it::class.simpleName == classStr ?: false }

    val values: List<TraceType> = listOf(
      Standard,
      SqlNormalizations,
      Normalizations,
      NestedQueryExpansion,
      AvoidAliasConflict,
      ReifyLiftings,
      PatMatch,
      Quotation,
      RepropagateTypes,
      RenameProperties,
      Warning,
      ShealthLeaf,
      ApplyMap,
      ExpandDistinct,
      ExprModel,
      Meta,
      Execution,
      DynamicExecution,
      Elaboration,
      SqlQueryConstruct,
      FlattenOptionOperation,
      Particularization,
      CrunchFlatJoins
    )
  }
}

data class TraceConfig(val enabledTraces: List<TraceType>, val outputSink: Tracer.OutputSink, val phaseLabel: String? = null) {
  companion object {
    val empty = TraceConfig(listOf(), Tracer.OutputSink.None, null)
  }
}
