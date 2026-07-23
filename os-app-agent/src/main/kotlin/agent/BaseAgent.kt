package dev.jakubw.omnisentry.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.FunctionalAIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.serialization.typeToken
import dev.jakubw.omnisentry.dto.ChatResponse
import dev.jakubw.omnisentry.dto.StreamEvent
import dev.jakubw.omnisentry.dto.TokenType
import dev.jakubw.omnisentry.proto.analysis.AnalysisResponse
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.AnalysisResponseDto
import dev.jakubw.omnisentry.service.VisualDataDto
import io.ktor.server.engine.*
import kotlinx.serialization.Serializable

@Serializable
data class PromptDto(
    val connectionId: String,
    val message: String,
)

@Serializable
data class ToolCallRequest(
    val connectionId: String,
    val customerId: String,
    val message: String,
)

abstract class BaseAgent(protected val grpcService : AnalysisGrpcService, protected val onEvent: (suspend(StreamEvent) -> Unit)) : Agent {
    protected var lastAnalysisResult : AnalysisResponseDto? = null

    protected var systemPrompt : String = "You are an empathetic and professional Financial Advisor. Your primary role is to assist users in managing their expenses and identifying anomalies in their transactions.\n" +
            "\n" +
            "Communication Guidelines:\n" +
            "1. General Conversation: If the user greets you, asks about your identity, or engages in \"small talk,\" respond naturally and politely as a human advisor. Do not mention the names of your tools (e.g., \"ExpensesTool\") or provide example JSON schemas unless specifically asked for technical help.\n" +
            "2. Tool Usage: Use the provided tools (ExpensesTool or AnomalyTool) ONLY when the user explicitly requests data analysis, expense checking, or searching for errors in their history.\n" +
            "3. Technical Discretion: Never explain to the user which technical parameters (like customerId or connectionId) you need. If these are missing, the system will provide them automatically. Request missing information in a natural, conversational way without mentioning function structures.\n" +
            "4. Tone and Style: Be professional yet approachable. Your goal is to build trust and provide insights, not to sound like an API documentation or a technical manual."

    protected var toolRegistry = ToolRegistry{
        tool(ExpensesTool(grpcService))
        tool(AnomalyTool(grpcService))
    }

    protected fun GraphAIAgent.FeatureContext.configureEventHandler(){
        handleEvents {
            onToolCallStarting { toolCall ->
                println("Tool call starting: ${toolCall.toolName}")
                onEvent(
                    StreamEvent.Token(
                        tokenType = TokenType.TOOL,
                        content = "Calling tool: ${toolCall.toolName}"
                    )
                )
            }

            onLLMStreamingFrameReceived { context ->
                when (val frame = context.streamFrame) {
                    is StreamFrame.TextDelta -> {
                        if (frame.text.isNotEmpty()) {
                            onEvent(StreamEvent.Token(TokenType.TEXT, frame.text))
                        }
                    }
                    is StreamFrame.ReasoningDelta -> {
                        val thinkText = frame.text ?: frame.summary
                        if (!thinkText.isNullOrEmpty()) {
                            onEvent(StreamEvent.Token(TokenType.REASONING, thinkText))
                        }
                    }
                    is StreamFrame.ToolCallDelta -> {
                        frame.content?.let { content ->
                            onEvent(StreamEvent.Token(TokenType.TOOL, content))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    inner class AnomalyTool(
        private val analysisGrpcService : AnalysisGrpcService
    ) : SimpleTool<ToolCallRequest>(
        argsType = typeToken<ToolCallRequest>(),
        name = "AnomalyTool",
        description = "Tool used to get financial anomaly analysis of user transactions"
    ) {

        override suspend fun execute(args: ToolCallRequest) : String {
            val analysisResponse = analysisGrpcService.getAnomalyAnalysis(args.customerId,args.connectionId)
            lastAnalysisResult = toDto(response = analysisResponse)
            return analysisResponse.summaryForAi.toString()
        }
    }

    inner class ExpensesTool(
        private val analysisGrpcService : AnalysisGrpcService
    ) : SimpleTool<ToolCallRequest>(
        argsType = typeToken<ToolCallRequest>(),
        name = "ExpensesTool",
        description = "Tool used to get financial analysis of user expenses"
    ) {
        override suspend fun execute(args: ToolCallRequest) : String {
            val analysisResponse = analysisGrpcService.getExpenseAnalysis(args.customerId,args.connectionId)
            lastAnalysisResult = toDto(response = analysisResponse)
            return analysisResponse.toString()

        }
    }

    protected fun toDto(response : AnalysisResponse) : AnalysisResponseDto{
        return AnalysisResponseDto(
            summaryForAi = response.summaryForAi,
            visualDataByCurrency = response.visualDataByCurrencyMap.map {
                visualDataDto -> Pair(visualDataDto.key, VisualDataDto(visualDataDto.value.labelsList,visualDataDto.value.valuesList,visualDataDto.value.threshold))
            }.toMap(),
            alertLevel = response.alertLevel
        )
    }
}