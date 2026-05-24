package dev.jakubw.omnisentry

import proto.analysis.analysis.AnalysisServiceGrpc
import service.AnalysisService

import io.grpc.ServerBuilder

import scala.concurrent.ExecutionContextExecutor

object Main {
  def main(args: Array[String]): Unit = {

    implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

    val port = 9093
    val backendHost = "os-main-backend"
    val backendPort = 9092

    val serviceImpl = new AnalysisService(backendHost, backendPort)

    val server = ServerBuilder
      .forPort(port)
      .addService(AnalysisServiceGrpc.bindService(serviceImpl, ec))
      .build()

    println(s"Starting server on port $port")

    server.start()

    sys.addShutdownHook {
      println("Shutting down gRPC server since JVM is shutting down")
      server.shutdown()
    }

    server.awaitTermination()
  }
}
