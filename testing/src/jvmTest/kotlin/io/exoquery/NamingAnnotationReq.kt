package io.exoquery

import io.exoquery.annotation.ExoEntity
import io.exoquery.annotation.ExoField
import kotlinx.serialization.SerialName

class NamingAnnotationReq: GoldenSpecDynamic(NamingAnnotationReqGoldenDynamic, Mode.ExoGoldenTest(), {

  @SerialName("person_annotated")
  data class PersonAnnotated(
    @SerialName("first_name")
    val firstName: String,

    @SerialName("last_name")
    val lastName: String,

    val age: Int
  )

  @ExoEntity("PERSON_ANNOTATED")
  @SerialName("person_annotated")
  data class PersonAnnotatedOverride(
    @ExoField("FIRST_NAME")
    @SerialName("first_name")
    val firstName: String,

    @SerialName("last_name")
    val lastName: String,

    val age: Int
  )

  "naming overrides should work in the correct order" - {
    "in a query" {
      val query = capture {
        Table<PersonAnnotated>().filter { it.firstName == "Joe" && it.lastName == "Bloggs" }
      }
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.buildFor.Postgres(), "SQL")
    }
    "in a query - with overrides" {
      val query = capture {
        Table<PersonAnnotatedOverride>().filter { it.firstName == "Joe" && it.lastName == "Bloggs" }
      }
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.buildFor.Postgres(), "SQL")
    }

    "in an action" {
      val action = capture {
        insert<PersonAnnotated> { set(firstName to "Joe", lastName to "Bloggs", age to 42) }
      }
      shouldBeGolden(action.xr, "XR")
      shouldBeGolden(action.buildFor.Postgres(), "SQL")
    }
    "in an action - with overrides" {
      val action = capture {
        insert<PersonAnnotatedOverride> { set(firstName to "Joe", lastName to "Bloggs", age to 42) }
      }
      shouldBeGolden(action.xr, "XR")
      shouldBeGolden(action.buildFor.Postgres(), "SQL")
    }

    "in an action with setParams" {
      val action = capture {
        insert<PersonAnnotated> { setParams(PersonAnnotated("Joe", "Bloggs", 42)) }
      }
      shouldBeGolden(action.determinizeDynamics().xr, "XR")
      shouldBeGolden(action.buildFor.Postgres().determinizeDynamics(), "SQL")
    }
    "in an action with setParams - with overrides" {
      val action = capture {
        insert<PersonAnnotatedOverride> { setParams(PersonAnnotatedOverride("Joe", "Bloggs", 42)) }
      }
      shouldBeGolden(action.determinizeDynamics().xr, "XR")
      shouldBeGolden(action.buildFor.Postgres().determinizeDynamics(), "SQL")
    }
  }

})
