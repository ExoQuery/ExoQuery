package io.exoquery.codegen

import io.exoquery.sql
import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.TableGrouping
import io.exoquery.generation.typemap.ClassOf
import io.exoquery.generation.typemap.From
import io.exoquery.generation.typemap.TypeMap
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UnliftingSpec: FreeSpec({
  "unlift version" - {
    "fixed" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B")
      )
    }
    "floating" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Floating,
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Floating,
        DatabaseDriver.Custom("A", "B")
      )
    }
  }
  "unlift driver" - {
    "postgres" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Postgres("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Postgres("A")
      )
    }
    "mysql" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.MySQL("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.MySQL("A")
      )
    }
    "sqlite" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.SQLite("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.SQLite("A")
      )
    }
    "h2" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.H2("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.H2("A")
      )
    }
    "oracle" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Oracle("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Oracle("A")
      )
    }
    "sqlserver" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.SqlServer("A")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.SqlServer("A")
      )
    }
    "custom" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B")
      )
    }
  }
  "unlift NameParser" - {
    "literal" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.Literal
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        nameParser = NameParser.Literal
      )
    }
    "snake case" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.SnakeCase
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        nameParser = NameParser.SnakeCase
      )
    }
    "using llm" -  {
      "ollama" {
        sql.generateJustReturn(
          Code.Entities(
            CodeVersion.Fixed("0"),
            DatabaseDriver.Custom("A", "B"),
            nameParser = NameParser.UsingLLM(LLM.Ollama("A"), 0, 1, "B", "C")
          )
        ) shouldBe Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.UsingLLM(LLM.Ollama("A"), 0, 1, "B", "C")
        )
      }
      "openai" {
        sql.generateJustReturn(
          Code.Entities(
            CodeVersion.Fixed("0"),
            DatabaseDriver.Custom("A", "B"),
            nameParser = NameParser.UsingLLM(LLM.OpenAI("A"), 0, 1, "B", "C")
          )
        ) shouldBe Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.UsingLLM(LLM.OpenAI("A"), 0, 1, "B", "C")
        )
      }
    }
  }

  "unlift TableGrouping" - {
    "schema per package" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          tableGrouping = TableGrouping.SchemaPerPackage
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        tableGrouping = TableGrouping.SchemaPerPackage
      )
    }
    "single package" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          tableGrouping = TableGrouping.SchemaPerObject
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        tableGrouping = TableGrouping.SchemaPerObject
      )
    }
  }

  "unlift UnrecognizedTypeStrategy" - {
    "throw" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
      )
    }
    "skip column" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn
      )
    }
    "use string" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
      )
    }
  }

  "unlift TypeMap" - {
    "using just column" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          typeMap = TypeMap(
            From("A") to ClassOf<String>()
          )
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        typeMap = TypeMap(
          From("A") to ClassOf<String>()
        )
      )
    }
    "using all fields" {
      sql.generateJustReturn(
        Code.Entities(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          typeMap = TypeMap(
            From("A", "B", "C", "D", 1, true) to ClassOf("E")
          )
        )
      ) shouldBe Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        typeMap = TypeMap(
          From("A", "B", "C", "D", 1, true) to ClassOf("E")
        )
      )
    }
  }

  // test out plugging in values into all of the nullable/defaultable arguments oc Code.Entities
  "top level Code.Entities constants" {
    sql.generateJustReturn(
      Code.Entities(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        packagePrefix = "C",
        username = "D",
        password = "E",
        usernameEnvVar = "F",
        passwordEnvVar = "G",
        propertiesFile = "H",
        schemaFilter = "I",
        tableFilter = "J",
        dryRun = true,
        detailedLogs = false
      )
    )
  }
})
