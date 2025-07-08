package com.mkonst.types.coverage

import com.mkonst.helpers.YateUtils
import kotlinx.serialization.Serializable

/**
 * Generic data class that contains the number of instances covered out of the complete sample
 */
@Serializable
data class BinaryCoverage(val covered: Int, val total: Int) {

    /**
     * Calculates and returns the score as a float number. (Calculated as covered/total)
     */
    fun getScore(): Float {
        return (covered.toFloat() / total.toFloat()) * 100
    }

    /**
     * Returns the score (covered / total) as a percentage. Depending on the flag includeRawValues,
     * the function may also include the division in parentheses (e.g. 10% (1/10) or just 10%)
     */
    fun getScoreText(includeRawValues: Boolean = true): String {
        val output = YateUtils.formatDecimal(getScore()) + "%"

        if (includeRawValues) {
            return "$output ($covered/$total)"
        }

        return output
    }
}