package io.exoquery

import io.exoquery.config.ExoCompileOptions
import io.exoquery.exoquery_plugin_gradle.BuildConfig
import org.gradle.api.Project
import org.gradle.api.file.Directory
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

  /** If you want to use code generation for entities (at compile-time), you need to specify the JDBC drivers here.
   * This is a list of gradle dependency strings, e.g. "org.postgresql:postgresql:42.7.3"
   */
  val codegenDrivers: ListProperty<String>

  val enableCrossFileStore: Property<Boolean>

  /**
   * If you want to use NameParser.UsingLLM for code generation (at compile-time), you need to enable this property.
   */
  val enableCodegenAI: Property<Boolean>

    /**
     * Code-generated entities will either be placed into MyProject/build/generated/entities
     * (or MyProject/entities if `codegenIntoPermanentLocation` is enabled). Normally
     * these directories will not be created unless there is actually generated
     * code that the plugin created there.
     *
     * Setting this property will automatically create empty directories at these locations
     * (when the build is refreshed) i.e. they will exist even if there is no generated code.
     */
    val autoCreateCodenDirectories: Property<Boolean>

    /**
     * Use a permanent location for generated entities i.e. `MyProject/src/main/entities` as opposed to
     * a temporary one under `MyProject/build/generated/entities` (default=true). Alternatively, if you want to specify a custom path
     * you can use `codegenCustomPath` property (a relative path will be based on the JVM working directory of the compilation.
     *
     * Due to the non-deterministic nature of the LLMs, the generated code may change
     * between builds. This means that when using an AI for table/column naming you will want to
     * generate into a directory that is checked into version control as opposed to a location
     * that is cleaned up between builds (which is the default i.e. MyProject/build/generated/entities).
     * When this option is enabled, the generated code will be placed in MyProject/src/main/entities
     * instead and the `entities` directory will be added to the source set.
     * Note that if you turn on `enableCodegenNamingAI` then this option is automatically enabled.
     *
     * Even when we are not using LLMs for assisting record-generation, in most cases we will want
     * the records to be permanently checked into version control so as to be able to track when they are changed.
     * Furthermore, since the (compile-time) record-generation process relies on be able to recompile kotlin code
     * (see the exoquery-sample-codegen project for example of how to work around this issue)
     * we do not want the records to ever be ephemeral. Therefore, the default value for this property is true
     * and unless you are generating records in a different build then your main one you will want to keep it that way.
     *
     * @see GeneratedEntitiesDirConventions for more detail.
     */
    val codegenIntoPermanentLocation: Property<Boolean>

    val codegenCustomPath: Property<String>

    val koogLibrary: Property<String>

  /**
   * Normally the code-generator will only regenerate code if the CodeVersion.Fixed("x.y.z") changes
   * or if it is set to CodeVersion.Floating. However, during development it can be useful to override
   * the CodeVersion.Fixed and force regeneration to happen anyway. This property does that.
   */
  val forceRegen: Property<Boolean>

    val debugGeneratedDirConventions: Property<Boolean>
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
        enableCodegenAI.convention(ExoCompileOptions.DefaultEnabledCodegenAI)
        forceRegen.convention(ExoCompileOptions.DefaultForceRegen)
        enableCrossFileStore.convention(ExoCompileOptions.EnableCrossFileStore)
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

      target.addCompilerDependency("org.mapdb:mapdb:3.1.0")
      target.addCompilerDependency("org.eclipse.collections:eclipse-collections-api:10.4.0")
      target.addCompilerDependency("org.eclipse.collections:eclipse-collections:10.4.0")
      target.addCompilerDependency("org.eclipse.collections:eclipse-collections-forkjoin:10.4.0")
      target.addCompilerDependency("com.google.guava:guava:31.0.1-jre")
      target.addCompilerDependency("net.jpountz.lz4:lz4:1.3.0")
      target.addCompilerDependency("org.mapdb:elsa:3.0.0-M5")


      //target.addCompilerDependency(BuildConfig.COROUTINES_LIBRARY)
    }

    val conventions = GeneratedEntitiesDirConventions(pluginExtConfig, target)
  }

  fun Project.addCompilerDependency(dependency: String) =
    dependencies.add("kotlinNativeCompilerPluginClasspath", dependency)

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
    }

    val enableCodegenAI = ext.enableCodegenAI.convention(ExoCompileOptions.DefaultEnabledCodegenAI)
    val forceRegen = ext.forceRegen.convention(ExoCompileOptions.DefaultForceRegen)

    if (enableCodegenAI.get()) {
      val koogLibrary = ext.koogLibrary.convention(BuildConfig.KOOG_LIBRARY).get()
      val coroutinesLibrary = BuildConfig.COROUTINES_LIBRARY

      println("[ExoQuery] LLM naming is enabled in ${kotlinCompilation.project.name}:${kotlinCompilation.name}! Adding Koog dependency for LLM naming: ${koogLibrary} (and also ${coroutinesLibrary})")
      // If LLM naming is enabled, we need to add the Koog dependency
      configurationName.let { configName ->
        project.dependencies.add(configName, koogLibrary)
        project.dependencies.add(configName, coroutinesLibrary)
      }
    }

    // ALSO needed for the plugin classpath
    kotlinCompilation.dependencies {
      api("io.exoquery:exoquery-engine:${BuildConfig.VERSION}")
    }

    val conventions = GeneratedEntitiesDirConventions(ext, project)

    val target = kotlinCompilation.target.name
    // This will always be the platform name e.g. jvm/linuxX64 etc... even if the files are in commonMain
    // because the builds don't actually build commonMain as a compile-target, only the actual platforms
    val sourceSetName =
      kotlinCompilation.defaultSourceSet.name

    // unlike the following function this doesn't really need `setOf(start)` but adding it just for consistency. We remove the `curr` source-set later
    fun transitiveUpstream(start: KotlinSourceSet): List<KotlinSourceSet> =
      start.dependsOn.flatMap { transitiveUpstream(it) + listOf(it) } + listOf(start)

    fun transitiveAssociated(compilation: KotlinCompilation<*>): List<KotlinCompilation<*>> =
      compilation.associatedCompilations.flatMap { transitiveAssociated(it) + listOf(it) } + listOf(compilation)


    val parentSourceSetNamesProvider: Provider<List<String>> = project.provider {
      val curr = kotlinCompilation.defaultSourceSet
      val associatedCompilations = transitiveAssociated(kotlinCompilation).flatMap { it.kotlinSourceSets }

      (associatedCompilations.distinct().flatMap { transitiveUpstream(it) } + transitiveUpstream(curr))
        .distinct()
        .filterNot { it == curr }
        .map { it.name }

    }

    val outputStringValue = ext.outputString.convention(ExoCompileOptions.DefaultOutputString).get()
    val queryFilesEnabled = ext.queryFilesEnabled.convention(ExoCompileOptions.DefaultQueryFilesEnabled).get()
    val queryPrintingEnabled = ext.queryPrintingEnabled.convention(ExoCompileOptions.DefaultQueryPrintingEnabled).get()

    val queriesBaseDir = project.generatedRootDir.get().dir("queries").asFile.absolutePath
    val (entitiesBaseDir, entitiesDirType) = conventions.generatedEntitiesDir(project)
    val autoCreateDirs = ext.autoCreateCodenDirectories.convention(false).get()
    val debugGeneratedDirConventions = ext.debugGeneratedDirConventions.convention(false).get()

    // Need to do this here and not in apply() because the kotlinExtension is not available yet there
    // and we need it to know where the generated directories are (i.e. it changes based on whether an LLM is used for codegen or not)
    project.afterEvaluate { project ->
      val kotlinExtension = project.extensions.findByType(KotlinProjectExtension::class.java)
      kotlinExtension?.let { decorateKotlinProject(it, DecorationContext(kotlinExtension, project, conventions, entitiesBaseDir, entitiesDirType, autoCreateDirs, debugGeneratedDirConventions)) }
    }

    val storedBaseDir = conventions.storedSubdir(target).get().asFile.absolutePath // legacy name

    return project.provider {
      val parentSourceSetNames = parentSourceSetNamesProvider.get()

      listOf(
        SubpluginOption("entitiesBaseDir",entitiesBaseDir.get().asFile.absolutePath),
        SubpluginOption("generationDir", project.generatedRootDir.get().asFile.absolutePath),
        SubpluginOption("projectSrcDir", project.projectDir.toPath().resolve("src").toAbsolutePath().toString()),
        SubpluginOption("sourceSetName", sourceSetName),
        SubpluginOption("targetName", target),
        CompositeSubpluginOption(
          "parentSourceSetNames",
          lazy { parentSourceSetNames.joinToString(";") },
          parentSourceSetNames.map { SubpluginOption("parentSourceSetNames", it) }
        ),
        SubpluginOption("projectDir", project.projectDir.path),
        SubpluginOption("queriesBaseDir", queriesBaseDir),
        SubpluginOption("storedBaseDir", storedBaseDir),
        SubpluginOption("outputString", outputStringValue),
        SubpluginOption("queryFilesEnabled", queryFilesEnabled.toString()),
        SubpluginOption("queryPrintingEnabled", queryPrintingEnabled.toString()),
        SubpluginOption("enableCodegenAI", enableCodegenAI.get().toString()),
        SubpluginOption("forceRegen", forceRegen.get().toString()),
        SubpluginOption("enableCrossFileStore", ext.enableCrossFileStore.convention(ExoCompileOptions.EnableCrossFileStore).getOrElse(ExoCompileOptions.EnableCrossFileStore).toString())
      )
    }
  }

  data class DecorationContext(
    val kotlin: KotlinProjectExtension,
    val project: Project,
    val conventions: GeneratedEntitiesDirConventions,
    val entitiesBaseDir: Provider<Directory>,
    val entitiesDirType: EntitiesDirType,
    val autoCreateDirs: Boolean,
    val debugGeneratedDirConventions: Boolean
  )

  private fun decorateKotlinProject(kotlin: KotlinProjectExtension, deco: DecorationContext) {
    when (kotlin) {
      is KotlinSingleTargetExtension<*> -> {
        val targetsAndSourceSets =
          kotlin.target.compilations.map { compilation ->
            kotlin.target to compilation.defaultSourceSet
          }
        generateEntitiesDirs(targetsAndSourceSets, deco)
      }
      is KotlinMultiplatformExtension -> {
        val targetsAndSourceSets =
          kotlin.targets.flatMap { target ->
            target.compilations.map { compilation ->
              target to compilation.defaultSourceSet
            }
          }
        generateEntitiesDirs(targetsAndSourceSets, deco)
      }
    }
  }

  private fun generateEntitiesDirs(targetsAndSourceSets: List<Pair<KotlinTarget, KotlinSourceSet>>, deco: DecorationContext) =
    targetsAndSourceSets.forEach { (target, sourceSet) ->
      val targetName = target.name
      val sourceSetName = sourceSet.name

      // Add the generated directory to kotlin source directories
      val project = deco.project
      val autoCreateDirs = deco.autoCreateDirs
      val (generatedDirProvider, dirType) =  deco.conventions.generatedEntitiesKotlin(deco.project, sourceSetName, targetName)
      val generatedDir = generatedDirProvider.get().asFile

      if (deco.debugGeneratedDirConventions)
        deco.conventions.show(project, sourceSetName, targetName)

      println("[ExoQuery] Auto-Adding directories${if (autoCreateDirs) "(with mkdirs)" else ""} to [${project.name}] ($targetName/$sourceSetName) (target/sourceSet) using a ${dirType} configuration (${generatedDir})")

      sourceSet.kotlin.srcDir(generatedDir)
      // Ensure the directory exists
      if (autoCreateDirs) {
        generatedDir.mkdirs()
      }
    }
}
