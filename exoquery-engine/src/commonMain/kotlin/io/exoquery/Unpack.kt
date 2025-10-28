package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.generation.Code
import io.exoquery.lang.SqlQueryModel
import io.exoquery.xr.EncodingXR
import io.exoquery.xr.XR
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString

@ExoInternal
fun unpackCodeEntities(expr: String): Code.Entities =
  EncodingXR.protoBuf.decodeFromHexString<Code.Entities>(expr)

@ExoInternal
fun unpackExpr(expr: String): XR.Expression =
  EncodingXR.protoBuf.decodeFromHexString<XR.Expression>(expr)

@ExoInternal
fun unpackQueryModel(query: String): SqlQueryModel =
  EncodingXR.protoBuf.decodeFromHexString<SqlQueryModel>(query)

@ExoInternal
fun unpackQueryModelLazy(query: String): () -> SqlQueryModel =
  { unpackQueryModel(query) }

@ExoInternal
fun unpackQuery(query: String): XR.Query =
  EncodingXR.protoBuf.decodeFromHexString<XR.Query>(query)

@ExoInternal
fun unpackXR(xr: String): XR =
  EncodingXR.protoBuf.decodeFromHexString<XR>(xr)

@ExoInternal
fun XR.packXR(): String =
  EncodingXR.protoBuf.encodeToHexString(this)

@ExoInternal
fun XR.Query.packQuery(): String =
  EncodingXR.protoBuf.encodeToHexString(this)

fun XR.Expression.packExpression(): String =
  EncodingXR.protoBuf.encodeToHexString(this)

@ExoInternal
fun XR.Action.packAction(): String =
  EncodingXR.protoBuf.encodeToHexString(this)

@ExoInternal
fun XR.Batching.packBatchAction(): String =
  EncodingXR.protoBuf.encodeToHexString(this)

@ExoInternal
fun unpackQueryLazy(query: String): () -> XR.Query =
  { unpackQuery(query) }

@ExoInternal
fun unpackAction(action: String): XR.Action =
  EncodingXR.protoBuf.decodeFromHexString<XR.Action>(action)

@ExoInternal
fun unpackActionLazy(action: String): () -> XR.Action =
  { unpackAction(action) }

@ExoInternal
fun unpackBatchAction(batchAction: String): XR.Batching =
  EncodingXR.protoBuf.decodeFromHexString<XR.Batching>(batchAction)

@ExoInternal
fun unpackBatchActionLazy(batchAction: String): () -> XR.Batching =
  { unpackBatchAction(batchAction) }
