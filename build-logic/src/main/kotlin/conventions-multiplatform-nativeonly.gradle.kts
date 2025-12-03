import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  id("conventions")
  kotlin("multiplatform")
}

/*
This thing is necessary because we can't use conventions-multiplatform with exoquery-runner-native because conventions-multiplatform
enables JVM by default (I need this for testing an exoquery-engine functionality) which we can't do for exoquery-runner-native as it
has no JVM-target native libraries i.e. (Terpal's) controller-native doesn't work for JVM (that's what controller-jvm is for)
and the sqlite libraries that it depends on don't have JVM targets either.
 */

kotlin {

  val isCI = project.hasProperty("isCI")
  // I.e. set this environment variable specifically to true to build (most) targets
  val fullLocal = !isCI && ((System.getenv("EXOQUERY_FULL_LOCAL")?.toBoolean() ?: false) || project.hasProperty("isLocalMultiplatform"))

  // Github sees these but no targets enabled below. Not sure why, maybe it doesn't see isCI. Need to investigage
  linuxX64()
  if (HostManager.hostIsMac) {
    macosX64()
    macosArm64()
  }

  val linuxCI = HostManager.hostIsLinux && isCI
  val mingCI = HostManager.hostIsMingw && isCI
  val macCI = HostManager.hostIsMac && isCI

  println("--------- Host Family: ${HostManager.host.family.name}, isCI: $isCI ---------")

  // Disabling JS for now because Kotest breaks on nested-tests (see here: https://github.com/kotest/kotest/pull/3913)
  // If you actually run this command:
  //   ./gradlew :testing:jsNodeTest --info
  // You get errors like this:
  // StatefulTransformerSpec.transforms asts using a transformation state[js, node, ChromeHeadless134.0.0.0, Linuxx86_64] FAILED
  //    IllegalStateException: Nested tests are not supported
  // Need to look into how we can flatten the tests.
  // One possibility is writing our own spec like this: https://gist.github.com/BenWoodworth/0fd0e463fdfc71c30f2a114da5c14672
  // Or using this library: https://opensavvy.gitlab.io/groundwork/prepared/docs/
  // Should look more into this.
  //if (linuxCI)
  //  js(IR) {
  //    browser()
  //    nodejs {
  //      testTask {
  //        useKarma {
  //          useChromeHeadless()
  //        }
  //      }
  //    }
  //  }

  if (linuxCI) linuxArm64()
  // LinuxCI Needs to know the OSX and MingW dependencies exist even though it does not build them so it can put them in the mmodules-list metadata in maven-central.
  if (linuxCI || macCI) macosX64()
  if (linuxCI || macCI) macosArm64()
  if (linuxCI || macCI) iosX64()
  if (linuxCI || macCI) iosArm64()
  if (linuxCI || macCI) tvosX64()
  if (linuxCI || macCI) tvosArm64()
  if (linuxCI || macCI) watchosX64()
  if (linuxCI || macCI) watchosArm32()
  if (linuxCI || macCI) watchosArm64()
  if (linuxCI || macCI) iosSimulatorArm64()
  //if (linux || mac) watchosDeviceArm64()
  //if (linux || mac) watchosSimulatorArm64()
  //if (linux || mac) tvosSimulatorArm64()
  if (linuxCI || mingCI) mingwX64()
}
