package io.exoquery.plugin

import io.exoquery.ExoValue
import io.exoquery.plugin.trees.PT.controller_SqlJsonValue
import kotlinx.serialization.Contextual
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull

fun IrType.isContextualColumn() =
  this.hasAnnotation<Contextual>() ||
    (this.classOrNull?.owner?.hasAnnotation<Contextual>() ?: false)

fun IrType.isExoValueColumn() =
  this.hasAnnotation<ExoValue>() ||
    (this.classOrNull?.owner?.hasAnnotation<ExoValue>() ?: false)

fun IrType.isSqlJsonColumn() =
  this.hasAnnotation(controller_SqlJsonValue) ||
    (this.classOrNull?.owner?.hasAnnotation(controller_SqlJsonValue) ?: false)

fun IrType.isNonValueDataClass() =
  this.isDataClass() && !this.isExoValueColumn() && !this.isContextualColumn()

fun IrCall.isSqlJsonColumnField(): Boolean =
  this.someOwnerHasAnnotation(controller_SqlJsonValue) || this.type.isSqlJsonColumn()
