package io.exoquery.codegen.util


/**
 * TODO use this in the NameParser.UsingRegex in order to be able to transform capture groups
 *      in custom ways e.g. to uppercase.
 */
fun transformGroups(
  input: String,
  regex: Regex,
  transform: (String) -> String
): String {
  return regex.replace(input) { matchResult ->
    val groups = matchResult.groups
    var result = matchResult.value

    // Replace groups in reverse order to avoid index shifting
    for (i in groups.size - 1 downTo 1) {
      groups[i]?.let { group ->
        val transformed = transform(group.value)
        result = result.replaceRange(
          group.rangeOrThrow().first - matchResult.range.first,
          group.rangeOrThrow().last - matchResult.range.first + 1,
          transformed
        )
      }
    }
    result
  }
}
