package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.analysis.KotlinClassContainer
import com.mkonst.interfaces.YateUnitGeneratorInterface
import com.mkonst.providers.ClassContainerProvider
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.ProgramLangType
import com.mkonst.types.YateResponse

open class YateUnitGenerator(modelName: String? = null, private val lang: ProgramLangType = ProgramLangType.JAVA) : AbstractModelComponent(modelName), YateUnitGeneratorInterface {

    /**
     * Generates the Unit Test cases at a class-level. It requires a ClassContainer instance of the class under test
     */
    override fun generateForClass(cutContainer: ClassContainer): YateResponse {
        val systemPrompt: String = PromptService.get("system")

        // Add the prompt that forces the LLM to identify the needed tests by generating a summary/report
        val generationPrompts: MutableList<String> = mutableListOf()
        val promptIdentifyTests = PromptService.get("identify_tests") + "\n\n" + cutContainer.getCompleteContent()
        generationPrompts.add(promptIdentifyTests)

        // Append a prompt that specifies non-public method (if any of them exist)
        val promptNonPublicMethods = getPromptForNonPublicMethod(cutContainer)
        if (promptNonPublicMethods !== null) {
            generationPrompts.add(promptNonPublicMethods)
        }

        // Append the last prompts that verify 100% branch coverage and ask the LLM to generate the tests
        generationPrompts.add(PromptService.get("identify_branch_coverage"))
        generationPrompts.add(PromptService.get("generate_tests"))
        val testClassName = cutContainer.className + "Test"

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    override fun generateForConstructors(cutContainer: ClassContainer): YateResponse {
        val systemPrompt: String = PromptService.get("system")
        val generationPrompts: MutableList<String> = mutableListOf()

        val testClassName: String = cutContainer.className + "ConstructorsTest"
        val promptIdentifyTests = PromptService.get("identify_tests_constructors") + "\n\n" + cutContainer.getCompleteContent()
        generationPrompts.add(promptIdentifyTests)
        generationPrompts.add(PromptService.get("generate_tests_named_class", hashMapOf("CLASS_NAME" to testClassName)))

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    override fun generateForMethod(cutContainer: ClassContainer, methodName: String): YateResponse {
        val systemPrompt: String = PromptService.get("system")
        val generationPrompts: MutableList<String> = mutableListOf()
        val testClassName: String = cutContainer.className + "_" + methodName + "_Test"

        generationPrompts.add(PromptService.get("identify_tests_method", hashMapOf("METHOD_NAME" to methodName)) + "\n\n" + cutContainer.getCompleteContent())
        generationPrompts.add(PromptService.get("generate_tests_named_class", hashMapOf("CLASS_NAME" to testClassName)))

        return generateTestUsingModel(systemPrompt, generationPrompts, cutContainer, testClassName)
    }

    protected fun getPromptForNonPublicMethod(cutContainer: ClassContainer): String? {
        var promptNonPublic: String = PromptService.get("specify_private_and_protected_methods")

        val privateMethods = cutContainer.getPrivateMethods()
        val protectedMethods = cutContainer.getProtectedMethods()

        if (privateMethods.size > 0) {
            promptNonPublic += "\n\nPrivate methods: " + privateMethods.joinToString()
        }

        if (protectedMethods.size > 0) {
            promptNonPublic += "\n\nProtected methods: " + protectedMethods.joinToString()
        }

        if (privateMethods.size + protectedMethods.size > 0) {
            return promptNonPublic
        }

        return null
    }

    /**
     * Uses the model to generate a new Test class based on the given prompts
     * Afterward, the method will instantiate a new ClassContainer instance that contains the generated Test Class
     *
     * Returns a YateResponse instance, that contains the generated Class Container and the interaction with the model
     */
    protected fun generateTestUsingModel(systemPrompt: String,
                                       generationPrompts: MutableList<String>,
                                       cutContainer: ClassContainer,
                                       newTestClassName: String): YateResponse {
        val response: CodeResponse = model.ask(generationPrompts, systemPrompt)

        // Prepare a new ClassContainer for the generated test class
        val testContainer = ClassContainerProvider.getFromContent(newTestClassName, response.codeContent, lang)
        testContainer.body.packageName = cutContainer.body.packageName
        testContainer.appendImports(cutContainer.body.imports)

        // Find that paths of the class under test and the generated test class
        testContainer.setPathsFromCut(cutContainer)

        return YateResponse(testContainer, response.conversation)
    }
}