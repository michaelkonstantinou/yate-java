package com.mkonst.types

data class MethodCoverage(
    val name: String,
    val lineCoverage: Float,
    val missedBranches: Int,
    val uncoveredLines: List<String> = mutableListOf(),
    val uncoveredBranches: List<String> = mutableListOf()
)
