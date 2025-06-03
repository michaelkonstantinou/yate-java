package com.mkonst.evaluation

data class RequestsCounter(
    val generation: Int = 0,
    val compilationFixing: Int = 0,
    val oracleFixing: Int = 0,
    val coverageEnhancement: Int = 0,
    val totalFixing: Int = compilationFixing + oracleFixing,
    val total: Int = generation + compilationFixing + oracleFixing + coverageEnhancement
)
