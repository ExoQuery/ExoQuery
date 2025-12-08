package io.exoquery.util

expect fun getExoVariable(propName: String, envName: String, default: String): String

object Globals {

  const val JsonExtractFunctionName = "json_extract"
  const val JsonExtractAsStringFunctionName = "json_extract_as_string"

  // This needs to be at the top for some reason, even if it is lazy
  private val cacheMap: MutableMap<String, Any> by lazy { mutableMapOf() }

  private fun variable(propName: String, envName: String, default: String) =
    getExoVariable(propName, envName, default)

  val querySubexpand get() = cache("exo.norm.subexpand", variable("exo.norm.subexpand", "exo_norm_subexpand,", "true").toBoolean())
  val traceColors get() = cache("exo.trace.color", variable("exo.trace.color", "exo_trace_color", "true").toBoolean())
  val traceEnabled get() = cache("exo.trace.enabled", variable("exo.trace.enabled", "exo_trace_enabled", "false").toBoolean())

  fun resetCache(): Unit = cacheMap.clear()

  @Suppress("UNCHECKED_CAST")
  private fun <T> cache(name: String, value: T): T =
    cacheMap.getOrPut(name, { value as Any }) as T

  internal val traces: List<TraceType>
    get() = run {
      val argValue = variable("exo.trace.types", "exo_trace_types", "standard")
      cache(
        "exo.trace.types",
        if (argValue == "all")
          TraceType.values
        else
          argValue
            .split(",")
            .toList()
            .map { it.trim() }
            .flatMap { trace -> TraceType.values.filter { traceType -> trace == traceType.value } }
      )
    }

  fun tracesEnabled(tt: TraceType): Boolean =
    (traceEnabled && traces.contains(tt)) || tt == TraceType.Warning

  fun traceConfig(): TraceConfig = TraceConfig(if (traceEnabled) traces else listOf(), Tracer.OutputSink.None)
}
