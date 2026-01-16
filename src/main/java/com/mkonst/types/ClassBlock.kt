package com.mkonst.types

data class ClassBlock(
    val name: String,
    val startLine: Int,
    val endLine: Int,
    val body: String
)
