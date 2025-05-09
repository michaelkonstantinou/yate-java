package com.mkonst.types

data class TestErrorLog (
        val testMethodName: String,
        val content: String,
        val lineNumber: Int,
        val className: String? = null,
        val message: String? = null,
        val type: String? = null,
)