package com.mkonst.components

import com.mkonst.analysis.ClassContainer

class YateCoverageEnhancer: AbstractModelComponent() {

    fun generateTestsForLineCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer) {}

    fun generateTestsForBranchCoverage(cutContainer: ClassContainer, testClassContainer: ClassContainer) {}
}