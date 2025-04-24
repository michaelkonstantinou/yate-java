package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.interfaces.YateUnitGeneratorInterface
import com.mkonst.services.PromptService
import java.util.HashMap

class YateUnitGenerator: YateUnitGeneratorInterface {
    override fun generate_for_class(cutContainer: ClassContainer) {
        val systemPrompt: String = PromptService.get("system")
    }

    override fun generate_for_constructors(cutContainer: ClassContainer) {
        TODO("Not yet implemented")
    }

    override fun generate_for_method(cutContainer: ClassContainer, methodName: String) {
        TODO("Not yet implemented")
    }

    fun get_prompts_for_generation(cutContainer: ClassContainer, testLevel: String) {
        val prompts = mutableListOf<String>()
        var promptIdentifyTests: String = "";
        var promptGenerateTests: String = "";

        when (testLevel) {
            "class" ->  {
                promptIdentifyTests = PromptService.get("identify_tests") + "\n\n";
                promptGenerateTests = PromptService.get("generate_tests")
            }
            "constructor" ->  {
                promptIdentifyTests = PromptService.get("identify_tests_constructors") + "\n\n";
                promptGenerateTests = PromptService.get("generate_tests_named_class", hashMapOf("CLASS_CONTENT" to cutContainer.getContent()))
            }
            "method" -> {
                promptIdentifyTests = PromptService.get("identify_tests_constructors") + "\n\n";
                promptGenerateTests = ""
            }

        }
    }
}