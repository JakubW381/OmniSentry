package dev.jakubw.omnisentry.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import dev.jakubw.omnisentry.tools.TestTool

class OllamaAgent : Agent {

    val localLLama = LLModel(
        id = "llama3.1:8b-instruct-q4_K_M",
        provider = LLMProvider.Ollama,
        capabilities = listOf(
            LLMCapability.Tools,
            LLMCapability.ToolChoice
        )
    )

    val llamaConfig = AIAgentConfig(
        prompt = prompt(
            id = "assistant",
            params = LLMParams(
                temperature = 0.7
            )
        ){
            system("You are very helpful professional painter and digital artist with many followers")
        },
        model = localLLama,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(
            "http://localhost:11434"
        ),
        agentConfig = llamaConfig,
        toolRegistry = ToolRegistry{
            tool(TestTool())
        }
    ){
        handleEvents {
            onToolCallStarting { toolCall ->
                println("Tool call starting: ${toolCall.toolName}")
            }
        }
    }

    override suspend fun chat(message: String): String {
        return agent.run(message)
    }

    override suspend fun getExpenseAnalysis(rawData: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getAnomalyAnalysis(logs: List<String>): String {
        TODO("Not yet implemented")
    }
}