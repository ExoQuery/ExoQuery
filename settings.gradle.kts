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


includeBuild("exoquery-engine")
includeBuild("exoquery-plugin-kotlin")

include("exoquery-runner-core")

val isCI: Boolean = settings.extra.has("isCI")
val isLocal = !isCI
val isLinux = System.getProperty("os.name").lowercase().contains("linux")

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
include("exoquery-runner-r2dbc")
include("testing")
include("testing-compile")

rootProject.name = "exoquery"
