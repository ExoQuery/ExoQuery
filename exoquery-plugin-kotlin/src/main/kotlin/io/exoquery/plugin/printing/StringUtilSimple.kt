package io.exoquery.plugin.printing


object StringUtilSimple {
  fun escapeStringCharacters(s: String): String {
    val buffer = java.lang.StringBuilder(s.length)
    escapeStringCharacters(s.length, s, "\"", buffer)
    return buffer.toString()
  }

  fun escapeStringCharacters(length: Int, str: String, buffer: StringBuilder) {
    escapeStringCharacters(length, str, "\"", buffer)
  }

  fun escapeStringCharacters(length: Int, str: String, additionalChars: String?, buffer: StringBuilder): StringBuilder {
    return escapeStringCharacters(length, str, additionalChars, true, buffer)
  }

  fun escapeStringCharacters(length: Int, str: String, additionalChars: String?, escapeSlash: Boolean, buffer: StringBuilder): StringBuilder {
    return escapeStringCharacters(length, str, additionalChars, escapeSlash, true, buffer)
  }

  fun escapeStringCharacters(length: Int, str: String, additionalChars: String?, escapeSlash: Boolean, escapeUnicode: Boolean, buffer: StringBuilder): StringBuilder {
    var prev = 0.toChar()

    for (idx in 0..<length) {
      val ch = str.get(idx)
      when (ch) {
        '\b' -> buffer.append("\\b")
        '\t' -> buffer.append("\\t")
        '\n' -> buffer.append("\\n")
        '\u000b' -> if (escapeSlash && ch == '\\') {
          buffer.append("\\\\")
        } else {
          if (additionalChars == null || additionalChars.indexOf(ch) <= -1 || !escapeSlash && prev == '\\') {
            if (escapeUnicode && !isPrintableUnicode(ch)) {
              val hexCode: CharSequence = Integer.toHexString(ch.code).uppercase()
              buffer.append("\\u")
              var paddingCount = 4 - hexCode.length

              while (paddingCount-- > 0) {
                buffer.append(0)
              }

              buffer.append(hexCode)
              break
            }

            buffer.append(ch)
            break
          }

          buffer.append("\\").append(ch)
        }
        '\u000c' -> buffer.append("\\f")
        '\r' -> buffer.append("\\r")
        else -> if (escapeSlash && ch == '\\') {
          buffer.append("\\\\")
        } else {
          if (additionalChars == null || additionalChars.indexOf(ch) <= -1 || !escapeSlash && prev == '\\') {
            if (escapeUnicode && !isPrintableUnicode(ch)) {
              val hexCode: CharSequence = Integer.toHexString(ch.code).uppercase()
              buffer.append("\\u")
              var paddingCount = 4 - hexCode.length

              while (paddingCount-- > 0) {
                buffer.append(0)
              }

              buffer.append(hexCode)
              break
            }

            buffer.append(ch)
            break
          }

          buffer.append("\\").append(ch)
        }
      }

      prev = ch
    }

    return buffer
  }


  fun isPrintableUnicode(c: Char): Boolean {
    val t = Character.getType(c)
    val block = Character.UnicodeBlock.of(c)
    return t != 0 && t != 13 && t != 14 && t != 15 && t != 16 && t != 18 && t != 19 && block !== Character.UnicodeBlock.VARIATION_SELECTORS && block !== Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT
  }
}
