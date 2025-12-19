plugins {
  id("conventions-multiplatform")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotest) apply false
  id("io.exoquery.exoquery-plugin")

  // NEED serialization to be able to read the encoded XR, in the future the GradlePlugin should probably add this to the classpath
  alias(libs.plugins.kotlinx.serialization)
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
  pluginManager.apply("io.kotest")
}

repositories {
  mavenCentral()
  mavenLocal()
}

kotlin {
  compilerOptions {
    optIn.add("io.exoquery.annotation.ExoInternal")
  }

  jvm()
  linuxX64()
  //@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  //wasmJs {
  //    nodejs()
  //}
  //iosX64()

  sourceSets {
    val commonMain by getting {
    }

    val commonTest by getting {
      kotlin.srcDir("${layout.buildDirectory}/generated/ksp/metadata/commonMain/kotlin")
      dependencies {
        // TODO probably the gradle plugin should add these? They don't seem to be just inherited from the exoquery-engine project
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)

        implementation(libs.kotest.framework)
        implementation(libs.kotest.assertions)

        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))

      }
    }

    val jvmMain by getting {
      dependencies {
        // Want to have the full pprint in the JVM to do reflective deep-object diffs
        implementation(libs.pprint.kotlin)

        implementation(libs.koog.agents)

        implementation(libs.exposed.core)
        implementation(libs.exposed.dao)
        implementation(libs.exposed.jdbc)

        implementation(libs.postgresql)
        implementation(libs.embedded.postgres)
        implementation(libs.embedded.postgres.binaries)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.framework.engine)
      }
    }
  }
}

//tasks.withType<GenerateProjectFileTask> {
//    gradleArgs = "--info"
//}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs = listOf(
      "-Xcontext-parameters",
      "-P",
      "plugin:io.exoquery.terpal-plugin:traceWrappers=true"
    )
    optIn.add("io.exoquery.annotation.ExoInternal")

    // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
    // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
    java {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}

exoQuery {
//  // CANNOT HAVE LINEBREAKS, will throw `Wrong plugin option format: null, should be plugin:<pluginId>:<optionName>=<value>`
//  outputString.set("%{br}==== Compiled %{kind} in %{total}ms: ====%{br}%{sql}")
  codegenDrivers.add(libs.postgresql.get().toString())
  this.enableCodegenAI = true
  //enableCrossFileStore = false // uncomment to disable cross-file storage (enabled by default)
}


dependencies {
  // These settings are set in GradlePlugin.kt. Otherwise would need to set them here:
  //kotlinNativeCompilerPluginClasspath("io.exoquery:terpal-runtime:${...}")
  //kotlinNativeCompilerPluginClasspath("io.exoquery:decomat-core-jvm:${...}")
}

tasks.named<Test>("jvmTest") {
  useJUnitPlatform {
    includeEngines("kotest")
  }
  filter {
    isFailOnNoMatchingTests = false
  }
  testLogging {
    showExceptions = true
    showStandardStreams = true
    events = setOf(
      org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
      org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
    )
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }

  // Add Kotest system properties for test discovery
  systemProperty("kotest.framework.discovery.enabled", "true")
  systemProperty("kotest.framework.classpath.scanning.config.disable", "false")
  systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "false")

  // Additional Kotest configuration
  systemProperty("kotest.framework.timeout", "10000")
  systemProperty("kotest.framework.dump.config", "true")
  systemProperty("kotest.framework.parallelism", "1")
}
