package com.mkonst.evaluation

import com.mkonst.types.TestLevel

data class EvaluationDatasetRecord(
    val repositoryPath: String,
    val classPath: String,
    val testLevel: TestLevel,
    var requests: Int,
    var generationTime: Long,
    var isExecuted: Boolean,
    var errors: String?,
    var outputDir: String?,
    var generatedTests: Int
)