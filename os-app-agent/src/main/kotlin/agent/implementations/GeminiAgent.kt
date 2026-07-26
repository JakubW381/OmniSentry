package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.collectText
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.dto.StreamEvent
import dev.jakubw.omnisentry.service.AnalysisGrpcService


/**
 * Works perfectly with default (non-streaming) strategy /message
 */


class GeminiAgent(grpcService: AnalysisGrpcService,
                  onEvent: suspend (StreamEvent) -> Unit,
                  promptExecutor: PromptExecutor
) : BaseAgent(grpcService,
    onEvent,
    promptExecutor
) {

    val geminiConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7
            )
        ){
            system(systemPrompt)
        },
        model = GoogleModels.Gemini3_Flash_Preview,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig = geminiConfig,
        toolRegistry = toolRegistry,
//        strategy = streamingStrategy
    ){
        configureEventHandler()
    }

    override suspend fun chat(message: String): ChatResponse {
        lastAnalysisResult = null

        val aiFinalText = agent.run(message)
        return ChatResponse(
            message = aiFinalText,
            analysis = lastAnalysisResult
        )
    }
}