package com.mkonst.types.ollama

import com.mkonst.config.ConfigYate
import com.mkonst.models.ChatOllamaModel

fun main() {
    ConfigYate.initialize()
    val model = ChatOllamaModel("llama3.2")

    val promptTest: String = "Can you generate tests for the following class? \n\npackage software.amazon.event.ruler;\n" +
            "\n" +
            "import java.util.Set;\n" +
            "\n" +
            "/**\n" +
            " * Represents a suggestion of a state/token combo from which there might be a transition, in an array-consistent fashion.\n" +
            " */\n" +
            "class ACStep {\n" +
            "    final int fieldIndex;\n" +
            "    final NameState nameState;\n" +
            "    final Set<SubRuleContext> candidateSubRuleIds;\n" +
            "    final ArrayMembership membershipSoFar;\n" +
            "\n" +
            "    ACStep(final int fieldIndex, final NameState nameState, final Set<SubRuleContext> candidateSubRuleIds,\n" +
            "           final ArrayMembership arrayMembership) {\n" +
            "        this.fieldIndex = fieldIndex;\n" +
            "        this.nameState = nameState;\n" +
            "        this.candidateSubRuleIds = candidateSubRuleIds;\n" +
            "        this.membershipSoFar = arrayMembership;\n" +
            "    }\n" +
            "}\n"
    println(model.ask(mutableListOf(promptTest), "You are a tool used to generate Junit5 tests.").codeContent)
}