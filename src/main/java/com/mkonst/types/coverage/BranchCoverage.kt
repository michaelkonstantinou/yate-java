package com.mkonst.types.coverage

data class BranchCoverage(
    val lineNumber: Int,
    val missedBranches: Int,
    val coveredBranches: Int
) {
    fun getTotalBranches(): Int {
        return missedBranches + coveredBranches
    }
    override fun toString(): String {
        if (missedBranches == coveredBranches) {
            return "In line $lineNumber: None of the ${getTotalBranches()} branches are covered"
        }

        if (missedBranches == 1) {
            return "In line $lineNumber: $missedBranches of the ${getTotalBranches()} branches is not covered"
        }

        return "In line $lineNumber: $missedBranches of ${getTotalBranches()} branches are not covered"
    }
}


