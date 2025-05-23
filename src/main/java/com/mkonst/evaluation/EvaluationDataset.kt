package com.mkonst.evaluation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.mkonst.types.TestLevel
import java.io.File

class EvaluationDataset(val file: String? = null) {
    val records: MutableList<EvaluationDatasetRecord> = mutableListOf()

    init {
        if (file !== null) {
            csvReader().readAllWithHeader(File(file)).forEach { row ->
                val record = EvaluationDatasetRecord(
                    repositoryPath = row["repositoryPath"]!!,
                    classPath = row["classPath"]!!,
                    testLevel = TestLevel.valueOf(row["testLevel"]!!.uppercase()),
                    requests = row["requests"]?.toIntOrNull() ?: 0,
                    generationTime = row["generationTime"]?.toLongOrNull() ?: 0,
                    isExecuted = row["isExecuted"]?.toBoolean() ?: false,
                    errors = row["errors"],
                    outputDir = row["outputDir"],
                    generatedTests = row["generatedTests"]?.toIntOrNull() ?: 0,
                )
                records.add(record)
            }
        }
    }

    fun saveAs(filename: String) {
        val header = listOf("repositoryPath", "classPath", "testLevel", "requests", "generationTime", "isExecuted", "errors", "outputDir", "generatedTests")

        csvWriter().open(File(filename)) {
            writeRow(header)
            records.forEach { record ->
                writeRow(
                    record.repositoryPath,
                    record.classPath,
                    record.testLevel.name,
                    record.requests,
                    record.generationTime,
                    record.isExecuted,
                    record.errors,
                    record.outputDir,
                    record.generatedTests
                )
            }
        }
    }

    fun printTotals() {
        var totalRequests = 0
        var totalGenerationTime = 0L
        var totalGeneratedTests = 0

        records.forEach { record ->
            totalRequests += record.requests
            totalGeneratedTests += record.generatedTests
            totalGenerationTime += record.generationTime
        }

        val avgRequests = totalRequests / records.size
        val avgGenerationTime = totalGenerationTime.toFloat() / records.size

        println("Total Requests: $totalRequests")
        println("Total Generated Tests: $totalGeneratedTests")
        println("Total Generation Time: $totalGenerationTime")
        println("Average Nr. Requests: $avgRequests")
        println("Average Generation Time: $avgGenerationTime")
    }
}