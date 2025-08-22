import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// In certain situations where tests are being rapidly iterated
// and the KSP artifacts are already produced we may want to sepectively disable KSP
// (i.e. while tests are being rewritten and other which requires XR changes -which is mostly where ExoQuery uses Decomat's KSP functionality-
// we can disable the KSP-based decomat code generation)
val kspEnabled = true

plugins {
  kotlin("multiplatform") version "2.2.0"
  id("maven-publish")

  id("io.exoquery.terpal-plugin") version "2.2.0-2.0.0.PL"

  id("conventions-multiplatform")
  id("publish")

  // can remove this if kspEnabled is false (this the variable kspEnabled cannot be used here)
  id("com.google.devtools.ksp") version "2.2.0-2.0.2"

  kotlin("plugin.serialization") version "2.2.0"
}

version = extra["pluginProjectVersion"].toString()

dependencies {
  if (kspEnabled)
    add("kspCommonMainMetadata", "io.exoquery:decomat-ksp:1.0.0")


  // Double ksp entry causes strange overrides so do not use it
  //if (kspEnabled) add("kspJvm", "io.exoquery:decomat-ksp:1.0.0")
  commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
  }

  jvm()
  linuxX64()
  mingwX64()

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin")

      dependencies {
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)

        // Actually this is going to be 0.0.5 - using an unpublished one now
        // No reflection in pprint-kmp
        api("io.exoquery:pprint-kotlin-kmp:3.0.0")
        // Actually this is going to be 0.0.5 - using an unpublished one now
        api("io.exoquery:decomat-core:1.0.0")
        api("io.exoquery:terpal-runtime:2.2.0-2.0.0.PL")
        // This is a java-only library, I have no idea who it can even be here.
        // maybe if I actually attempt to use it in KMP mode in a non-java target it will actually fail
        api("com.github.vertical-blank:sql-formatter:2.0.4")

        // JB Annotations are now multiplatform
        implementation("org.jetbrains:annotations:26.0.2")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.framework)
        implementation(libs.kotest.assertions)
        //implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        //jvm-only setup for kotest, need to find the KMP way
        //implementation("io.kotest:kotest-runner-junit5:5.8.0")
      }
    }

    val jvmMain by getting {
      dependencies {
        api("com.github.vertical-blank:sql-formatter:2.0.4")
        compileOnly(libs.koog.agents)
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(libs.kotest.runner.junit5)
      }
    }
  }

  jvmToolchain(17)
}

if (kspEnabled)
  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
      dependsOn("kspCommonMainKotlinMetadata")
    }
  }

// Add explicit dependency for KSP JVM tasks if they exist
// if (kspEnabled)
//   tasks.matching { task -> task.name.startsWith("ksp") && task.name.contains("Jvm") }.configureEach {
//     dependsOn("kspCommonMainKotlinMetadata")
//   }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
    freeCompilerArgs.add("-opt-in=io.exoquery.annotation.ExoInternal")

    // DOesn't work in KMP
    //freeCompilerArgs.add("-Xcontext-receivers")
    // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
    // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
    java {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
    // If I remove this I get:
    //  'compileJava' task (current target is 11) and 'kaptGenerateStubsKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
    // Not sure why
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

repositories {
  mavenCentral()
  mavenLocal()
  maven("https://s01.oss.sonatype.org/content/repositories/releases")
}

if (kspEnabled) {
  ksp {
    //arg("matchableName", "Mat")
    //arg("componentName", "Slot")
    //arg("middleComponentName", "MSlot")
    //arg("constructorComponentName", "CS")
    arg("fromHereFunctionName", "cs")
    arg("fromFunctionName", "csf")
    arg("renderAdtFunctions", "true")
    arg("renderFromHereFunction", "false")
    java {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}

tasks.named<Test>("jvmTest") {
  useJUnitPlatform()
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
}

// Add the kspCommonMainKotlinMetadata dependency to sourcesJar tasks if needed
// for example:
//tasks.findByName("jsSourcesJar")?.dependsOn("kspCommonMainKotlinMetadata")
//tasks.findByName("jvmSourcesJar")?.dependsOn("kspCommonMainKotlinMetadata")
//tasks.findByName("linuxX64SourcesJar")?.dependsOn("kspCommonMainKotlinMetadata")
//tasks.findByName("mingwX64SourcesJar")?.dependsOn("kspCommonMainKotlinMetadata")
//tasks.findByName("sourcesJar")?.dependsOn("kspCommonMainKotlinMetadata")
// Preferably we can just use the following to get every target:

if (kspEnabled) {
  tasks.filter { it.name == "sourcesJar" || it.name.endsWith("SourcesJar") }.forEach {
    it.dependsOn("kspCommonMainKotlinMetadata")
  }
}
