package io.exoquery

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.exoquery.printing.PrintableValue
import io.exoquery.printing.StaticStrings
import io.exoquery.xr.XR
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestScope
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayOutputStream
import kotlin.collections.ArrayList

@OptIn(ExperimentalCompilerApi::class)
open class MessageSpecDynamic(
  val goldenMessages: MessageSpecFile,
  val mode: Mode,
  body: MessageSpecDynamic.() -> Unit
): StringSpec(body as Function1<StringSpec, Unit>) {

  constructor(): this(
    goldenMessages = MessageSpecFile.Empty,
    mode = Mode.ExoGoldenTest(""),
    body = {}
  )

  private val outputMessages = LinkedHashMap<String, PrintableValue>()

  fun TestScope.testPath() = run {
    tailrec fun rec(case: TestCase, acc: List<String>): String =
      case.parent?.let { rec(it, acc + it.name.name) } ?: acc.reversed().joinToString("/")
    rec(this.testCase, listOf(testCase.name.name))
  }

  fun sourceFile(@Language("kotlin") code: String) = code

  private val fileHeadingRegex = Regex("""^e: file:///tmp/Kotlin-Compilation.+ \[ExoQuery\]""", RegexOption.MULTILINE)

  fun TestScope.shouldBeGoldenSuccess(@Language("kotlin")  source: String, suffix: String = "") =
    shouldBeGolden(source, KotlinCompilation.ExitCode.OK, suffix)

  fun TestScope.shouldBeGoldenError(@Language("kotlin")  source: String, suffix: String = "") =
    shouldBeGolden(source, KotlinCompilation.ExitCode.COMPILATION_ERROR, suffix)

  fun TestScope.shouldBeGolden(@Language("kotlin")  source: String, expectedExitCode: KotlinCompilation.ExitCode, suffix: String = "") = run {
    val label = testPath() + if (suffix.isEmpty()) "" else "/$suffix"
    val outStream = ByteArrayOutputStream()
    // to get the source-file name replace / and \ with _ in the label and remove quotes and other special chars
    val safeFileName =
      label
        .replace(Regex(" "), "-")
        .replace(Regex("[/\\\\]"), "_")
        .replace(Regex("[^A-Za-z0-9_]"), "")

    val result = KotlinCompilation().apply {
      sources = listOf(SourceFile.kotlin("Sample_$safeFileName.kt", source))
      inheritClassPath = true
      messageOutputStream = outStream
      compilerPluginRegistrars = listOf(io.exoquery.plugin.Registrar())
    }.compile()

    val messages = run {
      val replaced = result.messages.replace(fileHeadingRegex, "[ExoQuery]")
      val header = StaticStrings.StackTraceHeader
      if (!replaced.contains(header)) replaced else {
        val beforeHeader =
          replaced.split("\n")
          .takeWhile { it != header } // take until header

        val remainingPastHeader =
          replaced.split("\n")
          .dropWhile { it != header } // drop until header
          .drop(1) // then drop the header itself

        val (afterTruncation, numStackFrameLines) = remainingPastHeader.dropWhileCounting { it != StaticStrings.TruncationLine && it.isNotBlank() }

        (beforeHeader + listOf(header, "[Excluding ${numStackFrameLines} lines]") + afterTruncation).joinToString("\n")
      }
    }

    when (mode) {
      is Mode.ExoGoldenTest -> {
        goldenMessages.messages[label]?.let {
          assertEquals(result.exitCode, expectedExitCode)
          assertEquals(
            it.queryString.trimIndent().trim(), messages.trimIndent().trim(),
            "Golden message for label: \"$label\" did not match"
          )
        } ?: throw IllegalStateException("No golden message found for label: \"$label\"")
      }
      is Mode.ExoGoldenOverride -> {
        val printableValue = PrintableValue(messages, PrintableValue.Type.PlainText, XR.ClassId.Empty, label, listOf())
        val existing = outputMessages[label]
        if (existing != null && existing != printableValue) {
          throw IllegalStateException("The label: \"$label\" was already used with a different message.\nOld: ${existing.value}\nNew: ${printableValue.value}")
        } else if (existing == null) {
          outputMessages[label] = printableValue
        }
        assertEquals(result.exitCode, expectedExitCode)
      }
    }
  }

  private fun complete() {
    val isOverride = mode is Mode.ExoGoldenOverride
    if (!isOverride) return

    fun isNotSep(c: Char) = c != '/' && c != '\\'
    val fileNameBase = mode.fileName.dropLastWhile { it != '.' }.dropLast(1).takeLastWhile { isNotSep(it) }
    val fileName = fileNameBase + "GoldenDynamic"
    val fileContent = MessageFileKotlinMaker.invoke(outputMessages.values.toList(), fileName, "io.exoquery")

    println("========================= Generated Override File: ${mode.fileName} =========================\n" + fileContent)

    // Write next to the original file
    writeToFileCompat(mode.fileName, fileName, fileContent, true)
  }

  init {
    afterSpec { complete() }
  }
}

// Local writer for this module to avoid depending on testing module helpers
private fun writeToFileCompat(originalFilePath: String, newFileName: String, contents: String, override: Boolean) {
  try {
    val orig = java.io.File(originalFilePath)
    val dir = orig.parentFile ?: java.io.File(".")
    val target = java.io.File(dir, "$newFileName.kt")
    if (target.exists() && !override) return
    target.writeText(contents)
  } catch (t: Throwable) {
    // Best-effort: print so users can copy
    System.err.println("[MessageSpecDynamic] Failed to write golden file: ${t.message}")
  }
}


private inline fun <T> Iterable<T>.dropWhileCounting(predicate: (T) -> Boolean): Pair<ArrayList<T>, Int> {
  val list = ArrayList<T>()
  var count = 0
  var dropping = true
  for (item in this) {
    if (dropping && predicate(item)) {
      count++
      continue
    } else {
      dropping = false
      list.add(item)
    }
  }
  return list to count
}
