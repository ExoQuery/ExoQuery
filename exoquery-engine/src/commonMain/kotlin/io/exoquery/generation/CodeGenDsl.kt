package io.exoquery.generation

import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
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

@Ser
sealed interface CodeVersion {
  @Ser data class Fixed(val version: String): CodeVersion
  @Ser data object Floating: CodeVersion

  companion object {
  }
}

object Code {
  @Ser data class DataClasses(
    val codeVersion: CodeVersion,
    val driver: DatabaseDriver,
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
    val propertiesFile: String = DefaultPropertiesFile,

    val nameParser: NameParser = DefaultNameParser,

    val tableGrouping: TableGrouping = DefaultTableGrouping,


    /**
     * A regex schema filter. Only tables in schemas that match this regex will be included in the generated code.
     */
    val schemaFilter: String? = null,

    /**
     * A regex table filter. Only tables that match this regex will be included in the generated code.
     */
    val tableFilter: String? = null,

    val unrecognizedTypeStrategy: UnrecognizedTypeStrategy = DefaultUnrecognizedTypeStrategy,

    val dryRun: Boolean = DefaultDryRun,
    val detailedLogs: Boolean = DefaultDetailedLogs


    // TODO add typeMap
    // TypeMap(
    //    From(tableName=,columnName=,typeName=str,typeNum=) to KotlinType.of<T>() (or KotlinType.of(str))
    // )
  ) {
    companion object {
      // Use the pattern of specifying the default fetch policy here so it can be used in the compiler plugin unlifter
      val DefaultTableGrouping = TableGrouping.SchemaPerPackage
      val DefaultPropertiesFile = ".codegen.properties"
      val DefaultDryRun = false
      val DefaultNameParser = NameParser.Literal
      val DefaultDetailedLogs = false
      val DefaultUnrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
    }
  }
}

fun Code.DataClasses.encode(): String {
  return protoBuf.encodeToHexString(this)
}

@Ser
sealed interface DatabaseDriver {
  companion object {
  }

  val driverClass: String
  val jdbcUrl: String

  @Ser data class Postgres(override val jdbcUrl: String = Postgres.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "org.postgresql.Driver"
    companion object {
      val DefaultUrl = "jdbc:postgresql://localhost:5432"
    }
  }
  @Ser data class MySQL(override val jdbcUrl: String = MySQL.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "com.mysql.cj.jdbc.Driver"
    companion object {
      val DefaultUrl = "jdbc:mysql://localhost:3306"
    }
  }
  @Ser data class SQLite(override val jdbcUrl: String = SQLite.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "org.sqlite.JDBC"
    companion object {
      val DefaultUrl = "jdbc:sqlite::memory:"
    }
  }
  @Ser data class H2(override val jdbcUrl: String = H2.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "org.h2.Driver"
    companion object {
      val DefaultUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    }
  }
  @Ser data class Oracle(override val jdbcUrl: String = Oracle.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "oracle.jdbc.OracleDriver"
    companion object {
      val DefaultUrl = "jdbc:oracle:thin:@localhost:1521:xe"
    }
  }
  @Ser data class SqlServer(override val jdbcUrl: String = SqlServer.DefaultUrl): DatabaseDriver {
    override val driverClass: String = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    companion object {
      val DefaultUrl = "jdbc:sqlserver://localhost:1433;databaseName=master"
    }
  }
  @Ser data class Custom(
    override val driverClass: String,
    override val jdbcUrl: String
  ): DatabaseDriver {
    companion object {
      fun from(driverClass: String, jdbcUrl: String): Custom = Custom(driverClass, jdbcUrl)
    }
  }
}

@Ser sealed interface TableGrouping {
  @Ser data object SchemaPerPackage: TableGrouping
  @Ser data object SchemaPerObject: TableGrouping

  companion object {
  }
}
