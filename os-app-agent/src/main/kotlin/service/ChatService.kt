package dev.jakubw.omnisentry.service

import dev.jakubw.omnisentry.agent.Agent
import dev.jakubw.omnisentry.agent.PromptDto
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.repository.Message
import dev.jakubw.omnisentry.repository.MessageRepository
import dev.jakubw.omnisentry.repository.MessageRole

class ChatService(
    private val repository : MessageRepository,
    private val agent : Agent
) {

    suspend fun getHistory(range: IntRange, customerId: String) : List<Message>{
        return repository.getMessages(range, customerId);
    }

    suspend fun sendPrompt(prompt: PromptDto) : ChatResponse {
        val message = Message(
            customerId = prompt.customerId,
            text = prompt.message,
            role = MessageRole.USER,
            analysis = null
        )
        repository.saveMessage(message)

        val response = agent.chat("message : ${prompt.message}, \n customerId : ${prompt.customerId}, \n connectionId : ${prompt.connectionId}")

        val agentMessage = Message(
            customerId = prompt.customerId,
            text = null,
            role = MessageRole.ASSISTANT,
            analysis = response
        )
        repository.saveMessage(agentMessage)

        return response
    }
}