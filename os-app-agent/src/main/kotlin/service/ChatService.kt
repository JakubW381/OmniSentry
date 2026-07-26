package dev.jakubw.omnisentry.service

import dev.jakubw.omnisentry.agent.PromptDto
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.repository.Message
import dev.jakubw.omnisentry.repository.MessageRepository
import dev.jakubw.omnisentry.repository.MessageRole

class ChatService(
    private val repository : MessageRepository
) {

    suspend fun getHistory(range: IntRange, customerId: String) : List<Message>{
        return repository.getMessages(range, customerId);
    }
    suspend fun savePrompt(prompt: PromptDto, customerId: String) {
        val message = Message(
            customerId = customerId,
            text = prompt.message,
            role = MessageRole.USER,
            analysis = null
        )
        repository.saveMessage(message)
    }
    suspend fun saveLLMessage(response: ChatResponse, customerId: String){
        val agentMessage = Message(
            customerId = customerId,
            text = null,
            role = MessageRole.ASSISTANT,
            analysis = response
        )
        repository.saveMessage(agentMessage)
    }
    suspend fun saveSystemMessage(response: String, customerId: String){
        val agentMessage = Message(
            customerId = customerId,
            text = response,
            role = MessageRole.SYSTEM,
            analysis = null
        )
        repository.saveMessage(agentMessage)
    }

}