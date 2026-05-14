package dev.jakubw.omnisentry.agent

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
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.AnalysisResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val message: String,
    val analysis : AnalysisResponseDto? = null
)
class OllamaAgent(grpcService: AnalysisGrpcService) : BaseAgent(grpcService) {

    val localLLama = LLModel(
        id = "llama3.1:8b-instruct-q4_K_M",
        provider = LLMProvider.Ollama,
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        )
    )

    val llamaConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7
            )
        ){
            system("You are an empathetic and professional Financial Advisor. Your primary role is to assist users in managing their expenses and identifying anomalies in their transactions.\n" +
                    "\n" +
                    "Communication Guidelines:\n" +
                    "1. General Conversation: If the user greets you, asks about your identity, or engages in \"small talk,\" respond naturally and politely as a human advisor. Do not mention the names of your tools (e.g., \"ExpensesTool\") or provide example JSON schemas unless specifically asked for technical help.\n" +
                    "2. Tool Usage: Use the provided tools (ExpensesTool or AnomalyTool) ONLY when the user explicitly requests data analysis, expense checking, or searching for errors in their history.\n" +
                    "3. Technical Discretion: Never explain to the user which technical parameters (like customerId or connectionId) you need. If these are missing, the system will provide them automatically. Request missing information in a natural, conversational way without mentioning function structures.\n" +
                    "4. Tone and Style: Be professional yet approachable. Your goal is to build trust and provide insights, not to sound like an API documentation or a technical manual.")
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