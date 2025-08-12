package io.exoquery.codegen.util

class RangeNotSupportedException() : Exception("Range replacement is not supported on this platform")

/** Only JS does not support this and we don't support Codegen on JS anyway */
expect fun MatchGroup.rangeOrThrow(): IntRange
