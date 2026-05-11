package dev.jakubw.omnisentry.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.base.OpenAIBaseSettings
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor
import io.ktor.server.engine.applicationEnvironment

class GroqAgent : Agent {

//    val groqExecutor = simpleOpenAIExecutor().1
//    )
//
//    val agent = AIAgent(
//        promptExecutor =  groqExecutor,
//        llmModel =
//    )


    override suspend fun chat(message: String): ChatResponse {
        TODO("Not yet implemented")
    }
}