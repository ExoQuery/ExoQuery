package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object NullCheckSimplificationReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "general checking/if (nullable not null) nullable else alt/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; if (!{ val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle } == null) mp.handle else null }"
    ),
    "general checking/if (nullable not null) nullable else alt/SQL" to cr(
      "SELECT mp.handle AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "general checking/if (nullable not null) ?nullable else alt/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; if (!{ val tmp1_safe_receiver = mp; if (tmp1_safe_receiver == null) null else tmp1_safe_receiver.handle } == null) { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle } else null }"
    ),
    "general checking/if (nullable not null) ?nullable else alt/SQL" to cr(
      "SELECT mp.handle AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "general checking/if (nullable not null) nullable + 'foo' else alt/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; if (!{ val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle } == null) mp.handle + foo else null }"
    ),
    "general checking/if (nullable not null) nullable + 'foo' else alt/SQL" to cr(
      "SELECT mp.handle || 'foo' AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "general checking/if (nullable not null) ?nullable + 'foo' else alt/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; if (!{ val tmp2_safe_receiver = mp; if (tmp2_safe_receiver == null) null else tmp2_safe_receiver.handle } == null) { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle }; if (tmp1_safe_receiver == null) null else { it -> it + foo }.apply(tmp1_safe_receiver) } else null }"
    ),
    "general checking/if (nullable not null) ?nullable + 'foo' else alt/SQL" to cr(
      "SELECT mp.handle || 'foo' AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? column? let { op }/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle }; if (tmp1_safe_receiver == null) null else { it -> it + -crew }.apply(tmp1_safe_receiver) } }"
    ),
    "multi null check handling/row? column? let { op }/SQL" to cr(
      "SELECT mp.handle || '-crew' AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? column? let { op } ?: alt/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; { val tmp2_elvis_lhs = { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle }; if (tmp1_safe_receiver == null) null else { it -> it + -crew }.apply(tmp1_safe_receiver) }; if (tmp2_elvis_lhs == null) anon-crew else tmp2_elvis_lhs } }"
    ),
    "multi null check handling/row? column? let { op } ?: alt/SQL" to cr(
      "SELECT CASE WHEN (mp.handle || '-crew') IS NULL THEN 'anon-crew' ELSE mp.handle || '-crew' END AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? column/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfile)) { mp.astronautId == a.id }; { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.handle } }"
    ),
    "multi null check handling/row? column/SQL" to cr(
      "SELECT mp.handle AS value FROM Astronaut a LEFT JOIN MissionProfile mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? nested? column/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfileNest)) { mp.astronautId == a.id }; { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.data }; if (tmp1_safe_receiver == null) null else tmp1_safe_receiver.handle } }"
    ),
    "multi null check handling/row? nested? column/SQL" to cr(
      "SELECT mp.handle AS value FROM Astronaut a LEFT JOIN MissionProfileNest mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? nested? column let { op }/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfileNest)) { mp.astronautId == a.id }; { val tmp2_safe_receiver = { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.data }; if (tmp1_safe_receiver == null) null else tmp1_safe_receiver.handle }; if (tmp2_safe_receiver == null) null else { it -> it + -crew }.apply(tmp2_safe_receiver) } }"
    ),
    "multi null check handling/row? nested? column let { op }/SQL" to cr(
      "SELECT mp.handle || '-crew' AS value FROM Astronaut a LEFT JOIN MissionProfileNest mp ON mp.astronautId = a.id"
    ),
    "multi null check handling/row? nested? column let { op } ?: 'alt'/XR" to kt(
      "select { val a = from(Table(Astronaut)); val mp = leftJoin(Table(MissionProfileNest)) { mp.astronautId == a.id }; { val tmp3_elvis_lhs = { val tmp2_safe_receiver = { val tmp1_safe_receiver = { val tmp0_safe_receiver = mp; if (tmp0_safe_receiver == null) null else tmp0_safe_receiver.data }; if (tmp1_safe_receiver == null) null else tmp1_safe_receiver.handle }; if (tmp2_safe_receiver == null) null else { it -> it + -crew }.apply(tmp2_safe_receiver) }; if (tmp3_elvis_lhs == null) anon-crew else tmp3_elvis_lhs } }"
    ),
    "multi null check handling/row? nested? column let { op } ?: 'alt'/SQL" to cr(
      "SELECT CASE WHEN (mp.handle || '-crew') IS NULL THEN 'anon-crew' ELSE mp.handle || '-crew' END AS value FROM Astronaut a LEFT JOIN MissionProfileNest mp ON mp.astronautId = a.id"
    ),
  )
}
