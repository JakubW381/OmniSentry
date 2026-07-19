package dev.jakubw.omnisentry.agent

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.serialization.typeToken
import dev.jakubw.omnisentry.proto.analysis.AnalysisResponse
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.AnalysisResponseDto
import dev.jakubw.omnisentry.service.VisualDataDto
import io.ktor.server.engine.*
import kotlinx.serialization.Serializable

@Serializable
data class PromptDto(
    val customerId: String,
    val connectionId: String,
    val message: String,
)

abstract class BaseAgent(protected val grpcService : AnalysisGrpcService) : Agent {
    protected var lastAnalysisResult : AnalysisResponseDto? = null


    protected var systemPrompt : String = "You are an empathetic and professional Financial Advisor. Your primary role is to assist users in managing their expenses and identifying anomalies in their transactions.\n" +
            "\n" +
            "Communication Guidelines:\n" +
            "1. General Conversation: If the user greets you, asks about your identity, or engages in \"small talk,\" respond naturally and politely as a human advisor. Do not mention the names of your tools (e.g., \"ExpensesTool\") or provide example JSON schemas unless specifically asked for technical help.\n" +
            "2. Tool Usage: Use the provided tools (ExpensesTool or AnomalyTool) ONLY when the user explicitly requests data analysis, expense checking, or searching for errors in their history.\n" +
            "3. Technical Discretion: Never explain to the user which technical parameters (like customerId or connectionId) you need. If these are missing, the system will provide them automatically. Request missing information in a natural, conversational way without mentioning function structures.\n" +
            "4. Tone and Style: Be professional yet approachable. Your goal is to build trust and provide insights, not to sound like an API documentation or a technical manual."

    inner class AnomalyTool(
        private val analysisGrpcService : AnalysisGrpcService
    ) : SimpleTool<PromptDto>(
        argsType = typeToken<PromptDto>(),
        name = "AnomalyTool",
        description = "Tool used to get financial anomaly analysis of user transactions"
    ) {

        override suspend fun execute(args: PromptDto) : String {
            val analysisResponse = analysisGrpcService.getAnomalyAnalysis(args.customerId,args.connectionId)
            lastAnalysisResult = toDto(response = analysisResponse)

            println("---- AnomalyTool call ----")
            println("------- PromptDto")
            println(args.toString())
            println("------- Response")
            println(analysisResponse.toString())

            return analysisResponse.summaryForAi.toString()
        }
    }

    inner class ExpensesTool(
        private val analysisGrpcService : AnalysisGrpcService
    ) : SimpleTool<PromptDto>(
        argsType = typeToken<PromptDto>(),
        name = "ExpensesTool",
        description = "Tool used to get financial analysis of user expenses"
    ) {

        override suspend fun execute(args: PromptDto) : String {
            val analysisResponse = analysisGrpcService.getExpenseAnalysis(args.customerId,args.connectionId)
            lastAnalysisResult = toDto(response = analysisResponse)

            applicationEnvironment().log.info("---- ExpensesTool call ----")
            applicationEnvironment().log.info("------- PromptDto")
            applicationEnvironment().log.info(args.toString())
            applicationEnvironment().log.info("------- Response")
            applicationEnvironment().log.info(analysisResponse.toString())

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