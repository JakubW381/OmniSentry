package dev.jakubw.omnisentry.dto

import dev.jakubw.omnisentry.service.AnalysisResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val message: String,
    val analysis : AnalysisResponseDto? = null
)