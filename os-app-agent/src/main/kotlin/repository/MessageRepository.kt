package dev.jakubw.omnisentry.repository


interface MessageRepository {
        suspend fun ensureIndexes()
        suspend fun saveMessage(message : Message)
        suspend fun getMessages(range: IntRange , customerId : String) : List<Message>
}