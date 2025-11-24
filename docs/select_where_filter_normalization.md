Change: [PR#67](https://github.com/ExoQuery/ExoQuery/pull/67)

### Introduction
This change tightens how `select { ... }.filter(...)` chains are normalized so that filter predicates are pushed down into the base query’s `WHERE` clause whenever it is safe. In practical terms, the optimizer now prefers to combine your filter with the original `from + select` shape, instead of wrapping the base inside an extra subquery. The key enabler is careful beta-reduction of the filter parameter into the base projection—guarded by purity checks—so we only rewrite when semantics remain identical.

### Why this matters for ExoQuery
One of ExoQuery’s overarching goals is to keep generated queries as flat as possible. Flatter queries have several advantages:
- Better predicate pushdown: Moving conditions directly into the base `WHERE` improves pruning early, reducing scanned rows and intermediate results.
- More optimizer-friendly SQL: Many databases optimize flat `SELECT ... WHERE ...` plans better than nested subqueries, enabling better use of indexes, statistics, and join reordering.
- Simpler, portable SQL: Fewer nested layers mean fewer dialect-specific quirks and more predictable behavior across engines.
- Reduced overhead: Avoiding unnecessary subqueries decreases planning and execution overhead, often translating to lower latency and resource usage.
- Clearer intent: The SQL mirrors the logical intent of the DSL more closely when avoidable nesting is removed.

This update advances that goal by:
- Aggressively pushing `filter` into `WHERE` when the base has no `where`, or when the base is trivial and the projection is pure and singular.
- Avoiding unsafe rewrites when projections are impure or the base is non-trivial, preserving correctness.

The result: more cases normalize to a single-layer `SELECT ... FROM ... WHERE ...`, with subqueries introduced only when they are truly needed. The examples below highlight where this flattening occurs and where it is intentionally avoided for correctness.

### Summary of the change
The query-normalization logic for `select { ... }.filter(...)` has been updated to push `filter` predicates down into the base query’s `WHERE` clause more aggressively, but only when it is safe to do so.

Specifically:
- Case A — No existing WHERE: If the base `select` has no `where`, the `filter` can be tacked on as the `where` when either:
  - the filter’s parameter is not used in the filter body, or
  - the filter’s parameter names (aliases) are sourced from the base `from` clause.
- Case B — Trivial base with existing WHERE: If the base query is “trivial” (i.e., only `from + select`) and has a single projected expression that is pure (contains no “impurities”), then even if there is already a `where`, the `filter` is beta-reduced into the base and combined with the existing `where` using `AND`.
- Case C — Otherwise: If neither (A) nor (B) applies (e.g., non-trivial base, multiple select expressions, or the select expression is impure, or the filter refers to a non-from alias that matters), then a nested layer is introduced: the base becomes a subquery, and the `filter` is applied on top of that subquery.

Notes
- "Trivial" here means the base is only `from + select` (no joins, groupings, etc.). In this `SqlQueryModel`, most bases are a single select value at this stage; expansion to multiple values typically happens later.
- "Impurities" refer to expressions with side effects or non-determinism (e.g., `free("rand()")`). Such expressions prohibit safe beta-reduction of the filter parameter into the base’s projection.

---

### Practical rules of thumb
- If you selected a clean field (e.g., `p.name`) and your filter references only the filter parameter (or doesn’t use it), the filter typically becomes part of the base `WHERE`.
- If your selection contains non-deterministic or effectful expressions (e.g., calling `rand()` or other `free(...)`), the filter is applied on a wrapping subquery instead.
- If the base already has a `WHERE` and is trivial and pure with a single projection, the new filter is combined with `AND` into that `WHERE`.

---

### Examples (SqlQuery DSL → SQL)

#### 1) Simple pushdown when there’s no existing WHERE
SqlQuery DSL:
```kotlin
sql {
  select {
    val p = from(Person)
    p
  }.filter { p -> p.age > 10 }
}
```
Normalized SQL:
```sql
SELECT p.*
FROM Person p
WHERE p.age > 10;
```
Why: Base has no `where`, filter variable `p` is a from-alias → push into `WHERE`.

---

#### 2) Combine with existing WHERE when base is trivial and pure
SqlQuery DSL:
```kotlin
sql {
  select {
    val p = from(Person)
    where(p.active)
    p
  }.filter { p -> p.age > 10 }
}
```
Normalized SQL:
```sql
SELECT p.*
FROM Person p
WHERE p.active AND p.age > 10;
```
Why: Base is trivial (`from + select`), has a single projection (`p`), and that projection is pure → beta-reduce and `AND` into existing `WHERE`.

---

#### 3) Trivial and pure, but selection is a field — still reducible
SqlQuery DSL:
```kotlin
sql {
  select {
    val p = from(Person)
    where(p.city == "LA")
    p.name
  }.filter { name -> name.startsWith("A") }
}
```
Possible normalized SQL (assuming `startsWith` lowers to `LIKE 'A%'`):
```sql
SELECT p.name
FROM Person p
WHERE p.city = 'LA' AND p.name LIKE 'A%';
```
Why: The selected value `p.name` is pure and singular; the filter parameter `name` beta-reduces to `p.name`, so we can push it down and combine with the existing `WHERE`.

---

#### 4) Not reducible due to impurities → introduce a subquery
SqlQuery DSL:
```kotlin
sql {
  select {
    val p = from(Person)
    where(p.age > 10)
    free("rand()")<Int>() + p.age
  }.filter { x -> (x % 2) == 0 }
}
```
Normalized SQL (wrap base as subquery):
```sql
SELECT t.x
FROM (
  SELECT (RAND() + p.age) AS x
  FROM Person p
  WHERE p.age > 10
) t
WHERE (t.x % 2) = 0;
```
Why: The select expression is impure (contains `rand()`), so the filter cannot be safely pushed into the base `WHERE`.

---

#### 5) Filter doesn’t use its parameter
SqlQuery DSL:
```kotlin
sql {
  select {
    val p = from(Person)
    p
  }.filter { _ -> true }
}
```
Normalized SQL:
```sql
SELECT p.*
FROM Person p
WHERE TRUE;
```
Why: No existing `WHERE` and the filter parameter is unused → falls under Case A; we can attach it as a new `WHERE`.

If there is already a `WHERE` and the base is trivial/pure with a single projection, it combines via `AND`:
```kotlin
sql {
  select {
    val p = from(Person)
    where(p.active)
    p
  }.filter { _ -> true }
}
```
```sql
SELECT p.*
FROM Person p
WHERE p.active AND TRUE;
```

---

#### 6) Non-trivial base (e.g., groupBy) → likely needs a subquery unless safe by Case A
SqlQuery DSL:
```kotlin
sql {
  select {
    val s = from(Sale)
    groupBy(s.storeId) { g ->
      sum(g.amount) as "total"
    }
  }.filter { row -> row.total > 1000 }
}
```
Normalized SQL (typical):
```sql
SELECT t.total
FROM (
  SELECT s.storeId, SUM(s.amount) AS total
  FROM Sale s
  GROUP BY s.storeId
) t
WHERE t.total > 1000;
```
Why: The base is not trivial (has `GROUP BY`). This falls under Case C and gets wrapped as a subquery, then filtered.

---

### Edge considerations
- Multiple select values at this phase usually don’t occur in `SqlQueryModel` (they’re expanded later). When they do, Case B requires a single select expression to safely beta-reduce.
- If the filter references identifiers not present in the base `from` aliases (and it matters to evaluation), the safe route is to wrap the base as a subquery (Case C).
- The impurity check is conservative: if the selected expression contains any effectful or non-deterministic calls (e.g., `free("rand()")`), the optimizer avoids pushing the filter down.

---

### Takeaway
- Prefer pure, simple projections when you intend to chain `.filter(...)` after `select { ... }` — it maximizes opportunities for the optimizer to push filters into `WHERE` and avoid extra nesting.
- When projections are impure or the base is complex, expect the filter to be applied on a subquery instead of combining into the base `WHERE`.  
