import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  id("conventions")
  kotlin("multiplatform")
  id("com.android.library")
  kotlin("plugin.serialization") version "2.2.0"
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")

  // Already on the classpath
  //id("org.jetbrains.kotlin.android") version "2.2.0"

  id("com.google.devtools.ksp") version "2.2.0-2.0.2"
  id("androidx.room") version "2.7.1"
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
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        implementation("androidx.sqlite:sqlite-framework:2.4.0")

        val roomVersion = "2.7.1"
        implementation("androidx.room:room-runtime:${roomVersion}")
        implementation("androidx.room:room-ktx:${roomVersion}")
      }
    }

    val androidInstrumentedTest by getting {
      dependencies {
        implementation(project(":exoquery-runner-core"))

        //implementation(kotlin("test-junit"))
        //implementation("junit:junit:4.13.2")
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))

        implementation("org.robolectric:robolectric:4.13")
        implementation("androidx.test:core:1.6.1")
        implementation("androidx.test:runner:1.6.1")
        implementation("app.cash.sqldelight:android-driver:2.0.2")
        implementation("androidx.test.ext:junit:1.1.3")
        implementation("androidx.test.espresso:espresso-core:3.4.0")
      }
    }

    val androidUnitTest by getting {
      dependencies {
        implementation(project(":exoquery-runner-core"))

        //implementation(kotlin("test-junit"))
        //implementation("junit:junit:4.13.2")
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))

        implementation("org.robolectric:robolectric:4.13")
        implementation("androidx.test:core:1.6.1")
        implementation("androidx.test:runner:1.6.1")
        implementation("app.cash.sqldelight:android-driver:2.0.2")
        implementation("androidx.test.ext:junit:1.1.3")
        implementation("androidx.test.espresso:espresso-core:3.4.0")
      }
    }
  }

}

dependencies {
  commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}

repositories {
  mavenCentral()
  mavenLocal()
}
