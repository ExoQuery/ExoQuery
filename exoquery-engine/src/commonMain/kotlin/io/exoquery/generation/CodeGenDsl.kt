package io.exoquery.generation

import io.exoquery.xr.EncodingXR.protoBuf
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.Serializable as Ser

/*

capture.generate(
  TableClasses(
    codeVersion = "1.0.0",
    driver = DatabaseDriver.Postgres,
    packagePrefix = "io.exoquery.example",
    username = "user",
    password = "password",
    fetchPolicy = FetchPolicy.OnVersionChange,
  )
)
 */

object Code {
  @Ser data class DataClasses(
    val codeVersion: String,
    val driver: DatabaseDriver,
    val fetchPolicy: FetchPolicy = DefaultFetchPolicy,
    val packagePrefix: String? = null,
    val username: String? = null,
    /**
     * WARNING: DO NOT USE THIS IN PRODUCTION CODE OR WITH PRODUCTION DATABASES.
     * This parameter is only for generating code from test databases.
     * It is a very bad security practice to hard-code passwords in code.
     * Either use an environment-variable with the parameter passwordEnvVar or
     * use the propertiesFile parameter to specify the password.
     */
    val password: String? = null,
    val usernameEnvVar: String? = null,
    val passwordEnvVar: String? = null,

    /**
     * A file that contains the database connection properties.
     */
    val propertiesFile: PropertiesFile? = null,

    val tableGrouping: TableGrouping = DefaultTableGrouping,

    val dryRun: Boolean = DefaultDryRun,
  ) {
    companion object {
      // Use the pattern of specifying the default fetch policy here so it can be used in the compiler plugin unlifter
      val DefaultFetchPolicy = FetchPolicy.OnVersionChange
      val DefaultTableGrouping = TableGrouping.SchemaPerPackage
      val DefaultDryRun = false
    }
  }
}

fun Code.DataClasses.encode(): String {
  return protoBuf.encodeToHexString(this)
}

@Ser
sealed interface FetchPolicy {
  @Ser data object OnVersionChange: FetchPolicy
  @Ser data object Always: FetchPolicy

  companion object {
  }
}

@Ser
sealed interface DatabaseDriver {
  companion object {
  }

  val driverClass: String
  @Ser data object Postgres: DatabaseDriver {
    override val driverClass: String = "org.postgresql.Driver"
  }
  @Ser data object MySQL: DatabaseDriver {
    override val driverClass: String = "com.mysql.cj.jdbc.Driver"
  }
  @Ser data object SQLite: DatabaseDriver {
    override val driverClass: String = "org.sqlite.JDBC"
  }
  @Ser data object Oracle: DatabaseDriver {
    override val driverClass: String = "oracle.jdbc.OracleDriver"
  }
  @Ser data object SQLServer: DatabaseDriver {
    override val driverClass: String = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  }
  @Ser data class Custom(override val driverClass: String): DatabaseDriver
}

@Ser sealed interface PropertiesFile {
  val fileName: String
  @Ser data object Default: PropertiesFile {
    override val fileName: String = ".exoquery.properties"
  }
  @Ser data class Custom(override val fileName: String): PropertiesFile

  companion object {
  }
}

@Ser sealed interface TableGrouping {
  @Ser data object SchemaPerPackage: TableGrouping
  @Ser data object SchemaPerObject: TableGrouping

  companion object {
  }
}
