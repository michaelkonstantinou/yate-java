package com.mkonst.types

import com.aallam.openai.api.chat.ChatMessage
import com.mkonst.analysis.ClassContainer

data class YateResponse(val testClassContainer: ClassContainer, val conversation: MutableList<ChatMessage>, var isCompiling: Boolean = false) {
}