package io.exoquery

class NullCheckSimplificationReq: GoldenSpecDynamic(NullCheckSimplificationReqGoldenDynamic, Mode.ExoGoldenTest(), {

  data class Astronaut(val id: Int, val name: String)
  data class MissionProfile(val astronautId: Int, val bio: String?, val handle: String?)
  data class ProfileSummary(val astronaut: String, val heroBio: String, val decoratedHandle: String)

  data class Data(val bio: String?, val handle: String?)
  data class MissionProfileNest(val astronautId: Int, val data: Data?)

  "general checking" - {
    "if (nullable not null) nullable else alt" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        if (mp?.handle != null) mp.handle else null
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "if (nullable not null) ?nullable else alt" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        if (mp?.handle != null) mp?.handle else null
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "if (nullable not null) nullable + 'foo' else alt" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        if (mp?.handle != null) mp.handle + "foo" else null
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "if (nullable not null) ?nullable + 'foo' else alt" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        if (mp?.handle != null) mp?.handle?.let { it + "foo" } else null
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }

  "multi null check handling" - {
    "row?.column?.let { op }" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        mp?.handle?.let { it + "-crew" }
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "row?.column?.let { op } ?: alt" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        mp?.handle?.let { it + "-crew" } ?: "anon-crew"
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    // I.e. simple null checking
    "row?.column" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfile>()) { it.astronautId == a.id }
        mp?.handle
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "row?.nested?.column" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfileNest>()) { it.astronautId == a.id }
        mp?.data?.handle
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "row?.nested?.column.let { op }" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfileNest>()) { it.astronautId == a.id }
        mp?.data?.handle?.let { it + "-crew" }
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "row?.nested?.column.let { op } ?: 'alt'" {
      val q = sql.select {
        val a = from(Table<Astronaut>())
        val mp = joinLeft(Table<MissionProfileNest>()) { it.astronautId == a.id }
        mp?.data?.handle?.let { it + "-crew" } ?: "anon-crew"
      }.dynamic()

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }
})
