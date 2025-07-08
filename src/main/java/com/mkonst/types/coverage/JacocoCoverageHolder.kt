package com.mkonst.types.coverage

import kotlinx.serialization.Serializable

@Serializable
data class JacocoCoverageHolder(
    val lineCoverage: BinaryCoverage,
    val branchCoverage: BinaryCoverage,
    val methodCoverage: BinaryCoverage,
    val classCoverage: BinaryCoverage
) {
    override fun toString(): String {
        var output = "Jacoco coverages:\n"
        output += "Line Coverage: ${lineCoverage.getScoreText()}\n"
        output += "Branch Coverage: ${branchCoverage.getScoreText()}\n"
        output += "Method Coverage: ${methodCoverage.getScoreText()}\n"
        output += "Class Coverage: ${classCoverage.getScoreText()}\n"

        return output
    }
}
