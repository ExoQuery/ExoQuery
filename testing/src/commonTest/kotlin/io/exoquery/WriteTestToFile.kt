package io.exoquery

expect fun writeToFile(originalFilePath: String, newFileName: String, contents: String, override: Boolean)
