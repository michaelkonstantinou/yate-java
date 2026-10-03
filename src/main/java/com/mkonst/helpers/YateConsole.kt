package com.mkonst.helpers

object YateConsole {

    fun debug(output: String) {
        println("[YATE][${YateUtils.timestamp()}] (Debug) - $output")
    }

    fun info(output: String) {
        println("[YATE][${YateUtils.timestamp()}] (Info) - $output")
    }

    fun warning(output: String) {
        println("[YATE][${YateUtils.timestamp()}] (Warning) - $output")
    }

    fun error(output: String) {
        println("[YATE][${YateUtils.timestamp()}] (Error) - $output")
    }
}