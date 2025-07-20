package io.exoquery.codegen.util


fun <K, V> Map<K, V>.zipOnKeys(o: Map<K, V>): Map<K, Pair<V?, V?>> =
    (this.keys + o.keys).associateWith { key ->
        Pair(this[key], o[key])
    }

fun <K, V> LinkedHashMap<K, V>.zipOnKeysOrdered(o: LinkedHashMap<K, V>): LinkedHashMap<K, Pair<V?, V?>> = run {
  val dest = LinkedHashMap<K, Pair<V?, V?>>(this.size)
  (this.keys + o.keys).associateWithTo(dest) { key -> Pair(this[key], o[key]) }
}



// Scala:
//object MapExtensions {
//
//  implicit final class MapOps[K, V](private val m: Map[K, V]) extends AnyVal {
//    def zipOnKeys(o: Map[K, V]): Map[K, (Option[V], Option[V])]            = zipMapsOnKeys(m, o)
//    def zipOnKeysOrdered(o: Map[K, V]): ListMap[K, (Option[V], Option[V])] = zipMapsOnKeysOrdered(m, o)
//  }
//
//  def zipMapsOnKeys[K, V](one: Map[K, V], two: Map[K, V]): Map[K, (Option[V], Option[V])] =
//    (for (key <- one.keys ++ two.keys)
//      yield (key, (one.get(key), two.get(key)))).toMap
//
//  def zipMapsOnKeysOrdered[K, V](one: Map[K, V], two: Map[K, V]): ListMap[K, (Option[V], Option[V])] = {
//    val outList =
//      (for (key <- (ListSet() ++ one.keys.toSeq.reverse) ++ (ListSet() ++ two.keys.toSeq.reverse))
//        yield (key, (one.get(key), two.get(key))))
//    (new ListMap() ++ outList.toSeq.reverse)
//  }
//}
