package dev.jakubw.omnisentry.service

import dev.jakubw.omnisentry.proto.analysis.AnalysisRequest
import dev.jakubw.omnisentry.proto.analysis.AnalysisResponse
import dev.jakubw.omnisentry.proto.analysis.AnalysisServiceGrpc
import kotlinx.serialization.Serializable


@Serializable
data class AnalysisResponseDto(
    val summaryForAi : String,
    val visualDataByCurrency : Map<String, VisualDataDto>,
    val alertLevel : String
)
@Serializable
data class VisualDataDto(
    val labels : List<String>,
    val values : List<Double>,
    val threshold : Double
)

class AnalysisGrpcService(
    private val stub : AnalysisServiceGrpc.AnalysisServiceBlockingStub
) {
    fun getExpenseAnalysis(userId : String, connectionId : String) : AnalysisResponse {
        val grpcRequest = buildRequest(userId, connectionId)
        val response = stub.getExpenseAnalysis(grpcRequest)
        return response
    }

    fun getAnomalyAnalysis(userId : String, connectionId : String) : AnalysisResponse {
        val grpcRequest = buildRequest(userId, connectionId)
        val response = stub.getAnomalyAnalysis(grpcRequest)
        return response
    }

    private fun buildRequest(userId : String, connectionId : String) : AnalysisRequest {
        return AnalysisRequest.newBuilder()
            .setUserId(userId)
            .setConnectionId(connectionId)
            .build()
    }
}