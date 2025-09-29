import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

repositories {
  // Do NOT enable this otherwise all kinds of horror ensues. Not exactly sure why. Maybe something in the local repo interferes with the build. with builds.
  // Note that this is also used in the publish plugin althought it is not strictly necessary for it to be there.
  //mavenLocal()
  mavenCentral()
}


val kotlinVersion = "2.2.20"
val pluginVersion = "1.7.1.PL"

// For exoquery-engine, exoquery-plugin-gradle, exoquery-plugin-kotlin
extra["pluginProjectVersion"] = "${kotlinVersion}-${pluginVersion}"
// For exoquery-runner-core, exoquery-runner-jdbc, exoquery-runner-android, exoquery-runner-native
extra["controllerProjectVersion"] = "${pluginVersion}"

group = "io.exoquery"
// Default version is the plugin-project version. Overridden in the subprojects
version = extra["pluginProjectVersion"].toString()

val decomatVersion = "1.0.0"
extra["decomatVersion"] = decomatVersion

check("$version".isNotBlank() && version != "unspecified")
{ "invalid version $version" }

tasks.withType<AbstractTestTask> {
  testLogging {
    lifecycle {
      events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
      exceptionFormat = TestExceptionFormat.FULL

      showExceptions = true
      showCauses = true
      showStackTraces = false
      showStandardStreams = false
    }
    info.events = lifecycle.events
    info.exceptionFormat = lifecycle.exceptionFormat
  }

  val failedTests = mutableListOf<TestDescriptor>()
  val skippedTests = mutableListOf<TestDescriptor>()

  addTestListener(object : TestListener {
    override fun beforeSuite(suite: TestDescriptor) {}

    override fun beforeTest(testDescriptor: TestDescriptor) {}

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
      when (result.resultType) {
        TestResult.ResultType.FAILURE -> failedTests.add(testDescriptor)
        TestResult.ResultType.SKIPPED -> skippedTests.add(testDescriptor)
        else -> Unit
      }
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {
      if (suite.parent == null) {
        logger.lifecycle("################ Summary::Start ################")
        logger.lifecycle("Test result: ${result.resultType}")
        logger.lifecycle(
          "Test summary: ${result.testCount} tests, " +
              "${result.successfulTestCount} succeeded, " +
              "${result.failedTestCount} failed, " +
              "${result.skippedTestCount} skipped"
        )
        failedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tFailed Tests")
        skippedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tSkipped Tests:")
        logger.lifecycle("################ Summary::End ##################")
      }
    }

    private infix fun List<TestDescriptor>.prefixedSummary(subject: String) {
      logger.lifecycle(subject)
      forEach { test -> logger.lifecycle("\t\t${test.displayName()}") }
    }

    private fun TestDescriptor.displayName() = parent?.let { "${it.name} - $name" } ?: "$name"

  })
}
