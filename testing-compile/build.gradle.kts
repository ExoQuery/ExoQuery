plugins {
  id("conventions")
  kotlin("multiplatform") version "2.2.20"
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")
}

version = extra["controllerProjectVersion"].toString()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-receivers")
    optIn.add("io.exoquery.annotation.ExoInternal")
    java {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}

repositories {
  mavenCentral()
  mavenLocal()
}

kotlin {
  compilerOptions { optIn.add("io.exoquery.annotation.ExoInternal") }
  jvmToolchain(17)
  jvm {}

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  sourceSets {
    jvmMain {
      kotlin.srcDir("src/main/kotlin")
      resources.srcDir("src/main/resources")
      dependencies {
        // No main deps yet
      }
    }
    jvmTest {
      kotlin.srcDir("src/test/kotlin")
      resources.srcDir("src/test/resources")
      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.assertions)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        // Kotlin Compile Testing library (use the maintained fork compatible with Kotlin 2.x)
        implementation("dev.zacsweers.kctfork:core:0.11.0")
        // Use the compiler plugin from local maven if available (matches repo's pluginProjectVersion)
        implementation("io.exoquery:exoquery-plugin-kotlin:${version}")
        // Kotlin compiler embeddable needed by compile-testing in some environments
        implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
      }
    }
  }
}

tasks.named<Test>("jvmTest") {
  useJUnitPlatform()
  filter { isFailOnNoMatchingTests = false }
  testLogging {
    showExceptions = true
    showStandardStreams = true
    events = setOf(
      org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
      org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
    )
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}
