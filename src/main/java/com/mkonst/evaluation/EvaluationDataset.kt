package com.mkonst.evaluation

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.mkonst.helpers.YateUtils
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
                    requests = RequestsCounter(row["generationRequests"]?.toIntOrNull() ?: 0,
                                        row["compilationFixRequests"]?.toIntOrNull() ?: 0,
                                            row["oracleFixRequests"]?.toIntOrNull() ?: 0,
                                    row["coverageEnhancementRequests"]?.toIntOrNull() ?: 0),
                    generationTime = row["generationTime"]?.toLongOrNull() ?: 0,
                    isExecuted = row["isExecuted"]?.toBoolean() ?: false,
                    errors = row["errors"],
                    outputDir = row["outputDir"],
                    modelName = row["modelName"],
                    generatedTests = row["generatedTests"]?.toIntOrNull() ?: 0,
                )
                records.add(record)
            }
        }
    }

    fun saveAs(filename: String) {
        val header = listOf("repositoryPath", "classPath", "testLevel", "generationRequests", "compilationFixRequests", "oracleFixRequests", "coverageEnhancementRequests", "fixRequests", "totalRequests", "generationTime", "isExecuted", "errors", "outputDir", "modelName", "generatedTests")

        csvWriter().open(File(filename)) {
            writeRow(header)
            records.forEach { record ->
                writeRow(
                    record.repositoryPath,
                    record.classPath,
                    record.testLevel.name,
                    record.requests.generation,
                    record.requests.compilationFixing,
                    record.requests.oracleFixing,
                    record.requests.coverageEnhancement,
                    record.requests.totalFixing,
                    record.requests.total,
                    record.generationTime,
                    record.isExecuted,
                    record.errors,
                    record.outputDir,
                    record.modelName,
                    record.generatedTests
                )
            }
        }
    }

    fun printTotals() {
        println(getTotalsText())
    }

    fun getTotals(): MutableMap<String, Any> {
        var totalRequests = 0
        var totalGenerationRequests = 0
        var totalCompilingFixingRequests = 0
        var totalOracleFixingRequests = 0
        var totalCoverageEnhanceRequests = 0
        var totalGenerationTime = 0L
        var totalGeneratedTests = 0

        records.forEach { record ->
            totalRequests += record.requests.total
            totalGenerationRequests += record.requests.generation
            totalCompilingFixingRequests += record.requests.compilationFixing
            totalOracleFixingRequests += record.requests.oracleFixing
            totalCoverageEnhanceRequests += record.requests.coverageEnhancement
            totalGeneratedTests += record.generatedTests
            totalGenerationTime += record.generationTime
        }

        val avgRequests = totalRequests.toFloat() / records.size
        val avgGenerationTime = totalGenerationTime.toFloat() / records.size

        return mutableMapOf(
            "totalRequests" to totalRequests,
            "totalGenerationRequests" to totalGenerationRequests,
            "totalCompilingFixingRequests" to totalCompilingFixingRequests,
            "totalOracleFixingRequests" to totalOracleFixingRequests,
            "totalCoverageEnhanceRequests" to totalCoverageEnhanceRequests,
            "totalGeneratedTests" to totalGeneratedTests,
            "totalGenerationTime" to totalGenerationTime,
            "avgRequests" to avgRequests,
            "avgGenerationTime" to avgGenerationTime,
        )
    }

    /**
     * Calculates a summary of all its values and returns a structured multiline text with the results
     */
    fun getTotalsText(): String {
        val totals = this.getTotals()

        val output: StringBuilder = StringBuilder()
        output.appendLine("Total Requests: ${totals["totalRequests"]}")
        output.appendLine("Total Generation Requests: ${totals["totalGenerationRequests"]}")
        output.appendLine("Total Compiling fixing Requests: ${totals["totalCompilingFixingRequests"]}")
        output.appendLine("Total Oracle fixing Requests: ${totals["totalOracleFixingRequests"]}")
        output.appendLine("Total Coverage enhancement Requests: ${totals["totalCoverageEnhanceRequests"]}")
        output.appendLine("Total Generated Tests: ${totals["totalGeneratedTests"]}")
        output.appendLine("Total Generation Time: ${totals["totalGenerationTime"]}")
        output.appendLine("Total Generation Time (human readable): ${YateUtils.formatMillisToMinSec(totals["totalGenerationTime"] as Long)}")
        output.appendLine("Average Nr. Requests: ${YateUtils.formatDecimal(totals["avgRequests"] as Float)}")
        output.appendLine("Average Generation Time: ${YateUtils.formatDecimal(totals["avgGenerationTime"] as Float)}")

        return output.toString()
    }
}