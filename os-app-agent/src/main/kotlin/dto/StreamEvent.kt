package dev.jakubw.omnisentry.dto

import dev.jakubw.omnisentry.service.AnalysisResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StreamEvent {

    @Serializable
    data class Token(
        val tokenType: TokenType,
        val content: String
    ) : StreamEvent

    @Serializable
    data class Complete(
        val response: AnalysisResponseDto
    ) : StreamEvent
}


enum class TokenType {
    TEXT, REASONING, TOOL
}
