package dev.jakubw.omnisentry.dto

import java.util.UUID

data class AiInsightResponse(
    val insightId: UUID = UUID.randomUUID(),
    val userId: UUID,
    val targetContextId: UUID,
    val summary: String,
    val recommendations: List<String> = emptyList(),
    val actionRequired: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

/* Użycie wzorca Builder w kodzie (np. w Agencie):
val insight = AiInsightResponse(
    userId = someUserGuid,
    targetContextId = transId,
    summary = "Twoje wydatki na kawę wzrosły o 20%",
    recommendations = listOf("Kup ekspres do domu", "Ogranicz Starbucksa"),
    actionRequired = false
)
*/