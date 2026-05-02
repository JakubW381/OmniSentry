package dev.jakubw.omnisentry
package dto

case class VisualData(labels: Seq[String], values: Seq[Double], threshold: Double)

case class AnalysisResult(
                           summaryForAI: String,
                           visualData: Map[String, VisualData],
                           alertLevel: String
                         )

case class AnomalyResult(
                          transactionId: String,
                          score: Double,
                          isAnomaly: Boolean
                        )