package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.modelsById
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.collectText
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.dto.StreamEvent
import dev.jakubw.omnisentry.service.AnalysisGrpcService


/**
 * Doesn't work for now, cause of OpenAI
 */

class GroqAgent(grpcService: AnalysisGrpcService,
                onEvent: suspend (StreamEvent) -> Unit,
                promptExecutor: PromptExecutor
) : BaseAgent(grpcService,
    onEvent,
    promptExecutor
) {
    private val groqModel: LLModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "gpt-oss-20b",
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        ),
        contextLength = 131_072,
        maxOutputTokens = 131_072
    )
    val groqConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7
            )
        ){
            system(systemPrompt)
        },
        model = groqModel,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig = groqConfig,
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