package io.exoquery.lang

import io.exoquery.xr.BetaReduction
import io.exoquery.xr.XR

object SqlQueryHelper {
  /*
   * people.flatMap(p ->
   *   join(addr,p.id).map(a => (p,a)))
   *    .flatMap(kv -> join(robot,kv._2.id).map(r => (...))
   *
   * Is something like:
   * Fmap(people, p,
   *      Fmap(
   *        Map(Join(addr,p.id), a, (p, a)),
   *        kv,
   *        Fmap(Join(robot,a.id), r, ...)))
   *      )
   * )
   * This cannot be converted to SQL directly because `Join(addr,p.id)` ends up being in the head-position
   * of a query and the 1st thing a query can't be a join. That would be something like:
   * ```
   * select {
   *   p = from(people)
   *   select {
   *     kv = from(join(addr,p.id).map(a => (p,a))) // <- Invalid! Can't select directly from a join
   *     r = join(robot,kv._2.id)
   *     ...
   *   }
   * }
   * ```
   * Now in the case where the join is simply followed by a map, we can just pull out the join
   * and flatten the whole query. To look like this:
   * ```
   * select {
   *   p = from(people)
   *   a = join(addr,p.id)
   *   r = join(robot,p.id)
   * }
   * ```
   * Note however that since we're using the `a` from the `.map(p => (p,a))` clause in the join(robot)
   * we need to beta-reduce kv -> (p, a) since the proceeding clauses are using p and a directly.
   */
  fun XR.FlatMap.flattenDualHeadsIfPossible() =
    if (head is XR.Map) {
      XR.FlatMap(
        head.head, // join(addr,p.id)
        head.id, // the `kv` variable
        BetaReduction.ofQuery(body, id to head.body) // kv -> (p,a)
      )
    } else {
      null
    }

}
