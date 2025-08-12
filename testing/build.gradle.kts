plugins {
  id("conventions-multiplatform")
  kotlin("multiplatform") version "2.2.0"
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")

  // NEED serialization to be able to read the encoded XR, in the future the GradlePlugin should probably add this to the classpath
  kotlin("plugin.serialization") version "2.2.0"
}

kotlin {
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
      kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin")
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
        implementation("io.exoquery:pprint-kotlin:3.0.0")

        implementation("ai.koog:koog-agents:0.3.0")

        implementation("org.jetbrains.exposed:exposed-core:0.60.0")
        implementation("org.jetbrains.exposed:exposed-dao:0.60.0")
        implementation("org.jetbrains.exposed:exposed-jdbc:0.60.0")

        implementation("org.postgresql:postgresql:42.7.3")
        implementation("io.zonky.test:embedded-postgres:2.0.7")
        implementation("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64:16.2.0")
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation("io.kotest:kotest-runner-junit5:6.0.0.M1")
        implementation("io.kotest:kotest-framework-api:6.0.0.M1")
        implementation("io.kotest:kotest-framework-discovery:6.0.0.M1")
        implementation("io.kotest:kotest-framework-engine:6.0.0.M1")
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
      "-Xcontext-receivers",
      "-P",
      "plugin:io.exoquery.terpal-plugin:traceWrappers=true"
    )
    // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
    // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
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

exoQuery {
//  // CANNOT HAVE LINEBREAKS, will throw `Wrong plugin option format: null, should be plugin:<pluginId>:<optionName>=<value>`
//  outputString.set("%{br}==== Compiled %{kind} in %{total}ms: ====%{br}%{sql}")
  codegenDrivers.add("org.postgresql:postgresql:42.7.3")
  this.enableCodegenAI = true
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
