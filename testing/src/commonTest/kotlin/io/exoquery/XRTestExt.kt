package io.exoquery

import io.exoquery.xr.XR
import io.exoquery.xr.XRType

// In production code we want all the types of idents to be explicitly specified but for most
// tests it is fine to assume they just represent a value
operator fun XR.Ident.Companion.invoke(name: String) = XR.Ident(name, XRType.Value)

val String.toId: XR.Ident get() = XR.Ident(this)
