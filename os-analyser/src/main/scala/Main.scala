package dev.jakubw.omnisentry

import proto.analysis.analysis.AnalysisServiceGrpc
import service.AnalysisService

import dev.jakubw.omnisentry.util.OpenTelemetryInit
import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry

import scala.concurrent.ExecutionContextExecutor

object Main {
  def main(args: Array[String]): Unit = {

    val openTelemetry = OpenTelemetryInit.getOpenTelemetry(System.getenv("OTEL_SERVICE_NAME"))
    val grpcTelemetry = GrpcTelemetry.create(openTelemetry)

    implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

    val port = 9093
    val backendHost = "os-main-backend"
    val backendPort = 9092
    val channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
      .usePlaintext()
      .intercept(grpcTelemetry.createClientInterceptor())
      .build()

    val serviceImpl = new AnalysisService(channel)

    val server = ServerBuilder
      .forPort(port)
      .addService(AnalysisServiceGrpc.bindService(serviceImpl, ec))
      .intercept(grpcTelemetry.createServerInterceptor())
      .build()

    println(s"Starting server on port $port")

    server.start()

    sys.addShutdownHook {
      println("Shutting down gRPC server since JVM is shutting down")
      server.shutdown()
      channel.shutdown()
    }

    server.awaitTermination()
  }
}
