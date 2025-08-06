package io.exoquery.generation

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.AssemblingStrategy
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.NumericPreference
import java.sql.Driver
import java.sql.DriverManager

actual fun Code.DataClasses.toGenerator(absoluteRootPath: String): GeneratorBase<*, *, *> {
  val rootPathReal = BasicPath.SlashPath(absoluteRootPath)

  val jdbcUrl = this.driver.jdbcUrl

  val props = run {
    val workingProps = this.propertiesFile?.let {
      val props = java.util.Properties()
      try {
        props.load(java.io.File(it.fileName).inputStream())
      } catch (e: Exception) {
        throw IllegalArgumentException("Code Generation Failed. Failed to load properties file: ${it.fileName}", e)
      }
      props
    } ?: java.util.Properties()
    if (this.usernameEnvVar != null) {
      val user = System.getenv(this.usernameEnvVar)
      if (user == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for username is not set: ${this.usernameEnvVar}")
      }
      workingProps.setProperty("user", user)
    }
    if (this.passwordEnvVar != null) {
      val pass = System.getenv(this.passwordEnvVar)
      if (pass == null) {
        throw IllegalArgumentException("Code Generation Failed. Environment variable for password is not set: ${this.passwordEnvVar}")
      }
      workingProps.setProperty("password", pass)
    }
    if (this.username != null) workingProps.setProperty("user", this.username)
    if (this.password != null) workingProps.setProperty("password", this.password)
    workingProps
  }

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

  val gen = JdbcGenerator(
    LowLevelCodeGeneratorConfig(
      rootPath = rootPathReal,
      packagePrefix = this.packagePrefix?.let { BasicPath.DotPath(it) } ?: BasicPath.Empty,
      nameParser = NameParser.LiteralNames,
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
      //defaultExcludedSchemas = TODO()
      dryRun = dryRun
    ),
    connectionMaker = connectionMaker,
    allowUnknownDatabase = true
  )

  return gen
}
