package io.exoquery.generation

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.AssemblingStrategy
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.NumericPreference
import java.sql.Driver
import java.util.Properties

actual fun Code.DataClasses.toGenerator(absoluteRootPath: String, projectBaseDir: String?): GeneratorBase<*, *, *> {
  val rootPathReal = BasicPath.SlashPath(absoluteRootPath)

  val jdbcUrl = this.driver.jdbcUrl

  fun Properties.setUser(user: String): Unit { this.setProperty("user", user) }
  fun Properties.setPassword(password: String): Unit { this.setProperty("password", password) }
  fun Properties.setApiKey(apiKey: String): Unit { this.setProperty("api-key", apiKey) }
  fun Properties.getUser() = this.get("user")?.let { it.toString() }
  fun Properties.getPassword() = this.get("password")?.let { it.toString() }
  fun Properties.getApiKey() = this.get("api-key")?.let { it.toString() }

  val props = run {
    val workingProps = this.propertiesFile.let {
      val props = java.util.Properties()
      val propsFile = projectBaseDir?.let { baseDirValue -> java.io.File(baseDirValue, it) } ?: java.io.File(it)
      if (propsFile.exists()) {
        println("[ExoQuery] Detected properties file for code generation: ${propsFile.absolutePath}")
        try {
          props.load(propsFile.inputStream())
        } catch (e: Exception) {
          throw IllegalArgumentException("Code Generation Failed. Failed to load properties file: ${it}", e)
        }
      } else {
        println("[ExoQuery] No properties file found for code generation at: ${propsFile.absolutePath}. Using defaults.")
      }
      props
    }
    if (this.usernameEnvVar != null) {
      val user = System.getenv(this.usernameEnvVar)
      if (user == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for username is not set: ${this.usernameEnvVar}")
      }
      workingProps.setUser(user)
    }
    if (this.passwordEnvVar != null) {
      val pass = System.getenv(this.passwordEnvVar)
      if (pass == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for password is not set: ${this.passwordEnvVar}")
      }
      workingProps.setPassword(pass)
    }
    if (this.username != null) workingProps.setUser(this.username)
    if (this.password != null) workingProps.setPassword(this.password)
    workingProps
  }

  // copy the finalized values of various things into the data classes from the properties file
  val finalizedCodeDataClasses =
    this.copy(username = props.getUser(), password = props.getPassword())

  // TODO create a cache of this operation
  val connectionMaker = {
    // Even if a driver is on the classpath it isn't necessarily registered yet so it's easier
    // to just look it up from the driver manager
    val driver: Driver =
      try {
        Class.forName(driver.driverClass).newInstance() as? Driver
          ?: throw IllegalArgumentException("Code Generation Failed. Constructed instance of ${driver.driverClass} was not a java.sql.Driver")
      } catch (e: Exception) {
        // TODO should have specific error about how you should include this library
        //      in the gradle-config for ExoQuery i.e. the exoQuery { ... } block.
        throw IllegalArgumentException("Code Generation Failed. Failed to load or construct driver class: ${driver.driverClass}", e)
      }
    driver.connect(jdbcUrl, props)
  }

  val gen = JdbcGenerator.Live(
    LowLevelCodeGeneratorConfig(
      codeVersion = this.codeVersion,
      rootPath = rootPathReal,
      packagePrefix = this.packagePrefix?.let { BasicPath.DotPath(it) } ?: BasicPath.Empty,
      // NOTE: NameParser.preparedForRuntime SHOULD  NOT be used here because it requires Koog to be present at the compile-time code
      //       we do NOT want to assume that the client always has classpath, only when instruct the codegen at compile-time using
      //       exoQuery { enableCodegenAI = true } (for compile-time code generation) or when they explicit import the Koog library
      //       via a dependency e.g. `implementation("io.exoquery:koog-runtime:___")` can we assume that the client has Koog. None
      //       of which we actually know are the case here.
      nameParser = finalizedCodeDataClasses.nameParser, // If an API key is needed, it will be set in the nameParser by the procedure above
      //tableNamespacer = TODO(),
      //unrecognizedTypeStrategy = TODO(),
      namingAnnotation = NamingAnnotationType.SerialName,
      assemblingStrategy =
        when (this.tableGrouping) {
          TableGrouping.SchemaPerObject -> AssemblingStrategy.SchemaPerObject
          TableGrouping.SchemaPerPackage -> AssemblingStrategy.SchemaPerPackage
        },
      numericPreference = NumericPreference.UseDefaults,
      defaultNamespace = "schema",
      rootLevelOpenApiKey = props.getApiKey(),
      //defaultExcludedSchemas = TODO()
      dryRun = dryRun,
      detailedLogs = detailedLogs
    ),
    connectionMaker = connectionMaker,
    allowUnknownDatabase = true
  )

  return gen
}
