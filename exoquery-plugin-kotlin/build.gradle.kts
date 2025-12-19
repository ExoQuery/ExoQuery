import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  id("maven-publish")
  id("conventions")
  id("publish-jvm")
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.kotlin.kapt)
}

repositories {
  mavenCentral()
  mavenLocal()
  maven("https://s01.oss.sonatype.org/content/repositories/releases")
}

version = extra["pluginProjectVersion"].toString()
val runtimeVersion = extra["pluginProjectVersion"].toString()

kotlin {
  jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
  compilerOptions {
      freeCompilerArgs.add("-Xcontext-parameters")
      optIn.addAll(
          "io.exoquery.annotation.ExoInternal",
          "kotlinx.serialization.ExperimentalSerializationApi",
          "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI"
      )

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

dependencies {
  api("io.exoquery:exoquery-engine:${runtimeVersion}")
  implementation(libs.mapdb)

  compileOnly(libs.kotlin.compiler.embeddable)
  kapt(libs.auto.service)
  compileOnly(libs.auto.service.annotations)
  compileOnly(libs.koog.agents)

  //implementation("com.facebook:ktfmt:0.43") <-- requires kotlin-test:1.6.10 so need to add to the GradlePlugin libs if we want to use this

  api(libs.decomat.core)
  api(libs.pprint.kotlin)
  api(libs.sql.formatter)
  // For the plugin to use Reflect it should be fine because the plugin dependencies do not get packed into the runtime jar
  api(kotlin("reflect"))
}
