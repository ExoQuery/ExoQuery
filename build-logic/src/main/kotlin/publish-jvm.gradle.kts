import org.jetbrains.dokka.gradle.AbstractDokkaTask

plugins {
  kotlin("jvm") apply false
  id("conventions")
  `java-library`
  `maven-publish`
  signing
  id("io.github.gradle-nexus.publish-plugin")
  id("org.jetbrains.dokka")
}

// nexusPublishing {
//   val user = System.getenv("SONATYPE_USERNAME")
//   val pass = System.getenv("SONATYPE_PASSWORD")
//
//   repositories {
//     sonatype {
//       nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
//       snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//       username.set(user)
//       password.set(pass)
//     }
//   }
// }

apply(plugin = "io.github.gradle-nexus.publish-plugin")
apply(plugin = "kotlin")
apply(plugin = "maven-publish")

repositories {
  mavenCentral()
  maven(url = "https://plugins.gradle.org/m2/")
}

tasks {
  compileKotlin {
    compilerOptions {
      suppressWarnings.set(true)
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }

  compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }

  compileTestKotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }

  compileTestJava {
    targetCompatibility
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

// Can also do this instead of above, for more details
// see here: https://stackoverflow.com/questions/69079963/how-to-set-compilejava-task-11-and-compilekotlin-task-1-8-jvm-target-com
//  java {
//    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
//  }

// Disable publishing for decomat examples
tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf {
    publication.artifactId != "decomat-examples"
  }
}

val varintName = project.name

apply {
  plugin("org.jetbrains.kotlin.jvm")
  plugin("org.jetbrains.dokka")
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
tasks {
  val javadocJar by creating(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.get().outputDirectory)
  }
  val sourcesJar by creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
  }
}

/*
Cannot use `publications.withType<MavenPublication> { ... } ` approach using kotlin-jvm unlike KMP
it seems that in KMP the publication is is already created internally and you just have to configure it.
  */
publishing {
  val user = System.getenv("SONATYPE_USERNAME")
  val pass = System.getenv("SONATYPE_PASSWORD")

  repositories {
    maven {
      name = "Oss"
      setUrl {
        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/maven2/"
      }
      credentials {
        username = user
        password = pass
      }
    }
  }

  fun MavenPom.configureMavenCentralMetadata() {
    name.set("exoquery")
    description.set("ExoQuery - Custom string-interpolation for Kotlin")
    url.set("https://github.com/exoquery/exoquery")

    licenses {
      license {
        name.set("The Apache Software License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("repo")
      }
    }

    developers {
      developer {
        name.set("Alexander Ioffe")
        email.set("deusaquilus@gmail.com")
        organization.set("github")
        organizationUrl.set("http://www.github.com")
      }
    }

    scm {
      url.set("https://github.com/exoquery/exoquery/tree/main")
      connection.set("scm:git:git://github.com/ExoQuery/ExoQuery.git")
      developerConnection.set("scm:git:ssh://github.com:ExoQuery/ExoQuery.git")
    }
  }

  publications {
    create<MavenPublication>("mavenJava") {
      from(components["kotlin"])
      artifactId = varintName

      artifact(tasks["javadocJar"])
      artifact(tasks["sourcesJar"])

      // The below configuration clause should probably take care of this. Can try to remove it
      pom.configureMavenCentralMetadata()
    }
  }

  // Regular kotlin `kotlin("jvm")` builds need a create<MavenPublication>("mavenJava") while odd ones
  // like the exoquery-plugin-gradle build `id("java-gradle-plugin")` type already has a publication.
  // that means we need to configuration the licenses/developers/scm for each kind
  publications.withType<MavenPublication>().configureEach {
    pom.configureMavenCentralMetadata()
  }
}

val isCI = project.hasProperty("isCI")
val isLocal = !isCI
val noSign = project.hasProperty("nosign")
val doNotSign = isLocal || noSign

signing {
  // Sign if we're not doing a local build and we haven't specifically disabled it
  if (!doNotSign) {
    val signingKeyRaw = System.getenv("NEW_SIGNING_KEY_ID_BASE64")
    if (signingKeyRaw == null) error("ERROR: No Signing Key Found")
    // Seems like the right way was to have newlines after all the exported (ascii armored) lines
    // and you can put them into the github-var with newlines but if you
    // include the "-----BEGIN PGP PRIVATE KEY BLOCK-----" and "-----END PGP PRIVATE KEY BLOCK-----"
    // parts with that then errors happen. Have a look at https://github.com/gradle/gradle/issues/15718 for more detail
    // Ultimately however `iurysza` is only partially correct and they key-itself does not need to be escaped
    // and can be put into a github-var with newlines.
    val signingKey =
      "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n${signingKeyRaw}\n-----END PGP PRIVATE KEY BLOCK-----"
    useInMemoryPgpKeys(
      System.getenv("NEW_SIGNING_KEY_ID_BASE64_ID"),
      signingKey,
      System.getenv("NEW_SIGNING_KEY_ID_BASE64_PASS")
    )
    sign(publishing.publications)
  }
}

// If not present :exoquery-plugin-gradle:publishToMavenLocal is called it will fail with:
// Execution failed for task ':exoquery-plugin-gradle:signPluginMavenPublication'.
// e.g. if ./deploy-local.sh is called.
if (isLocal) {
  tasks.withType<Sign>().configureEach {
    enabled = false
  }
}

// Fix for Kotlin issue: https://youtrack.jetbrains.com/issue/KT-61313
tasks.withType<Sign>().configureEach {
  val pubName = name.removePrefix("sign").removeSuffix("Publication")

  // These tasks only exist for native targets, hence findByName() to avoid trying to find them for other targets

  // Task ':linkDebugTest<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
  tasks.findByName("linkDebugTest$pubName")?.let {
    mustRunAfter(it)
  }
  // Task ':compileTestKotlin<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
  tasks.findByName("compileTestKotlin$pubName")?.let {
    mustRunAfter(it)
  }
}

// Was having odd issues happening in CI releases like this:
// e.g. Task ':pprint-kotlin-core:publish<AndroidNativeArm32>PublicationToOssRepository' uses this output of task ':pprint-kotlin-core:sign<AndroidNativeArm64>Publication' without declaring an explicit or implicit dependency.
// I tried a few things that caused other issues. Ultimately the working solution I got from here:
// https://github.com/gradle/gradle/issues/26091#issuecomment-1722947958
tasks.withType<AbstractPublishToMaven>().configureEach {
  val signingTasks = tasks.withType<Sign>()
  mustRunAfter(signingTasks)
}
