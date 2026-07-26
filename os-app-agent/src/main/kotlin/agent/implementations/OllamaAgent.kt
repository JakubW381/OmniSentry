package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
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
 * Streaming strategy works fine for normal prompts, but tool calls doesn't work
 * however tool calls works fine with singleRunStrategy()
 */

class OllamaAgent(grpcService: AnalysisGrpcService,
                  onEvent: suspend (StreamEvent) -> Unit,
                  promptExecutor: PromptExecutor
) : BaseAgent(grpcService,
    onEvent,
    promptExecutor
) {

    val localLLama = LLModel(
        id = "qwen3:8b",
        provider = LLMProvider.Ollama,
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        ),
        contextLength = 131_072,
        maxOutputTokens = 4096,
    )

    val llamaConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7

            )
        ){
            system(systemPrompt)
        },
        model = localLLama,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = MultiLLMPromptExecutor(
            LLMProvider.Ollama to OllamaClient(baseUrl = "http://host.docker.internal:11434")
        ),
        agentConfig = llamaConfig,
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