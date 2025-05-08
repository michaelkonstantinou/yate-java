package com.mkonst.types

data class OracleError(
        val testMethodName: String,
        val className: String? = null,
        val actualValue: String? = null,
        val expectedValue: String? = null,
        val exceptionType: String? = null,
        val lineNumber: Int,
)
