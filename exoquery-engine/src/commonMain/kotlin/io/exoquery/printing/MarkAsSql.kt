package io.exoquery.printing

import org.intellij.lang.annotations.Language

data class GoldenResult(val queryString: String, val params: List<Pair<String, String>> = listOf())

fun cr(@Language("SQL") str: String, vararg params: Pair<String, String>): GoldenResult = GoldenResult(str, params.toList())
fun kt(@Language("Kotlin") str: String): GoldenResult = GoldenResult(str)
fun pl(str: String): GoldenResult = GoldenResult(str)
