package com.mkonst.evaluation.ablation

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.components.AbstractModelComponent
import com.mkonst.components.YateUnitGenerator
import com.mkonst.helpers.YateConsole
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse

class RawUnitGenerator(modelName: String? = null): AbstractModelComponent(modelName) {

    init {
        YateConsole.info("RawUnitGenerator initialized with model: $modelName")
    }

    fun generateForClass(cutContainer: ClassContainer): CodeResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptGenerateTests = PromptService.get("ablation_generate_simple", hashMapOf("CLASS_CONTENT" to cutContainer.getCompleteContent()))
        generationPrompts.add(promptGenerateTests)

        val testClassName = cutContainer.className + "Test"

        return generateUnitTests(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    fun generateForConstructors(cutContainer: ClassContainer): CodeResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val testClassName = cutContainer.className + "ConstructorsTest"
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptVars = hashMapOf(
            "CLASS_CONTENT" to cutContainer.getCompleteContent(),
            "CLASS_NAME" to testClassName)
        val promptGenerateTests = PromptService.get("ablation_generate_simple_constructors_named", promptVars)
        generationPrompts.add(promptGenerateTests)

        return generateUnitTests(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    fun generateForMethod(cutContainer: ClassContainer, methodName: String): CodeResponse {
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

        return generateUnitTests(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    /**
     * Uses the model to generate a new Test class based on the given prompts
     * Afterward, the method will instantiate a new ClassContainer instance that contains the generated Test Class
     *
     * Returns a YateResponse instance, that contains the generated Class Container and the interaction with the model
     */
    fun generateUnitTests(systemPrompt: String,
                          generationPrompts: MutableList<String>,
                          cutContainer: ClassContainer,
                          newTestClassName: String): CodeResponse {
        val response: CodeResponse = model.ask(generationPrompts, systemPrompt)

        // Prepare a new ClassContainer for the generated test class
        val testContainer = JavaClassContainer(newTestClassName, response.codeContent)
        testContainer.body.packageName = cutContainer.body.packageName

        // Find that paths of the class under test and the generated test class
        testContainer.setPathsFromCut(cutContainer)

        return response
    }
}