package dev.jakubw.omnisentry.dto

import java.time.Instant
import java.util.UUID


sealed interface AnalysisResult {
    val transactionId: UUID
    val timestamp: Instant
    val score: Double
}

data class FraudAnalysisResult(
    override val transactionId: UUID,
    override val timestamp: Instant,
    override val score: Double, // 0.0 - 1.0 (im wyżej tym gorzej)
    val flag: RiskLevel,
    val detectedAnomalies: List<String>
) : AnalysisResult

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class SpendingAnalysisResult(
    override val transactionId: UUID,
    override val timestamp: Instant,
    override val score: Double,
    val category: String,
    val aiPersonalNote: String,
    val isRecurring: Boolean
) : AnalysisResult