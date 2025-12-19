import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// In certain situations where tests are being rapidly iterated
// and the KSP artifacts are already produced we may want to sepectively disable KSP
// (i.e. while tests are being rewritten and other which requires XR changes -which is mostly where ExoQuery uses Decomat's KSP functionality-
// we can disable the KSP-based decomat code generation)
val kspEnabled = true

plugins {
  id("conventions-multiplatform")
  // can remove this if kspEnabled is false (this the variable kspEnabled cannot be used here)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.terpal)
  id("publish")
}

version = extra["pluginProjectVersion"].toString()

dependencies {
  if (kspEnabled)
    add("kspCommonMainMetadata", libs.decomat.ksp)

  // Double ksp entry causes strange overrides so do not use it
  //if (kspEnabled) add("kspJvm", libs.decomat.ksp)
  commonMainApi(libs.kotlinx.datetime)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xwhen-guards")
    optIn.addAll(
        "io.exoquery.annotation.ExoInternal",
        "kotlinx.serialization.ExperimentalSerializationApi"
    )
  }

  jvm()
  linuxX64()
  //mingwX64()

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir("${layout.buildDirectory.get().asFile}/generated/ksp/metadata/commonMain/kotlin")

      dependencies {
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)

        // Actually this is going to be 0.0.5 - using an unpublished one now
        // No reflection in pprint-kmp
        api(libs.pprint.kotlin.kmp)
        // Actually this is going to be 0.0.5 - using an unpublished one now
        api(libs.decomat.core)
        api(libs.terpal.runtime)
        // This is a java-only library, I have no idea who it can even be here.
        // maybe if I actually attempt to use it in KMP mode in a non-java target it will actually fail
        api(libs.sql.formatter)

        // JB Annotations are now multiplatform
        implementation(libs.jb.annotations)
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(libs.kotest.framework)
        implementation(libs.kotest.assertions)
        //implementation(libs.kotlinx.datetime)
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        //jvm-only setup for kotest, need to find the KMP way
        //implementation(libs.kotest.runner.junit5)
      }
    }

    val jvmMain by getting {
      dependencies {
        api(libs.sql.formatter)
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
    compilerOptions {
      optIn.add("io.exoquery.annotation.ExoInternal")
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
    optIn.add("io.exoquery.annotation.ExoInternal")

    // DOesn't work in KMP
    //freeCompilerArgs.add("-Xcontext-parameters")
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
  tasks.filter { it.name == "sourcesJar" || it.name.endsWith("SourcesJar") || it.name.contains("dokkaHtml") }.forEach {
    it.dependsOn("kspCommonMainKotlinMetadata")
  }
}
