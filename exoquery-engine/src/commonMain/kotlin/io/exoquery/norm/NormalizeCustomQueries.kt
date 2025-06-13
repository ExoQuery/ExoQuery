package io.exoquery.norm

import io.exoquery.xr.StatefulTransformer
import io.exoquery.xr.StatelessTransformer
import io.exoquery.xr.XR

// Convert all instances of CustomQuery (that are convertable to regular XR).
// Right now that is the only supported custom query
class NormalizeCustomQueries private constructor (val isOutermostQuery: Boolean) : StatelessTransformer {

  override fun invoke(xr: XR.Query): XR.Query =
    when {
      xr is XR.CustomQueryRef && xr.customQuery is XR.CustomQuery.Convertable -> {
        // If there are select clauses inside of this one that need to be transformed, tell them that they're not outermost-queries
        val a = xr.customQuery.handleStatelessTransform(NormalizeCustomQueries(false))
        // Current state applies to the current query (i.e. if it is outermost)
        a.toQueryXR(isOutermostQuery)
      }
      else -> super.invoke(xr)
    }

  companion object {
    operator fun invoke(xr: XR.Query) = NormalizeCustomQueries(true).invoke(xr)
  }
}

