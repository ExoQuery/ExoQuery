package io.exoquery

import io.exoquery.annotation.ExoExtras

@ExoExtras
fun printStoredXRs(): String = errorCap("Compile time plugin did not transform the tree")

@ExoExtras
fun printLineage(arg: Any): String = errorCap("Compile time plugin did not transform the tree")

@ExoExtras
fun <R> printSource(f: () -> R): String = errorCap("Compile time plugin did not transform the tree")
fun printSourceExpr(input: String): String = input

@ExoExtras
fun <R> printSourceBefore(f: () -> R): String = errorCap("Compile time plugin did not transform the tree")

@ExoExtras
fun <T> elaborateDataClass(value: T): List<Pair<String, Any?>> = errorCap("Compile time plugin did not transform the tree")

@ExoExtras
fun currentSourceFile(): String = errorCap("Compile time plugin did not transform the tree")
