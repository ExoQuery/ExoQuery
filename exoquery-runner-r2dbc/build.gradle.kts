plugins {
  id("conventions")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")
  alias(libs.plugins.kotlinx.serialization)
}

repositories {
  mavenCentral()
  mavenLocal()
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

kotlin {
  compilerOptions {
    optIn.add("io.exoquery.annotation.ExoInternal")
  }

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
        api(libs.controller.r2dbc)
        api(project(":exoquery-runner-core"))

        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)
        api(libs.kotlinx.coroutines.core)
        implementation(libs.typesafe.config)
      }
    }
    jvmTest {
      kotlin.srcDir("src/test/kotlin")
      resources.srcDir("src/test/resources")

      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        // Embedded Postgres and driver for R2DBC Postgres tests
        implementation(libs.embedded.postgres)
        implementation(libs.embedded.postgres.binaries)
        // R2DBC Postgres driver
        api("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
        // R2DBC SQL Server driver
        api("io.r2dbc:r2dbc-mssql:1.0.2.RELEASE")
        // R2DBC MySQL driver
        api("io.asyncer:r2dbc-mysql:1.1.0")
        // R2DBC H2 driver
        api("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
        // R2DBC Oracle driver
        api("com.oracle.database.r2dbc:oracle-r2dbc:1.2.0")
      }
    }
  }
}

tasks.named<Test>("jvmTest") {
  useJUnitPlatform()
  filter { isFailOnNoMatchingTests = false }
}
