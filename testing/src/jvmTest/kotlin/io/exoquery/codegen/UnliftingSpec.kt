package io.exoquery.codegen

import io.exoquery.capture
import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.TableGrouping
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UnliftingSpec: FreeSpec({
  "unlift version" - {
    "fixed" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B")
      )
    }
    "floating" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Floating,
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Floating,
        DatabaseDriver.Custom("A", "B")
      )
    }
  }
  "unlift driver" - {
    "postgres" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Postgres("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Postgres("A")
      )
    }
    "mysql" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.MySQL("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.MySQL("A")
      )
    }
    "sqlite" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.SQLite("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.SQLite("A")
      )
    }
    "h2" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.H2("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.H2("A")
      )
    }
    "oracle" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Oracle("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Oracle("A")
      )
    }
    "sqlserver" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.SqlServer("A")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.SqlServer("A")
      )
    }
    "custom" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B")
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B")
      )
    }
  }
  "unlift NameParser" - {
    "literal" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.Literal
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        nameParser = NameParser.Literal
      )
    }
    "snake case" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.SnakeCase
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        nameParser = NameParser.SnakeCase
      )
    }
    "using llm" -  {
      "ollama" {
        capture.generateJustReturn(
          Code.DataClasses(
            CodeVersion.Fixed("0"),
            DatabaseDriver.Custom("A", "B"),
            nameParser = NameParser.UsingLLM(LLM.Ollama("A"), 0, 1, "B", "C")
          )
        ) shouldBe Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.UsingLLM(LLM.Ollama("A"), 0, 1, "B", "C")
        )
      }
      "openai" {
        capture.generateJustReturn(
          Code.DataClasses(
            CodeVersion.Fixed("0"),
            DatabaseDriver.Custom("A", "B"),
            nameParser = NameParser.UsingLLM(LLM.OpenAI("A"), 0, 1, "B", "C")
          )
        ) shouldBe Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          nameParser = NameParser.UsingLLM(LLM.OpenAI("A"), 0, 1, "B", "C")
        )
      }
    }
  }

  "unlift TableGrouping" - {
    "schema per package" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          tableGrouping = TableGrouping.SchemaPerPackage
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        tableGrouping = TableGrouping.SchemaPerPackage
      )
    }
    "single package" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          tableGrouping = TableGrouping.SchemaPerObject
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        tableGrouping = TableGrouping.SchemaPerObject
      )
    }
  }

  "unlift UnrecognizedTypeStrategy" - {
    "throw" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
      )
    }
    "skip column" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn
      )
    }
    "use string" {
      capture.generateJustReturn(
        Code.DataClasses(
          CodeVersion.Fixed("0"),
          DatabaseDriver.Custom("A", "B"),
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
        )
      ) shouldBe Code.DataClasses(
        CodeVersion.Fixed("0"),
        DatabaseDriver.Custom("A", "B"),
        unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
      )
    }
  }

  // test out plugging in values into all of the nullable/defaultable arguments oc Code.DataClasses
  "top level Code.DataClasses constants" {
    capture.generateJustReturn(
      Code.DataClasses(
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
