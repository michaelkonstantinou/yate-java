package com.mkonst.evaluation.ablation

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateConsole
import com.mkonst.helpers.YateIO
import com.mkonst.providers.ClassContainerProvider
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.ProgramLangType
import com.mkonst.types.YateResponse
import kotlinx.coroutines.runBlocking
import java.io.File

class SimulatedUnitTestGenerator(modelName: String? = null, private val inputDirectory: String, private val lang: ProgramLangType = ProgramLangType.JAVA): YateUnitGenerator(modelName, lang) {

    init {
        YateConsole.info("SimulatedUnitTestGenerator initialized with model: $modelName")
    }

    override fun generateForConstructors(cutContainer: ClassContainer): YateResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val testClassName = cutContainer.className + "ConstructorsTest"
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptVars = hashMapOf(
            "CLASS_CONTENT" to cutContainer.getCompleteContent(),
            "CLASS_NAME" to testClassName)
        val promptGenerateTests = PromptService.get("ablation_generate_simple_constructors_named", promptVars)
        generationPrompts.add(promptGenerateTests)

        return simulateGeneration(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    override fun generateForMethod(cutContainer: ClassContainer, methodName: String): YateResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val testClassName = cutContainer.className + "_${methodName}_Test"
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptVars = hashMapOf(
            "METHOD_NAME" to methodName,
            "CLASS_CONTENT" to cutContainer.getCompleteContent(),
            "CLASS_NAME" to testClassName)
        val promptGenerateTests = PromptService.get("ablation_generate_simple_method_named", promptVars)
        generationPrompts.add(promptGenerateTests)

        return simulateGeneration(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    /**
     * The function will simulate the generation response. Instead of generating a brand new Test Class though, it will
     * load one that has already been generated in the past
     */
    private fun simulateGeneration(systemPrompt: String,
                                 generationPrompts: MutableList<String>,
                                 cutContainer: ClassContainer,
                                 newTestClassName: String): YateResponse {
        YateConsole.debug("Simulating generation response")

        val conversation: MutableList<ChatMessage> = mutableListOf()
        conversation.add(ChatMessage(ChatRole.System, systemPrompt))

        var answer: String? = null
        for (prompt in generationPrompts) {
            conversation.add(ChatMessage(ChatRole.User, prompt))
        }

        // Reads the content of a class that has already been generated
        answer = fetchGeneratedTestClass(cutContainer, newTestClassName)
        conversation.add(ChatMessage(ChatRole.Assistant, answer))

        val response: CodeResponse = CodeResponse(answer, conversation)

        // Prepare a new ClassContainer for the generated test class
        val testContainer = ClassContainerProvider.getFromContent(newTestClassName, response.codeContent, lang)
        testContainer.body.packageName = cutContainer.body.packageName
        testContainer.appendImports(cutContainer.body.imports)

        // Find that paths of the class under test and the generated test class
        testContainer.setPathsFromCut(cutContainer)

        return YateResponse(testContainer, response.conversation)
    }

    /**
     * It will look for the Test Class, that YATE should have generated and stored in its generation process
     * It will read and return its content
     */
    private fun fetchGeneratedTestClass(cutContainer: ClassContainer, newTestClassName: String): String {
        val testClassFilename = "${newTestClassName}${cutContainer.lang.extension}"
        val directoriesAfterRepository: String = cutContainer.paths.cut!!.substringAfter("src/main").substringBefore(cutContainer.className)
        val dirWithGeneratedContent = this.inputDirectory + directoriesAfterRepository

        val targetFile = File(dirWithGeneratedContent, testClassFilename)

        println("Fetching class from ${targetFile.absolutePath}")
        return YateIO.readFile(targetFile.absolutePath)
    }
}