package dev.jakubw.omnisentry

import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.jakubw.omnisentry.agent.BaseAgent
import dev.jakubw.omnisentry.agent.ChatResponse
import dev.jakubw.omnisentry.agent.GroqAgent
import dev.jakubw.omnisentry.agent.OllamaAgent
import dev.jakubw.omnisentry.agent.PromptDto
import dev.jakubw.omnisentry.proto.analysis.AnalysisServiceGrpc
import dev.jakubw.omnisentry.repository.MongoMessageRepository
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import io.grpc.ManagedChannelBuilder
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

val appModule = module {
    single {
        try {
            val grpcHost = System.getenv("GRPC_HOST") ?: "localhost"
            val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 9090
            println("DI: Initializing gRPC Channel to $grpcHost:$grpcPort")

            ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build()
        } catch (e: Exception) {
            println("DI ERROR: Failed to create gRPC Channel: ${e.message}")
            throw e
        }
    }
    val mongo = module {
        // Client
        single{
            val uri = System.getenv("AGENT_DB_URI") ?: "mongodb://localhost:27017"
            MongoClient.create(uri)
        }
        // Database
        single{
            val dbName = System.getenv("AGENT_DB_NAME") ?: "omnisentry"
            val client = get<MongoClient>()
            client.getDatabase(dbName)
        }

        // Repository
        //  --- Messages
        single {
            MongoMessageRepository(get())
        }
    }

    single {
        val channel = get<io.grpc.ManagedChannel>()
        AnalysisServiceGrpc.newBlockingStub(channel)
    }

    single { AnalysisGrpcService(get()) }

    if (System.getenv("AGENT_TYPE") == "GROQ") {
        factory<BaseAgent> { GroqAgent(get()) }
    } else {
        factory<BaseAgent> { OllamaAgent(get()) }
    }
}

fun main() {
    println("Starting OmniSentry AI Agent Server on port 8085...")

    embeddedServer(Netty, port = 8085) {
        install(Koin) {
            modules(appModule)
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        routing {

            post("/ai/message") {
                val agent by inject<BaseAgent>()
                try {
                    val prompt = call.receive<PromptDto>()
                    val response : ChatResponse = agent.chat("message : ${prompt.message}, \n customerId : ${prompt.customerId}, \n connectionId : ${prompt.connectionId}")
                    call.respond(response)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respondText("Error: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
                }
            }

            get("/ai/health") {
                call.respondText("OK")
            }
        }
    }.start(wait = true)
}