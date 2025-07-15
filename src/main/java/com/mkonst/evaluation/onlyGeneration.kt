package com.mkonst.evaluation

import com.mkonst.config.ConfigYate
import com.mkonst.runners.YateJavaRunner
import com.mkonst.runners.YateRawRunner
import com.mkonst.services.PromptService

object onlyGeneration {

    @JvmStatic
    fun main(args: Array<String>) {
        ConfigYate.initialize()
        PromptService.initialize()

        val csvFile = "/Users/michael.konstantinou/Projects/yate/output/input_windward_class_mistral_7b.csv"

        val dataset = EvaluationDataset(csvFile)

        val runner = YateRawRunner(dataset.records[0].repositoryPath, "gpt-4o-mini")
        for (record in dataset.records) {
            println("Iterating file: ${record.classPath}")
            runner.generate(record.classPath)
        }
    }
}