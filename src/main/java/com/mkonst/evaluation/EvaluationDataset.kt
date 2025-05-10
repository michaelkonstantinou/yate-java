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
                    classLevel = TestLevel.valueOf(row["classLevel"]!!.uppercase()),
                    requests = row["requests"]?.toInt() ?: 0,
                    generationTime = row["generationTime"]?.toLong() ?: 0,
                    isExecuted = row["isExecuted"]?.toBoolean() ?: false,
                    errors = row["errors"],
                    outputDir = row["outputDir"]
                )
                records.add(record)
            }
        }
    }

    fun saveAs(filename: String) {
        val header = listOf("repositoryPath", "classPath", "classLevel", "requests", "generationTime", "isExecuted", "errors", "outputDir")

        csvWriter().open(File(filename)) {
            writeRow(header)
            records.forEach { record ->
                writeRow(
                    record.repositoryPath,
                    record.classPath,
                    record.classLevel.name,
                    record.requests,
                    record.generationTime,
                    record.isExecuted,
                    record.errors,
                    record.outputDir
                )
            }
        }
    }
}