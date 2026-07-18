package dev.jakubw.omnisentry

import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.jakubw.omnisentry.agent.*
import dev.jakubw.omnisentry.proto.analysis.AnalysisServiceGrpc
import dev.jakubw.omnisentry.repository.*
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.ChatService
import io.grpc.ManagedChannelBuilder
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("dev.jakubw.omnisentry.App")

val appModule: Module = module {
    single {
        try {
            val grpcHost = System.getenv("GRPC_HOST") ?: "localhost"
            val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 9090
            logger.info("DI: Initializing gRPC Channel to $grpcHost:$grpcPort")

            ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build()
        } catch (e: Exception) {
            logger.error("DI ERROR: Failed to create gRPC Channel: ${e.message}")
            throw e
        }
    }

    single {
        val channel = get<io.grpc.ManagedChannel>()
        AnalysisServiceGrpc.newBlockingStub(channel)
    }

    single { AnalysisGrpcService(get()) }

    single {
        val uri = System.getenv("AGENT_DB_URI") ?: "mongodb://mongo-db:27017"
        MongoClient.create(uri)
    }

    single {
        val dbName = System.getenv("AGENT_DB_NAME") ?: "omnisentry"
        val client = get<MongoClient>()
        client.getDatabase(dbName)
    }

    single<MessageRepository> {
        MongoMessageRepository(get())
    }

    if (System.getenv("AGENT_TYPE") == "GROQ") {
        factory<Agent> { GroqAgent(get()) }
    } else {
        factory<Agent> { OllamaAgent(get()) }
    }

    factory<ChatService> {
        ChatService(get(), get())
    }
}

fun main() {
    logger.info("Starting OmniSentry AI Agent Server on port 8085...")

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

        val repository by inject<MessageRepository>()
        val chatService by inject<ChatService>()

        monitor.subscribe(ApplicationStarted) {
            launch {
                try {
                    logger.info("Initializing MongoDB indexes...")
                    repository.ensureIndexes()
                    logger.info("MongoDB indexes checked/created successfully.")
                } catch (e: Exception) {
                    logger.error("Failed to create MongoDB indexes on startup: ${e.message}", e)
                }
            }
        }

        routing {
            post("/ai/message") {
                try {
                    val prompt = call.receive<PromptDto>()
                    val response = chatService.sendPrompt(prompt)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            get("/ai/history") {
                val queryParams = call.request.queryParameters

                val customerId = queryParams["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing 'customerId' query parameter")

                val rangeInt = queryParams["range"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid 'range' query parameter")

                val range: IntRange = 0..rangeInt

                try {
                    val response = chatService.getHistory(range, customerId)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            get("/ai/health") {
                call.respondText("OK")
            }
        }
    }.start(wait = true)
}