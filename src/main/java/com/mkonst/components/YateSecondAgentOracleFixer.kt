package com.mkonst.components

import com.mkonst.models.ChatOpenAIModel

class YateSecondAgentOracleFixer(repositoryPath: String,
                                 dependencyTool: String,
                                 expectedTypesToIgnore: MutableList<String> = mutableListOf("java.lang.RuntimeException", "RuntimeException")
): YateOracleFixer(repositoryPath, dependencyTool, expectedTypesToIgnore) {
    private var modelSecondFixer: ChatOpenAIModel = ChatOpenAIModel()

    fun fixErrorsUsingSecondAgent() {

    }
}