package dev.jakubw.omnisentry.agent

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
import dev.jakubw.omnisentry.service.AnalysisGrpcService

class GroqAgent(grpcService: AnalysisGrpcService) : BaseAgent(grpcService) {

    val settings = OpenAIClientSettings(baseUrl = "https://api.groq.com/openai/v1")
    val client = OpenAILLMClient(settings = settings , apiKey = System.getenv("GROQ_API_KEY"))
    val executer = MultiLLMPromptExecutor(client)

    val groqModel = LLModel(
        id = "openai/gpt-oss-20b",
        provider = LLMProvider.OpenAI,
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        )
    )

    val agent = AIAgent(
        promptExecutor = executer,
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