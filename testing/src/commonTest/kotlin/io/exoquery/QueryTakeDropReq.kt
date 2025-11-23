package io.exoquery

class QueryTakeDropReq: GoldenSpecDynamic(GoldenQueryFile.Empty, Mode.ExoGoldenOverride(), {
  data class Person(val name: String, val age: Int)

  "take" - {
    "sqlite" {
      val query = sql { Table<Person>().take(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "drop" - {
    "sqlite" {
      val query = sql { Table<Person>().drop(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "take and drop" - {
    "sqlite" {
      val query = sql { Table<Person>().take(4).drop(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "drop and limit" - {
    "sqlite" {
      val query = sql { Table<Person>().drop(4).limit(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "offset and limit" - {
    "sqlite" {
      val query = sql { Table<Person>().offset(4).limit(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "limit and drop" - {
    "sqlite" {
      val query = sql { Table<Person>().limit(4).drop(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "limit and offset" - {
    "sqlite" {
      val query = sql { Table<Person>().limit(4).offset(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "take and offset" - {
    "sqlite" {
      val query = sql { Table<Person>().take(4).offset(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }
})
