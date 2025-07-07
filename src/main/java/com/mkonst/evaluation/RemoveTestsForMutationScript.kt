package com.mkonst.evaluation

import com.mkonst.helpers.YateCodeUtils
import com.mkonst.helpers.YateIO
import com.mkonst.services.PiTestService
import com.mkonst.types.DependencyTool
import com.mkonst.types.ProgramLangType
import com.mkonst.types.coverage.MutationScore

object RemoveTestsForMutationScript {

    @JvmStatic
    fun main(args: Array<String>) {
        val repositoryPath = "/Users/michael.konstantinou/Datasets/yate_evaluation/ConfigMe/"
        val piTestService: PiTestService = PiTestService(repositoryPath)
        val ms: MutationScore = piTestService.runMutationScore(DependencyTool.MAVEN)

        println(ms)
    }
}