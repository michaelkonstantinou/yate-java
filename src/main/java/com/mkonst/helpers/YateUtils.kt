package com.mkonst.helpers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object YateUtils {

    fun timestamp(): String {
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))

        return formatted
    }
}