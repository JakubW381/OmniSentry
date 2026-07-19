package dev.jakubw.omnisentry.agent.implementations

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.service.AnalysisGrpcService

class GeminiAgent(grpcService: AnalysisGrpcService) : BaseAgent(grpcService) {

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
        promptExecutor = simpleGoogleAIExecutor(System.getenv("GEMINI_API_KEY")),
        agentConfig = geminiConfig,
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