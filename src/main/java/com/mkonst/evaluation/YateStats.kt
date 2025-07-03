package com.mkonst.evaluation

import com.mkonst.helpers.YateIO
import com.mkonst.helpers.YateUtils

object YateStats {
    private val timeStarts: MutableMap<String, Long> = mutableMapOf()
    private val timeStats: MutableMap<String, Long> = mutableMapOf()
    private val counts: MutableMap<String, Int> = mutableMapOf()

    /**
     * Saves the current time stamp in milliseconds for the provided key
     */
    fun startTime(key: String) {
        this.timeStarts[key] = System.currentTimeMillis()
    }

    /**
     * Calculates the time in milliseconds between the current stored starting time
     * On the end, removes the starting time for the given key and updates the timeStats map for future reference
     */
    fun endTime(key: String, appendOnExisting: Boolean = false): Long {
        val startTime = this.timeStarts[key]
        if (startTime === null) {
            throw Exception("Start time not found for given key: $key")
        }

        // Update end time and return
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Update maps
        if (appendOnExisting && this.timeStats.containsKey(key)) {
            this.timeStats[key] = this.timeStats[key]!! + totalTime
        } else {
            this.timeStats[key] = totalTime
        }

        this.timeStarts.remove(key)

        return totalTime
    }

    /**
     * Returns the already calculated time stored for the given key. Does not calculate anything new
     */
    fun getTimeStat(key: String): Long? {
        return this.timeStats[key]
    }

    fun getCount(key: String): Int? {
        return this.counts[key]
    }

    fun addCount(key: String, value: Int = 1) {
        if (this.counts.containsKey(key)) {
            this.counts[key] = this.counts[key]!! + value

            return
        }

        this.counts[key] = value
    }

    fun save(outputFilepath: String = "yate_stats.txt") {
        val content: StringBuilder = StringBuilder()
        for (stat in this.timeStats) {
            content.appendLine("${stat.key}: ${YateUtils.formatMillisToMinSec(stat.value)}")
        }

        for (count in this.counts) {
            content.appendLine("${count.key}: ${count.value}")
        }

        YateIO.writeFile(outputFilepath, content.toString())
    }
}