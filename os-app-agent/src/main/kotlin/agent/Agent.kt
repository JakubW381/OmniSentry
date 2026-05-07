package dev.jakubw.omnisentry.agent

interface Agent {
    suspend fun chat(message : String) : String
    suspend fun getExpenseAnalysis(rawData : String) : String
    suspend fun getAnomalyAnalysis(logs : List<String>) : String
}