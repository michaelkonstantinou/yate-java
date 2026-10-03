package com.mkonst.evaluation.ablation

data class RepairAblationOptions(
    val fixImportStatements: Boolean = true,
    val fixOtherCompilationErrors: Boolean = true,
    val fixOracleErrorsWithYate: Boolean = true,
)
