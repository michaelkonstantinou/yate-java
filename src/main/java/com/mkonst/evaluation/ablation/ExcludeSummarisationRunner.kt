package com.mkonst.evaluation.ablation

import com.mkonst.analysis.ClassContainer
import com.mkonst.components.YateUnitGenerator
import com.mkonst.services.PromptService
import com.mkonst.types.YateResponse

class ExcludeSummarisationRunner(modelName: String? = null): YateUnitGenerator(modelName) {
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
        throw Exception("This component is used only for evaluation")
    }

    override fun generateForMethod(cutContainer: ClassContainer, methodName: String): YateResponse {
        throw Exception("This component is used only for evaluation")
    }
}