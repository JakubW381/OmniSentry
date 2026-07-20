package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.dto.StreamEvent
import dev.jakubw.omnisentry.service.AnalysisGrpcService


class GroqAgent(grpcService: AnalysisGrpcService, onEvent: suspend (StreamEvent) -> Unit) : BaseAgent(grpcService, onEvent) {

    val settings = OpenAIClientSettings(baseUrl = "https://api.groq.com/openai/v1")

    val client = OpenAILLMClient(settings = settings, apiKey = System.getenv("GROQ_API_KEY"))

    val executor = MultiLLMPromptExecutor(client)
    val groqModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "openai/gpt-oss-20b",
        contextLength = 131_072,
        maxOutputTokens = 4096,
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        )
    )

    val agent = AIAgent(
        promptExecutor = executor,
        agentConfig = AIAgentConfig(
            prompt(
                id = "assistant",
                params = LLMParams(
                    temperature = 0.7
                )
            ) {
                system(systemPrompt)
            },
            model = groqModel,
            maxAgentIterations = 10
        ),
        toolRegistry = toolRegistry
    ) {
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