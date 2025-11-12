package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.BooleanLiteralSupport
import io.exoquery.lang.SqlIdiom
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer

@ExoInternal
class BooleanLiteralTestDialect(override val traceConf: TraceConfig = TraceConfig.empty) : BooleanLiteralSupport, SqlIdiom {
  override val concatFunction: String = "||"
  override val useActionTableAliasAs = SqlIdiom.ActionTableAliasBehavior.UseAs

  override val trace: Tracer by lazy { Tracer(traceType, traceConf, 1) }
}
