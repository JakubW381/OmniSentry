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

            applicationEnvironment().log.info("---- AnomalyTool call ----")
            applicationEnvironment().log.info("------- PromptDto")
            applicationEnvironment().log.info(args.toString())
            applicationEnvironment().log.info("------- Response")
            applicationEnvironment().log.info(analysisResponse.toString())

            return analysisResponse.toString()
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