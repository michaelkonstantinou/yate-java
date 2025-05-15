package com.mkonst.types

import com.aallam.openai.api.chat.ChatMessage
import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateIO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class YateResponse(
    var testClassContainer: ClassContainer,
    var conversation: MutableList<ChatMessage>,
    var hasChanges: Boolean = false)
{
    /**
     * Creates and assigns a new test class container to this instance, based on the new body content given
     * It makes sure that the new TestClassContainer contains the old import statements, package name, and paths
     */
    fun recreateTestClassContainer(newBodyContent: String?, hasChanges: Boolean = true) {
        if (newBodyContent === null) {
            return
        }

        val newTestClassContainer: ClassContainer = JavaClassContainer(testClassContainer.className, newBodyContent)
        newTestClassContainer.body.packageName = testClassContainer.body.packageName
        newTestClassContainer.appendImports(testClassContainer.body.imports)
        newTestClassContainer.paths = testClassContainer.paths.copy()

        testClassContainer = newTestClassContainer

        // Make sure to update the hasChanges flag to reflect the current state of this response
        this.hasChanges = hasChanges
    }

    fun save() {
        // Save conversation
        val jsonString = Json.encodeToString(conversation)
        val outputFilepath: String = ConfigYate.getString("DIR_OUTPUT") + testClassContainer.className + "_conversation.json"
        YateIO.writeFile(outputFilepath, jsonString)

        // Save Test Class Container (important) fields
        testClassContainer.toJson()
    }
}