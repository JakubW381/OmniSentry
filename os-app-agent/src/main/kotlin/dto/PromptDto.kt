package dev.jakubw.omnisentry.dto

import kotlinx.serialization.Serializable

@Serializable
data class PromptDto(
    val customerId: String,
    val connectionId: String,
    val message: String,
)