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


actual fun Code.DataClasses.toLowLevelConfig(absoluteRootPath: String, propertiesBaseDir: String?): Pair<LowLevelCodeGeneratorConfig, PropsData> {

  val propsData = with(PropsOps) {
    val workingProps = propertiesFile.let {
      val props = java.util.Properties()
      val propsFile = propertiesBaseDir?.let { baseDirValue -> java.io.File(baseDirValue, it) } ?: java.io.File(it)
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
    if (usernameEnvVar != null) {
      val user = System.getenv(usernameEnvVar)
      if (user == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for username is not set: ${usernameEnvVar}")
      }
      workingProps.setUser(user)
    }
    if (passwordEnvVar != null) {
      val pass = System.getenv(passwordEnvVar)
      if (pass == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for password is not set: ${passwordEnvVar}")
      }
      workingProps.setPassword(pass)
    }
    if (username != null) workingProps.setUser(username)
    if (password != null) workingProps.setPassword(password)

    PropsData(
      user = workingProps.getUser(),
      password = workingProps.getPassword(),
      apiKey = workingProps.getApiKey()
    )
  }

  // copy the finalized values of various things into the data classes from the properties file
  val finalizedCodeDataClasses =
    this.copy(username = propsData.user, password = propsData.password)

  val lowLevelConfig =
    LowLevelCodeGeneratorConfig(
      codeVersion = this.codeVersion,
      rootPath = absoluteRootPath,
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
      rootLevelOpenApiKey = propsData.apiKey,
      //defaultExcludedSchemas = TODO()
      dryRun = dryRun,
      detailedLogs = detailedLogs,
      tableFilter = this.tableFilter,
      schemaFilter = this.schemaFilter,
      unrecognizedTypeStrategy = this.unrecognizedTypeStrategy,
      typeMap = this.typeMap
    )

  return lowLevelConfig to propsData
}

private fun PropsData.toDatabaseProps(): Properties {
  val props = Properties()
  with (PropsOps) {
    user?.let { props.setUser(it) }
    password?.let { props.setPassword(it) }
  }
  return props
}

private object PropsOps {
  fun Properties.setUser(user: String): Unit { this.setProperty("user", user) }
  fun Properties.setPassword(password: String): Unit { this.setProperty("password", password) }
  fun Properties.setApiKey(apiKey: String): Unit { this.setProperty("api-key", apiKey) }
  fun Properties.getUser() = this.get("user")?.let { it.toString() }
  fun Properties.getPassword() = this.get("password")?.let { it.toString() }
  fun Properties.getApiKey() = this.get("api-key")?.let { it.toString() }
}


actual fun Code.DataClasses.toGenerator(absoluteRootPath: String, projectBaseDir: String?): GeneratorBase<*, *> {
  val (lowLevelConfig, propsData) = this.toLowLevelConfig(absoluteRootPath, projectBaseDir)

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
        throw IllegalArgumentException(
          """
            Code Generation Failed. Failed to load or construct driver class: ${driver.driverClass}. Have you included this driver in your gradle exoQuery { ... } block?
            (Note that if you have a JDBC driver dependency in your regular dependencies block, this will not help if the code generator is running at compile-time. Instead you need to add it to the exoQuery block.)
            ==================== For Example: ====================
            exoQuery {
              codegenDrivers.add("org.postgresql:postgresql:42.7.3")
            }
          """.trimIndent(),
          e
        )
      }

    val jdbcUrl = this.driver.jdbcUrl
    driver.connect(jdbcUrl, propsData.toDatabaseProps())
  }

  val gen = JdbcGenerator.Live(
    lowLevelConfig,
    connectionMaker = connectionMaker,
    allowUnknownDatabase = true
  )

  return gen
}
