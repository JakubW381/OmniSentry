package dev.jakubw.omnisentry.repository

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import dev.jakubw.omnisentry.agent.ChatResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import kotlin.time.Duration.Companion.milliseconds
import java.time.Instant

data class MessageEntity(
    @BsonId val id: ObjectId = ObjectId(),
    val customerId: String,
    val text: String? = "",
    val chatResponse: ChatResponse? = null,
    val role: MessageRole,
    val timestamp: Instant = Instant.now(),
){
    fun toDto(): Message{
        return Message(
            customerId = customerId,
            text = text,
            role = role,
            timestamp = timestamp.toEpochMilli(),
            analysis = chatResponse
        )
    }
}

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
data class Message(
    val customerId: String,
    val text: String?,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val analysis: ChatResponse?
){
    fun toEntity(): MessageEntity{
        return MessageEntity(
            customerId = customerId,
            text = text,
            role = role,
            timestamp = Instant.ofEpochMilli(timestamp),
            chatResponse = analysis
        )
    }
}

class MongoMessageRepository(
    private val database: MongoDatabase
) : MessageRepository {

    private val collection = database.getCollection<MessageEntity>("messages")

    override suspend fun ensureIndexes() {
        collection.createIndex(Indexes.ascending("customerId", "timestamp"))
    }

    override suspend fun saveMessage(message: Message) {
        try {
            withTimeout(2000.milliseconds) {
                collection.insertOne(message.toEntity())
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to save message for ${message.customerId}", e)
        }
    }

    override suspend fun getMessages(range: IntRange, customerId: String): List<Message> {
        return collection.find(eq("customerId", customerId))
            .sort(Sorts.descending("timestamp"))
            .limit(range.count())
            .toList()
            .map { it.toDto() }
    }
}