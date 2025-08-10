package io.exoquery

import io.exoquery.config.ExoCompileOptions
import io.exoquery.exoquery_plugin_gradle.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*


interface ExoQueryGradlePluginExtension {
    /** Override the string printed when a static query is executed use %total and %sql switches to specify */
    val outputString: Property<String>
    /** Whether to create target/generated/exo/.../_.queries.sql files or not */
    val queryFilesEnabled: Property<Boolean>
    /** Whether to print queries at compile-time or not (per the outputString syntax) */
    val queryPrintingEnabled: Property<Boolean>

    val codegenDrivers: ListProperty<String>
    val enableCodegenNamingAI: Property<Boolean>

    /**
     * Move the location of generated entities from `MyProject/build/generated/entities` to `MyProject/src/main/entities`
     * (whatever directory it is, it will be added to the source set). Alternatively, if you want to specify a custom path
     * you can use `codegenCustomPath` property (a relative path will be based on the JVM working directory of the compilation.
     *
     * Due to the non-deterministic nature of the LLMs, the generated code may change
     * between builds. This means that when using an AI for table/column naming you will want to
     * generate into a directory that is checked into version control as opposed to a location
     * that is cleaned up between builds (which is the default i.e. MyProject/build/generated/entities).
     * When this option is enabled, the generated code will be placed in MyProject/src/main/entities
     * instead and the `entities` directory will be added to the source set.
     * Note that if you turn on `enableCodegenNamingAI` then this option is automatically enabled.
     */
    val codegenIntoPermanentLocation: Property<Boolean>

    val codegenCustomPath: Property<String>

    val koogLibrary: Property<String>

}


class GradlePlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "io.exoquery.exoquery-plugin"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "io.exoquery",
    artifactId = "exoquery-plugin-kotlin",
    version = BuildConfig.VERSION
  )

  override fun apply(target: Project) {
    // This adds dependency until it has been configured by kotlin plugin
    // with the below code (starting with isMultiplatform) I don't think this is needed
    //target.plugins.withId("org.jetbrains.kotlin.jvm") {
    //    target.dependencies.add("implementation", "io.exoquery:exoquery-engine:${BuildConfig.VERSION}")
    //}

    val pluginExtConfig =
      target.extensions.create("exoQuery", ExoQueryGradlePluginExtension::class.java).apply {
        outputString.convention(ExoCompileOptions.DefaultOutputString)
        queryFilesEnabled.convention(ExoCompileOptions.DefaultQueryFilesEnabled)
        queryPrintingEnabled.convention(ExoCompileOptions.DefaultQueryPrintingEnabled)
        codegenDrivers.convention(ExoCompileOptions.DefaultJdbcDrivers)
      }

    val isMultiplatform = target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    val sourceSetName = if (isMultiplatform) "commonMain" else "main"
    val sourceSetApiConfigName =
      target.extensions.getByType(KotlinSourceSetContainer::class.java).sourceSets.getByName(sourceSetName).apiConfigurationName
    val runtimeDependencies = buildList {
      add(target.dependencies.create("io.exoquery:exoquery-engine:${BuildConfig.VERSION}"))
    }
    target.configurations.getByName(sourceSetApiConfigName).dependencies.addAll(runtimeDependencies)

    // Needed for the plugin classpath
    // Note that these do not bring in transitive dependencies so every transitive needs to be explicitly specified!
    target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
      target.addCompilerDependency("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
      target.addCompilerDependency("com.github.vertical-blank:sql-formatter:2.0.4")
      target.addCompilerDependency("io.exoquery:terpal-runtime:2.2.0-2.0.0.PL")
      target.addCompilerDependency("io.exoquery:pprint-kotlin-core-jvm:3.0.0")
      target.addCompilerDependency("io.exoquery:pprint-kotlin-kmp-jvm:3.0.0")
      // Fansi and core pprint ADT come from here
      target.addCompilerDependency("io.exoquery:pprint-kotlin:3.0.0")
      // in some places the compiler needs to print things, so the compiled plugin needs pprint
      target.addCompilerDependency("io.exoquery:exoquery-engine:${BuildConfig.VERSION}")
      target.addCompilerDependency("org.jetbrains.kotlinx:kotlinx-serialization-core:${BuildConfig.SERIALIZATION_VERSION}")
      target.addCompilerDependency("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${BuildConfig.SERIALIZATION_VERSION}")
      // Since this is compiler-plugin it works in the compiler which is written in Java so we use JVM dependencies
      target.addCompilerDependency("io.exoquery:decomat-core-jvm:${BuildConfig.DECOMAT_VERSION}")
    }

    target.afterEvaluate { project ->
      val kotlinExtension = target.extensions.findByType(KotlinProjectExtension::class.java)
      kotlinExtension?.let { decorateKotlinProject(it, project) }
    }
  }

  fun Project.addCompilerDependency(dependency: String) =
    dependencies.add("kotlinNativeCompilerPluginClasspath", dependency)

  private fun decorateKotlinProject(kotlin: KotlinProjectExtension, project: Project) {
    when (kotlin) {
      is KotlinSingleTargetExtension<*> -> {
        val targetsAndSourceSets =
          kotlin.target.compilations.map { compilation ->
            kotlin.target to compilation.defaultSourceSet
          }
        generatateDirs(project, targetsAndSourceSets)
      }
      is KotlinMultiplatformExtension -> {
        val targetsAndSourceSets =
          kotlin.targets.flatMap { target ->
            target.compilations.map { compilation ->
              target to compilation.defaultSourceSet
            }
          }
        generatateDirs(project, targetsAndSourceSets)
      }
    }
  }

  private fun generatateDirs(project: Project, targetsAndSourceSets: List<Pair<KotlinTarget, KotlinSourceSet>>) =
    targetsAndSourceSets.forEach { (target, sourceSet) ->
      val targetName = target.name
      val sourceSetName = sourceSet.name

      println("=========== Decorating [${project.name}] target:$targetName->sourceSet:$sourceSetName ===========")

      // Add the generated directory to kotlin source directories
      val generatedDir = project.generatedEntitiesKotlin(sourceSetName, targetName).get().asFile
      sourceSet.kotlin.srcDir(generatedDir)

      // Ensure the directory exists
      generatedDir.mkdirs()
    }


  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val ext = project.extensions.getByType(ExoQueryGradlePluginExtension::class.java)

    val configurationName = when (kotlinCompilation.target.platformType) {
      KotlinPlatformType.jvm -> "kotlinCompilerPluginClasspath"
      KotlinPlatformType.js -> "kotlinJsCompilerPluginClasspath"
      KotlinPlatformType.native -> "kotlinNativeCompilerPluginClasspath"
      KotlinPlatformType.common -> "kotlinCompilerPluginClasspath" // Common/metadata uses the same as JVM!
      else -> "kotlinCompilerPluginClasspath" // Fallback
    }

    ext.codegenDrivers.get().forEach { dependency ->
      println("[ExoQuery] adding driver dependency: $dependency")
      configurationName.let { project.dependencies.add(it, dependency) }

      if (ext.enableCodegenNamingAI.convention(false).get()) {
        println("[ExoQuery] LLM naming is enabled! Adding Koog dependency for LLM naming: ${ext.koogLibrary.get()}")
        // If LLM naming is enabled, we need to add the Koog dependency
        val koogLibrary = ext.koogLibrary.convention(BuildConfig.KOOG_LIBRARY).get()
        configurationName.let {
          project.dependencies.add(it, koogLibrary)
        }
      }
    }

    // ALSO needed for the plugin classpath
    kotlinCompilation.dependencies {
      api("io.exoquery:exoquery-engine:${BuildConfig.VERSION}")
    }

    val target = kotlinCompilation.target.name
    // This will always be the platform name e.g. jvm/linuxX64 etc... even if the files are in commonMain
    // because the builds don't actually build commonMain as a compile-target, only the actual platforms
    val sourceSetName = kotlinCompilation.defaultSourceSet.name

    val outputStringValue = ext.outputString.convention(ExoCompileOptions.DefaultOutputString).get()
    val queryFilesEnabled = ext.queryFilesEnabled.convention(ExoCompileOptions.DefaultQueryFilesEnabled).get()
    val queryPrintingEnabled = ext.queryPrintingEnabled.convention(ExoCompileOptions.DefaultQueryPrintingEnabled).get()

    val queriesBaseDir = project.generatedRootDir.get().dir("queries").asFile.absolutePath

    return project.provider {
      listOf(
        SubpluginOption("entitiesBaseDir", project.generatedEntitiesDir.get().asFile.absolutePath),
        SubpluginOption("generationDir", project.generatedRootDir.get().asFile.absolutePath),
        SubpluginOption("projectSrcDir", project.projectDir.toPath().resolve("src").toAbsolutePath().toString()),
        SubpluginOption("sourceSetName", sourceSetName),
        SubpluginOption("targetName", target),
        SubpluginOption("projectDir", project.projectDir.path),
        SubpluginOption("queriesBaseDir", queriesBaseDir),
        SubpluginOption("outputString", outputStringValue),
        SubpluginOption("queryFilesEnabled", queryFilesEnabled.toString()),
        SubpluginOption("queryPrintingEnabled", queryPrintingEnabled.toString())
      )
    }
  }
}
