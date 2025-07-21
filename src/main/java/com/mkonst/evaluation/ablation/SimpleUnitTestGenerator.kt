package com.mkonst.evaluation.ablation

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.helpers.YateConsole
import com.mkonst.services.PromptService
import com.mkonst.types.ProgramLangType
import com.mkonst.types.YateResponse

class SimpleUnitTestGenerator(modelName: String? = null, lang: ProgramLangType = ProgramLangType.JAVA): YateUnitGenerator(modelName, lang) {

    init {
        YateConsole.info("SimpleUnitTestGenerator initialized with model: $modelName")
    }

    override fun generateForClass(cutContainer: ClassContainer): YateResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptGenerateTests = PromptService.get("ablation_generate_simple", hashMapOf("CLASS_CONTENT" to cutContainer.getCompleteContent()))
        generationPrompts.add(promptGenerateTests)

        val testClassName = cutContainer.className + "Test"

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
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

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
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

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
    }
}