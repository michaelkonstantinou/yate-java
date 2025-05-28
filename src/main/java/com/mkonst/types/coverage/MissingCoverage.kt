package com.mkonst.types.coverage

data class MissingCoverage(
    val key: String,
    var missedLines: MutableList<String> = mutableListOf(),
    val missedBranches: MutableList<BranchCoverage> = mutableListOf()
) {
    fun isFullyLineCovered(): Boolean {
        return missedLines.isEmpty()
    }

    fun isFullyBranchCovered(): Boolean {
        return missedBranches.isEmpty()
    }

    fun mergeMissedLines() {
        if (missedLines.size < 2) {
            return
        }

        val newMissedLineList = mutableListOf<String>()
        var startingIndex = 0
        for (idx: Int in 0..<missedLines.size) {
            if (idx == missedLines.size - 1 || missedLines.get(idx).toInt() != missedLines.get(idx + 1).toInt() - 1) {
                if (startingIndex != idx) {
                    newMissedLineList.add(missedLines.get(startingIndex) + "-" + missedLines.get(idx))
                } else {
                    newMissedLineList.add(missedLines.get(idx))
                }

                startingIndex = idx + 1
            }
        }

        missedLines = newMissedLineList
    }

    // ResourcePluginProxy 38-43, 45, 47-48
}