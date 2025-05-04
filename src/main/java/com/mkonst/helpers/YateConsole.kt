package com.mkonst.helpers

object YateConsole {

    fun debug(output: String) {
        println("[YATE] (Debug) - $output")
    }

    fun info(output: String) {
        println("[YATE] (Info) - $output")
    }

    fun warning(output: String) {
        println("[YATE] (Warning) - $output")
    }

    fun error(output: String) {
        println("[YATE] (Error) - $output")
    }
}