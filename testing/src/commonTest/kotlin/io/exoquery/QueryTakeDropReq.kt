package io.exoquery

class QueryTakeDropReq: GoldenSpecDynamic(QueryTakeDropReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val name: String, val age: Int)

  "take" - {
    "sqlite" {
      val query = sql { Table<Person>().take(4) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().take(4) }
      val result = query.buildFor.Postgres()
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
    "postgres" {
      val query = sql { Table<Person>().drop(4) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "take and drop" - {
    "sqlite" {
      val query = sql { Table<Person>().take(3).drop(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().take(3).drop(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "drop and limit" - {
    "sqlite" {
      val query = sql { Table<Person>().drop(2).limit(3) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().drop(2).limit(3) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "offset and limit" - {
    "sqlite" {
      val query = sql { Table<Person>().offset(2).limit(3) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().offset(2).limit(3) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "limit and drop" - {
    "sqlite" {
      val query = sql { Table<Person>().limit(3).drop(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().limit(3).drop(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "limit and offset" - {
    "sqlite" {
      val query = sql { Table<Person>().limit(3).offset(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().limit(3).offset(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  "take and offset" - {
    "sqlite" {
      val query = sql { Table<Person>().take(3).offset(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().take(3).offset(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  // Additional chaining case
  "drop, take, then drop" - {
    "sqlite" {
      val query = sql { Table<Person>().drop(2).take(3).drop(1) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().drop(2).take(3).drop(1) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  // This should be (Select * from SqlQuery.limit(5)).offset(2).limit(2) or .limit(2).offset(2)
  // The inner limit needs to be set first because unlike, limit take is eager (i.e. take it now as opposed to
  // taking it from the total). After we do the inner query it doesn't mater whether limit or offset is applied first
  // because the `take` operator is applied last.
  "take, drop, then take" - {
    "sqlite" {
      val query = sql { Table<Person>().take(5).drop(2).take(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().take(5).drop(2).take(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  // In this case, the limit and offset should be applied first (and it doesn't matter which order)
  // since `limit` is lazy i.e. it applies to the total result set. Then it needs to be nested
  // and the outer limit taken from that.
  "limit, offset, then limit" - {
    "sqlite" {
      val query = sql { Table<Person>().limit(5).offset(2).limit(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().limit(5).offset(2).limit(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }

  // The first two takes should be nested, then the drop applied to the result
  "take, take, drop" - {
    "sqlite" {
      val query = sql { Table<Person>().take(5).take(3).drop(2) }
      val result = query.buildFor.Sqlite()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
    "postgres" {
      val query = sql { Table<Person>().take(5).take(3).drop(2) }
      val result = query.buildFor.Postgres()
      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(result, "SQL")
    }
  }
})
