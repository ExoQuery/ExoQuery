import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("conventions")
  kotlin("multiplatform")
  id("com.android.library")
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")

  // Already on the classpath
  //id("org.jetbrains.kotlin.android") version "2.3.0"

  alias(libs.plugins.ksp)
  alias(libs.plugins.androidx.room)
}

repositories {
  mavenCentral()
  mavenLocal()
}

version = extra["controllerProjectVersion"].toString()

android {
  namespace = "io.exoquery"

  compileSdk = 34
  defaultConfig {
    minSdk = 26
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions {
    targetSdk = 34
    unitTests.isIncludeAndroidResources = true
    //unitTests.all {
    //  it.useJUnitPlatform()
    //}
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging {
    resources {
      // keep the first copy, ignore the rest
      pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"

      // (optional) while you’re here, silence other common META-INF clashes
      excludes += setOf(
        "META-INF/LICENSE*", "META-INF/NOTICE*",
        "META-INF/AL2.0",    "META-INF/LGPL2.1"
      )
    }
  }
}

room {
  schemaDirectory(layout.projectDirectory.dir("schemas"))
  generateKotlin = true          // emit Kotlin instead of Java stubs
}

kotlin {
  androidTarget {
    compilations.all {
      compileTaskProvider {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
        }
      }
    }
    publishLibraryVariants("release", "debug")
  }

  sourceSets {

    val androidMain by getting {
      // TODO Should NOT Keep this here (or even the androidx.room plugin even if we want a dynamic path for room, that stuff doesn't need androidx-room and only really needs to be in Terpal anyway).
      //      Create a separate project for room testing e.g. `testing-room` to have these things
      kotlin.srcDir("src/exoroom/kotlin")

      dependencies {
        api(project(":exoquery-runner-core"))
        api(libs.controller.android)

        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)
        api(libs.kotlinx.coroutines.core)
        implementation(libs.androidx.sqlite.framework)

        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
      }
    }

    // Common test dependencies to avoid duplication
    val androidTestDependencies: DependencyHandler.() -> Unit = {
        implementation(project(":exoquery-runner-core"))
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation(libs.robolectric)
        implementation(libs.androidx.test.core)
        implementation(libs.androidx.test.runner)
        implementation(libs.sqldelight.android.driver)
        implementation(libs.androidx.test.junit)
        implementation(libs.androidx.test.espresso.core)
      }

    val androidInstrumentedTest by getting {
      dependencies(androidTestDependencies)
    }

    val androidUnitTest by getting {
      dependencies(androidTestDependencies)
      }
    }
  }

dependencies {
  commonMainApi(libs.kotlinx.datetime)
}
