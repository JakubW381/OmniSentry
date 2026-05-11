package dev.jakubw.omnisentry.agent

interface Agent {
    suspend fun chat(message : String) : ChatResponse
}