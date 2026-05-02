package dev.jakubw.omnisentry
package analyser

import proto.transactions.TransactionDto

import java.time.ZonedDateTime

object Extractor {
  def toFeatures(dto : TransactionDto) : Array[Double] = {
    val date = ZonedDateTime.parse(dto.madeOn)
    Array(
      dto.amount,
      date.getHour.toDouble,
      date.getDayOfWeek.getValue.toDouble,
      dto.category.toDouble
    )
  }
}
