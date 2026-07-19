package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.AnalysisResponseDto
import kotlinx.serialization.Serializable


class OllamaAgent(grpcService: AnalysisGrpcService) : BaseAgent(grpcService) {

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
        promptExecutor = simpleOllamaAIExecutor(
            System.getenv("OLLAMA_HOST") ?: "http://localhost:11434"
        ),
        agentConfig = llamaConfig,
        toolRegistry = ToolRegistry{
            tool(ExpensesTool(grpcService))
            tool(AnomalyTool(grpcService))
        }
    ){
        handleEvents {
            onToolCallStarting { toolCall ->
                println("Tool call starting: ${toolCall.toolName}")
            }
        }
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