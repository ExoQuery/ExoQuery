package io.exoquery

actual fun writeToFile(originalFilePath: String, newFileName: String, contents: String, override: Boolean) {
  // This function only needs to be run once in order to generate the golden-spec files so it is run from the JVM deployment
  // and then the files are checked in to the repository.
}
