plugins {
  id("conventions-multiplatform-nativeonly")
  kotlin("multiplatform") version "2.2.0"
  alias(libs.plugins.kotest)
  id("io.exoquery.exoquery-plugin")

  // NEED serialization to be able to read the encoded XR, in the future the GradlePlugin should probably add this to the classpath
  kotlin("plugin.serialization") version "2.2.0"
}

version = extra["controllerProjectVersion"].toString()

kotlin {
  jvmToolchain(11)

  jvm {
    compilerOptions {
      java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
      }
    }
  }
  linuxX64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.controller.core)
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.protobuf)
      }
    }

    val jvmMain by getting {
      dependencies {
        api(libs.controller.jdbc)
      }
    }

    nativeMain.dependencies {
      dependencies {
        api(libs.controller.native)
      }
    }


    // This is a library that tests will reuse so we need to put everything into the commonMain part
    // KMP doesn't allow reusing commonTest code even in upstream commonTest dependencies
    //val commonTest by getting {
    //    dependencies {
    //    }
    //}
  }
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  // These settings are set in GradlePlugin.kt. Otherwise would need to set them here:
  //kotlinNativeCompilerPluginClasspath("io.exoquery:terpal-runtime:${...}")
  //kotlinNativeCompilerPluginClasspath("io.exoquery:decomat-core-jvm:${...}")
}
