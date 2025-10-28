package io.exoquery.lang

sealed interface EqualityBehavior {
  object AnsiEquality : EqualityBehavior
  object NonAnsiEquality : EqualityBehavior
}

sealed interface ConcatBehavior {
  object AnsiConcat : ConcatBehavior
  object NonAnsiConcat : ConcatBehavior
}

sealed interface ProductAggregationToken {
  object Star : ProductAggregationToken
  object VariableDotStar : ProductAggregationToken
}

// Scala:
//trait EqualityBehavior
//object EqualityBehavior {
//  case object AnsiEquality    extends EqualityBehavior
//  case object NonAnsiEquality extends EqualityBehavior
//}
//
//trait ConcatBehavior
//object ConcatBehavior {
//  case object AnsiConcat    extends ConcatBehavior
//  case object NonAnsiConcat extends ConcatBehavior
//}
//
//sealed trait ProductAggregationToken
//object ProductAggregationToken {
//  case object Star            extends ProductAggregationToken
//  case object VariableDotStar extends ProductAggregationToken
//}
