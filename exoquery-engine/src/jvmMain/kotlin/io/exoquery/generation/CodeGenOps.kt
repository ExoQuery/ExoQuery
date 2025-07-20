package io.exoquery.generation

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.AssemblingStrategy
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.NumericPreference
import java.sql.DriverManager

actual fun Code.DataClasses.generate(absoluteRootPath: String): Unit {
  val rootPathReal = BasicPath.SlashPath(absoluteRootPath)

  val jdbcUrl = this.driver.driverClass

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

  val connectionMaker = {
    DriverManager.getConnection(jdbcUrl, props)
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
}
