package com.mkonst.types.coverage

data class MethodCoverage(
    val name: String,
    val lineCoverage: Float,
    val missedBranches: Int,
    val missedCoverage: MissingCoverage = MissingCoverage(name)
)
