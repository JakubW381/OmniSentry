package dev.jakubw.omnisentry.agent

import dev.jakubw.omnisentry.dto.ChatResponse

interface Agent {
    suspend fun chat(message : String) : ChatResponse
}