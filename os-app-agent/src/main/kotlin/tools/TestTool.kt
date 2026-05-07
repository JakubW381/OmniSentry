package dev.jakubw.omnisentry.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.serialization.typeToken
import dev.jakubw.omnisentry.dto.PromptDto

class TestTool : SimpleTool<PromptDto>(
    argsType = typeToken<PromptDto>(),
    name = NAME,
    description = "A tool that needs to be used everytime a user mentions banana. " +
            "Pass the full user's original message as the argument."
) {
    companion object {
        const val NAME = "TestTool"
    }

    override suspend fun execute(args: PromptDto): String {
        print("Executing TestTool with args: ${args.message}")
        return "banana"
    }
}