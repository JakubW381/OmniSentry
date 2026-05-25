package dev.jakubw.omnisentry
package analyser

import proto.transactions.TransactionDto

import java.time.LocalDate

object Extractor {
  def toFeatures(dto : TransactionDto) : Array[Double] = {
    val date = LocalDate.parse(dto.madeOn)
    Array(
      dto.amount,
      dto.description.toUpperCase.hashCode.toDouble,
      dto.currency.toUpperCase.hashCode.toDouble,
      date.getDayOfYear.toDouble,
      date.getDayOfMonth.toDouble,
      date.getDayOfWeek.getValue.toDouble,
      dto.category.toUpperCase.hashCode.toDouble,
    )
  }
}
