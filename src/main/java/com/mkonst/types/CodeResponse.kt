package com.mkonst.types

import com.aallam.openai.api.chat.ChatMessage
import com.mkonst.config.ConfigYate
import java.util.regex.Pattern

class CodeResponse(content: String?, conversation: MutableList<ChatMessage>, isCompiling: Boolean = false) {
    private var codeContent: String? = null
    private var conversation: MutableList<ChatMessage>
    private var isCompiling: Boolean

    init {
        this.codeContent = if (content !== null) extractCodeFromResponse(content) else null
        this.conversation = conversation
        this.isCompiling = if (this.codeContent === null) false else isCompiling
    }

    /**
     * Decodes the response and looks for the first code snippet (if exists) and returns it
     * The method is useful for extracting the code out of a response that may contain verbal context
     */
    private fun extractCodeFromResponse(content: String): String? {
        val lang: String = ConfigYate.getString("LANG").lowercase()
        return try {
            if ("```$lang" in content) {
                val pattern = Pattern.compile("```$lang(.*?)```", Pattern.DOTALL)
                val matcher = pattern.matcher(content)
                if (matcher.find()) {
                    matcher.group(1).trim()
                } else {
                    content.replace("```$lang", "").replace("```", "")
                }
            } else {
                content
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun toString(): String {
        return "(CodeResponse) [Content: ${this.codeContent}, Conversation: ${this.conversation}, IsCompiling: ${this.isCompiling}]"
    }
}