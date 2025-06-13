pluginManagement {
  includeBuild("exoquery-plugin-gradle")
  includeBuild("build-logic")

  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://s01.oss.sonatype.org/service/local/repositories/releases/content/")
    mavenLocal()
    google()
  }
}


// Need this or you will get:
// Using a toolchain installed via auto-provisioning, but having no toolchain repositories configured. This behavior is deprecated. Consider defining toolchain download repositories, otherwise the build might fail in clean environments; see https://docs.gradle.org/8.6/userguide/toolchains.html#sub:download_repositories
// at org.gradle.jvm.toolchain.internal.JavaToolchainQueryService.warnIfAutoProvisionedToolchainUsedWithoutRepositoryDefinitions(JavaToolchainQueryService.java:171)
// at org.gradle.jvm.toolchain.internal.JavaToolchainQueryService.lambda$findInstalledToolchain$6(JavaToolchainQueryService.java:157)
// at java.base/java.util.Optional.map(Optional.java:260)
// at org.gradle.jvm.toolchain.internal.JavaToolchainQueryService.findInstalledToolchain(JavaToolchainQueryService.java:156)
dependencyResolutionManagement {
  repositories {
    mavenCentral() // Required for toolchain resolution
  }
}

includeBuild("exoquery-engine")
includeBuild("exoquery-plugin-kotlin")

include("exoquery-runner-core")

val isCI: Boolean = settings.extra.has("isCI")
val isLocal = !isCI
val isLinux = System.getProperty("os.name").toLowerCase().contains("linux")

// If it's a local build or we're building the CI onl linux include the android project
if (isLocal || isLinux) {
  val buildLabel =
    if (isLocal) "Local"
    else if (isLinux) "Linux"
    else "Unknown"

  println("------------------- Building JDBC/Android Libraries for ${buildLabel} Build -------------------")
  include("exoquery-runner-jdbc")
  include("exoquery-runner-android")
}

include("exoquery-runner-native")
include("testing")

rootProject.name = "exoquery"
