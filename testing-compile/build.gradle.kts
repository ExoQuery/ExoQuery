plugins {
  id("conventions")
  kotlin("multiplatform") version "2.2.20"
  alias(libs.plugins.kotest)
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
        //{
          // exclude "kotlin-compiler-embeddable"
          //exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
          //exclude(group = "org.jetbrains.kotlin", module = "kotlin-annotation-processing-embeddable")
        //}
        // val pluginVersion = extra["pluginProjectVersion"].toString()
        // implementation("io.exoquery:exoquery-plugin-kotlin:$pluginVersion")
        // Use the compiler plugin from local maven if available (matches repo's pluginProjectVersion)
        implementation("io.exoquery:exoquery-plugin-kotlin:2.2.20-2.0.0.PL.1")
        // Kotlin compiler embeddable needed by compile-testing in some environments
        implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")


        // TODO need to figure out where these are coming from and use them instead of the embedded libraries in order to have a applies-to-apples comparison with dependencies
        // kotlin-compiler-ide = { group = "org.jetbrains.kotlin", name = "kotlin-compiler-for-ide", version.ref = "kotlinIdeVersion" }
        //implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:1.9.20-506") {
        //  isTransitive = false
        //}

        //implementation("org.jetbrains.kotlin:idea:231-1.9.20-506-IJ8109.175") {  isTransitive = false }
        //implementation("org.jetbrains.kotlin:core:231-1.9.20-506-IJ8109.175")
        //implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
        //implementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:${libs.versions.kotlin.get()}")
      }
    }
  }
}

repositories {
  mavenCentral()
  mavenLocal()
  gradlePluginPortal()

  maven("https://repo.spring.io/snapshot")
  maven("https://repo.spring.io/milestone")
  maven("https://redirector.kotlinlang.org/maven/kotlin-ide")
  maven("https://redirector.kotlinlang.org/maven/dev")
  maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
  maven("https://www.myget.org/F/rd-snapshots/maven/")
  maven("https://redirector.kotlinlang.org/maven/kotlin-ide")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.named<Test>("jvmTest") {
  dependsOn(":testing-compile-dependencies:copyDependencies")
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
