package dev.jakubw.omnisentry.repository

import kotlinx.serialization.Serializable
import kotlin.time.Instant


interface MessageRepository {
        suspend fun saveMessage(message : Message)
        suspend fun getMessages(range: IntRange , customerId : String) : List<Message>
}