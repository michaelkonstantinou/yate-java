package com.mkonst.components

import com.mkonst.analysis.ClassContainer
import com.mkonst.analysis.JavaClassContainer
import com.mkonst.interfaces.YateUnitGeneratorInterface
import com.mkonst.models.ChatOpenAIModel
import com.mkonst.services.PromptService
import com.mkonst.types.CodeResponse
import com.mkonst.types.YateResponse

class YateUnitGenerator : YateUnitGeneratorInterface {
    private var model: ChatOpenAIModel = ChatOpenAIModel();

    /**
     * Generates the Unit Test cases at a class-level. It requires a ClassContainer instance of the class under test
     */
    override fun generateForClass(cutContainer: ClassContainer): YateResponse {
        val systemPrompt: String = PromptService.get("system")
        val generationPrompts: MutableList<String> = getPromptsForGeneration(cutContainer, "class")
        val response: CodeResponse = model.ask(generationPrompts, systemPrompt)

        // Prepare a new ClassContainer for the generated test class
        val testContainer = JavaClassContainer(cutContainer.className + "Test", response.codeContent)
        testContainer.body.packageName = cutContainer.body.packageName
        testContainer.appendImports(cutContainer.body.imports)

        // Find that paths of the class under test and the generated test class
        testContainer.setPathsFromCut(cutContainer)

        return YateResponse(testContainer, response.conversation)
    }

    override fun generateForConstructors(cutContainer: ClassContainer): YateResponse {
        TODO("Not yet implemented")
    }

    override fun generateForMethod(cutContainer: ClassContainer, methodName: String): YateResponse {
        TODO("Not yet implemented")
    }

    /**
     * The function will check the testLevel and construct a list of the prompts that will be given to the LLM to
     * generate the test cases.
     */
    fun getPromptsForGeneration(cutContainer: ClassContainer, testLevel: String, mut: String? = null): MutableList<String> {
        val prompts = mutableListOf<String>()
        var promptIdentifyTests: String = "";
        var promptGenerateTests: String = "";

        when (testLevel) {
            "class" ->  {
                promptIdentifyTests = PromptService.get("identify_tests") + "\n\n" + cutContainer.getCompleteContent()
                promptGenerateTests = PromptService.get("generate_tests")
                prompts.add(promptIdentifyTests)

                // Append a prompt that specifies non-public method (if any of them exist)
                val promptNonPublicMethods = getPromptForNonPublicMethod(cutContainer)
                if (promptNonPublicMethods !== null) {
                    prompts.add(promptNonPublicMethods)
                }
            }
            "constructor" ->  {
                val testClassName: String = cutContainer.className + "ConstructorsTest"
                promptIdentifyTests = PromptService.get("identify_tests_constructors") + "\n\n" + cutContainer.getCompleteContent()
                prompts.add(promptIdentifyTests)
                promptGenerateTests = PromptService.get("generate_tests_named_class", hashMapOf("CLASS_NAME" to testClassName))
            }
            "method" -> {
                if (mut === null) {
                    throw Exception("Method level testing requires a valid method name to test. Null given")
                }

                val testClassName: String = cutContainer.className + "_" + mut + "_Test"
                promptIdentifyTests = PromptService.get("identify_tests_method", hashMapOf("METHOD_NAME" to mut)) + "\n\n" + cutContainer.getCompleteContent()
                prompts.add(promptIdentifyTests)
                promptGenerateTests = PromptService.get("generate_tests_named_class", hashMapOf("CLASS_NAME" to testClassName))
            }
        }

        prompts.add(promptGenerateTests)

        return prompts
    }

    private fun getPromptForNonPublicMethod(cutContainer: ClassContainer): String? {
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

    override fun closeConnection() {
        this.model.closeConnection()
    }
}