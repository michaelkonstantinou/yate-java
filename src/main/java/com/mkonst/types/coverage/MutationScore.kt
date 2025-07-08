package com.mkonst.types.coverage

import com.mkonst.helpers.YateUtils

data class MutationScore(val generatedMutants: Int, val killedMutants: Int) {

    fun getScore(): Float {
        return killedMutants.toFloat() / generatedMutants.toFloat()
    }

    fun getScorePercentage(): Float {
        return (killedMutants.toFloat() / generatedMutants.toFloat()) * 100
    }

    override fun toString(): String {
        if (generatedMutants <= 0) {
            return "(Problematic score: Has 0 generated mutants)"
        }

        val scorePercentage = YateUtils.formatDecimal(getScorePercentage())
        return "$scorePercentage% ($killedMutants/$generatedMutants)"
    }
}
