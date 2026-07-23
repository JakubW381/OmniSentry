package dev.jakubw.omnisentry

import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.jakubw.omnisentry.agent.Agent
import dev.jakubw.omnisentry.agent.PromptDto
import dev.jakubw.omnisentry.agent.implementations.GeminiAgent
import dev.jakubw.omnisentry.agent.implementations.GroqAgent
import dev.jakubw.omnisentry.agent.implementations.OllamaAgent
import dev.jakubw.omnisentry.dto.StreamEvent
import dev.jakubw.omnisentry.proto.analysis.AnalysisServiceGrpc
import dev.jakubw.omnisentry.repository.MessageRepository
import dev.jakubw.omnisentry.repository.MongoMessageRepository
import dev.jakubw.omnisentry.service.AnalysisGrpcService
import dev.jakubw.omnisentry.service.ChatService
import dev.jakubw.omnisentry.util.getOpenTelemetry
import io.grpc.ManagedChannelBuilder
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("dev.jakubw.omnisentry.App")

fun createAppModule(openTelemetry: OpenTelemetry): Module = module {
    single { openTelemetry }

    single {
        val grpcTelemetry = GrpcTelemetry.create(get<OpenTelemetry>())
        val grpcHost = System.getenv("GRPC_HOST") ?: "localhost"
        val grpcPort = System.getenv("GRPC_PORT")?.toInt() ?: 9090
        logger.info("DI: Initializing gRPC Channel to $grpcHost:$grpcPort")

        ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
            .intercept(grpcTelemetry.createClientInterceptor())
            .usePlaintext()
            .build()
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

    factory<Agent>(named("GROQ")) { (onEvent: suspend (StreamEvent) -> Unit) ->
        GroqAgent(get(), onEvent)
    }
    factory<Agent>(named("OLLAMA")) { (onEvent: suspend (StreamEvent) -> Unit) ->
        OllamaAgent(get(), onEvent)
    }
    factory<Agent>(named("GEMINI")) { (onEvent: suspend (StreamEvent) -> Unit) ->
        GeminiAgent(get(), onEvent)
    }

    factory<Agent> { (onEvent: suspend (StreamEvent) -> Unit) ->
        val agentType = System.getenv("AGENT_TYPE") ?: "OLLAMA"

        try {
            get<Agent>(named(agentType)) { parametersOf(onEvent) }
        } catch (e: Exception) {
            throw IllegalArgumentException("Unsupported AGENT_TYPE: $agentType", e)
        }
    }

    single { ChatService(get()) }
}

fun main() {
    val serviceName = System.getenv("OTEL_SERVICE_NAME") ?: "os-app-agent"
    val openTelemetry = getOpenTelemetry(serviceName)

    logger.info("Starting OmniSentry AI Agent Server on port 8085...")

    embeddedServer(Netty, port = 8085) {
        install(KtorServerTelemetry) {
            setOpenTelemetry(openTelemetry)
        }

        install(Koin) {
            modules(createAppModule(openTelemetry))
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
                var customerId: String? = null
                try {
                    val prompt = call.receive<PromptDto>()
                    customerId = call.request.headers["X-User-CustomerId"]

                    chatService.savePrompt(prompt)

                    val noopAgent = get<Agent> { parametersOf(suspend { _: StreamEvent -> }) }
                    val response = noopAgent.chat("message : ${prompt.message}, \n customerId : ${customerId}, \n connectionId : ${prompt.connectionId}")

                    chatService.saveLLMessage(response, prompt.message)
                    call.respond(response)
                } catch (e: Exception) {
                    logger.error("Error processing message", e)
                    customerId?.let { id ->
                        try {
                            chatService.saveSystemMessage("Error: ${e.message}\n Status: ${HttpStatusCode.InternalServerError}", id)
                        } catch (dbEx: Exception) {
                            logger.error("Failed to save system error message to database", dbEx)
                        }
                    }

                    call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            post("/ai/sse") {
                var customerId: String? = null
                try {
                    val prompt = call.receive<PromptDto>()
                    customerId = call.request.headers["X-User-CustomerId"]

                    chatService.savePrompt(prompt)

                    call.response.headers.append(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
                    call.response.headers.append(HttpHeaders.CacheControl, "no-cache")
                    call.response.headers.append(HttpHeaders.Connection, "keep-alive")

                    call.respondBytesWriter {

                        writeStringUtf8(": ping\n\n")
                        flush()

                        val streamingAgent = get<Agent> {
                            parametersOf(
                                suspend { event: StreamEvent ->
                                    val json = Json.encodeToString(StreamEvent.serializer(), event)
                                    writeStringUtf8("data: $json\n\n")
                                    flush()
                                }
                            )
                        }

                        val response = streamingAgent.chat(
                            "message : ${prompt.message}, \n customerId : ${customerId}, \n connectionId : ${prompt.connectionId}"
                        )

                        val analysisJson = Json.encodeToString(response.analysis)
                        writeStringUtf8("data: $analysisJson\n\n")
                        flush()

                        chatService.saveLLMessage(response, prompt.message)
                    }

                } catch (e: Exception) {
                    logger.error("SSE Streaming Error", e)

                    customerId?.let { id ->
                        try {
                            chatService.saveSystemMessage("Error during SSE stream: ${e.message}", id)
                        } catch (dbEx: Exception) {
                            logger.error("Failed to save SSE error message to DB", dbEx)
                        }
                    }

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