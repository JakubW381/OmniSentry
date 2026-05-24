package dev.jakubw.omnisentry
package service

import analyser.*
import dto.AnalysisResult
import proto.analysis.*
import proto.analysis.analysis.{AnalysisRequest, AnalysisResponse, AnalysisServiceGrpc, VisualDataDto}
import proto.transactions.*

import io.grpc.ManagedChannelBuilder

import scala.concurrent.{ExecutionContext, Future}

class AnalysisService(host : String , port : Int)(implicit ec : ExecutionContext) extends AnalysisServiceGrpc.AnalysisService {

  private final val analyser = new Analyser()

  private val channel = ManagedChannelBuilder.forAddress(host , port).usePlaintext().build()
  private val historyStub = AnalyticsDataServiceGrpc.stub(channel)

  override def getExpenseAnalysis(request: AnalysisRequest): Future[AnalysisResponse] = {
    val historyRequest = HistoryRequest(request.userId,request.connectionId)

    historyStub.getHistory(historyRequest).map{history =>
      val result = analyser.expenseAnalysis(history.transactions)
      mapToProto(result)
    }
  }

  override def getAnomalyAnalysis(request: AnalysisRequest): Future[AnalysisResponse] = {
    val historyRequest = HistoryRequest(request.userId, request.connectionId)

    historyStub.getHistory(historyRequest).map { history =>
      val result = analyser.anomalyAnalysis(history.transactions)
      mapToProto(result)
    }
  }

  private def mapToProto(result: AnalysisResult): AnalysisResponse = {
    val protoVisualData = result.visualData.map { case (currency, data) =>
      currency -> VisualDataDto(
        labels = data.labels,
        values = data.values,
        threshold = data.threshold
      )
    }

    AnalysisResponse(
      summaryForAi = result.summaryForAI,
      visualDataByCurrency = protoVisualData,
      alertLevel = result.alertLevel
    )
  }
}

