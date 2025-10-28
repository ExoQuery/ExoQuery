package io.exoquery.sample

import io.exoquery.annotation.CapturedFunction
import io.exoquery.sql

fun main() {
  data class Stock(val weight: Double, val sharesOutstanding: Int, val price: Double, val earnings: Double)

  @CapturedFunction
  fun peRatioWeighted(stock: Stock, weight: Double) = sql.expression {
    (stock.price / stock.earnings) * weight
  }

  // A extension function used in the query!
  @CapturedFunction
  fun Stock.marketCap() = sql.expression {
    price * sharesOutstanding
  }

  val q = sql {
    val totalWeight = Table<Stock>().map { it.marketCap().use }.sum() // A local variable used in the query!
    Table<Stock>().map { stock -> peRatioWeighted(stock, stock.marketCap().use / totalWeight) }
  }
  println(q.buildFor.Postgres().value)
  // SELECT (stock.price / stock.earnings) * ((this.price * this.sharesOutstanding) / (SELECT sum(this.price * this.sharesOutstanding) FROM Stock it)) AS value FROM Stock stock
}
