package dev.jakubw.omnisentry

import dev.jakubw.omnisentry.agent.OllamaAgent
import dev.jakubw.omnisentry.dto.PromptDto
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json


fun main(args: Array<String>) {

        if (args.isEmpty()) {
            println("Running basic server...")
            println("Provide the 'configured' argument to run a configured server.")
        }
        val mode = args.getOrNull(0) ?: "basic"

        when (mode) {
            "basic" -> runBasicServer()
            "configured" -> runConfiguredServer()
            else -> runServerWithCommandLineConfig(args)
        }
    }

    fun runBasicServer() {
        val ollama: OllamaAgent = OllamaAgent()
        embeddedServer(Netty, port = 8085) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
                post("/message") {
                    val prompt = call.receive<PromptDto>()
                    call.respondText(ollama.chat(prompt.message))
                }
            }
        }.start(wait = true)
    }

    fun runConfiguredServer() {
        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = "0.0.0.0"
                port = 8084
            })
            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        }) {
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
            }
        }.start(wait = true)
    }

    fun runServerWithCommandLineConfig(args: Array<String>) {
        embeddedServer(
            factory = Netty,
            configure = {
                val cliConfig = CommandLineConfig(args)
                takeFrom(cliConfig.engineConfig)
                loadCommonConfiguration(cliConfig.rootConfig.environment.config)
            }
        ) {
            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }
            }
        }.start(wait = true)
    }

