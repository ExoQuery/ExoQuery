plugins {
  id("conventions")
  kotlin("multiplatform") version "2.2.20"
}

version = extra["controllerProjectVersion"].toString()

group = "io.exoquery"

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
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val kotlinCompilerPluginDependency: Configuration by configurations.creating {
  isTransitive = false
  isCanBeResolved = true
  isCanBeConsumed = false
}

kotlin {
  jvmToolchain(17)
  jvm()
  sourceSets {
    val jvmMain by getting {
      dependencies {
        // kotlinx.serialization core/protobuf/datetime
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0") {
          isTransitive = false
        }
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:1.9.0") {
          isTransitive = false
        }
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") {
          isTransitive = false
        }

        // Kotlin serialization compiler plugin (as a dependency artifact)
        implementation("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin:2.2.20") {
          isTransitive = false
        }

        // ExoQuery plugin and runtime
        implementation("io.exoquery:exoquery-plugin-kotlin:2.2.20-2.0.0.PL.2") {
          isTransitive = false
        }
        implementation("io.exoquery:exoquery-engine:2.2.20-2.0.0.PL.2") {
          isTransitive = false
        }

        // Decomat, Terpal runtime, SQL formatter
        implementation("io.exoquery:decomat-core-jvm:1.0.0") {
          isTransitive = false
        }
        implementation("io.exoquery:terpal-runtime:2.2.20-2.0.1.PL") {
          isTransitive = false
        }
        implementation("com.github.vertical-blank:sql-formatter:2.0.4") {
          isTransitive = false
        }

        // pprint variants
        implementation("io.exoquery:pprint-kotlin-core-jvm:3.0.0") {
          isTransitive = false
        }
        implementation("io.exoquery:pprint-kotlin-kmp-jvm:3.0.0") {
          isTransitive = false
        }
        implementation("io.exoquery:pprint-kotlin:3.0.0") {
          isTransitive = false
        }

        // Logging and utilities
        implementation("net.logstash.logback:logstash-logback-encoder:8.1") {
          isTransitive = false
        }
        implementation("org.jetbrains.intellij.deps:trove4j:1.0.20221201") {
          isTransitive = false
        }

        // Kotlin reflect and stdlib bundle (explicit artifacts)
        implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.20") {
          isTransitive = false
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20") {
          isTransitive = false
        }
        //implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.20")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20") {
          isTransitive = false
        }

        // Kotlin compiler and script runtime
        //implementation("org.jetbrains.kotlin:kotlin-compiler:2.2.20") {
        //  isTransitive = false
        //}
        implementation("org.jetbrains.kotlin:kotlin-script-runtime:2.2.20") {
          isTransitive = false
        }

        // Kotlin compiler for IDE
        implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:1.9.20-506") {
          isTransitive = false
        }
      }
    }
  }
}

val copiedLibs = layout.projectDirectory.dir("target/dependencies")

val copyDependencies by tasks.register<Sync>("copyDependencies") {
  from(configurations.named("jvmCompileClasspath"))
  into(copiedLibs)
}
