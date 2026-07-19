package dev.jakubw.omnisentry
package analyser

import dto.{AnalysisResult, VisualData}
import proto.transactions.TransactionDto

import smile.anomaly.IsolationForest

import java.util.Properties

class Analyser {

  def expenseAnalysis(transactions: Seq[TransactionDto]): AnalysisResult = {
    val byCurrency = transactions.groupBy(_.currency)

    val visualDataByCurrency: Map[String, VisualData] = byCurrency.map { case (currency, txsInCurrency) =>
      val categoryMap = txsInCurrency
        .groupBy(tx => if (tx.category.trim.isEmpty) "other" else tx.category)
        .map { case (cat, t) => cat -> t.map(_.amount).sum }

      val sorted = categoryMap.toSeq.sortBy(-_._2)

      currency -> VisualData(
        labels = sorted.map(_._1),
        values = sorted.map(_._2),
        threshold = 0.0
      )
    }

    val totalsString = byCurrency.map { case (curr, txs) =>
      s"${"%.2f".format(txs.map(_.amount).sum)} $curr"
    }.mkString(", ")

    val topCategory = transactions
      .groupBy(tx => if (tx.category.trim.isEmpty) "other" else tx.category)
      .view.mapValues(_.size).toMap
      .maxByOption(_._2).map(_._1).getOrElse("none")

    val summary = s"User spent in total: $totalsString. Top Category: $topCategory."

    AnalysisResult(
      summaryForAI = summary,
      visualData = visualDataByCurrency,
      alertLevel = "INFO"
    )
  }

  def anomalyAnalysis(transactions: Seq[TransactionDto], threshold: Double = 0.70): AnalysisResult = {
    if (transactions.size < 10) {
      return AnalysisResult("Sample is too small for anomaly analysis.", Map.empty, "LOW")
    }
    val data = transactions.map(Extractor.toFeatures).toArray
    val properties = new Properties()
    properties.setProperty("smile.isolation_forest.trees", "100")

    val model = IsolationForest.fit(data, properties)
    val scores = model.score(data)

    val txWithScores = transactions.zip(scores)

    val visualDataByCurrency = txWithScores.groupBy(_._1.currency).map { case (curr, pairs) =>
      curr -> VisualData(
        labels = pairs.map { case (dto, _) =>
          val shortDate = dto.madeOn.take(10)
          val shortDesc = dto.description.take(12)
          s"$shortDate ($shortDesc...)"
        },
        values = pairs.map(_._2),
        threshold = threshold
      )
    }

    val anomaliesFound = scores.count(_ > threshold)
    val maxScore = if (scores.nonEmpty) scores.max else 0.0
    val alert = if (anomaliesFound > 0) "HIGH" else "STABLE"

    val summary = s"Anomalies Found: $anomaliesFound. Highest Z score: ${"%.2f".format(maxScore)}."

    AnalysisResult(summary, visualDataByCurrency, alert)
  }
}