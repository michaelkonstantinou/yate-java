package com.mkonst.config

import java.io.FileInputStream
import java.io.IOException
import java.util.*

object ConfigYate {
    private var properties: Properties? = null

    @JvmStatic
    @Throws(IOException::class)
    fun initialize(configFile: String = ".env") {
        properties = Properties()
        FileInputStream(configFile).use { `in` ->
            properties!!.load(`in`)
        }
    }

    @JvmStatic
    fun setValue(name: String, value: String) {
        properties!!.setProperty(name, value)
    }

    @JvmStatic
    fun getStringOrNull(name: String): String? {
        return if (properties!!.keys.contains(name)) properties!!.getProperty(name) else null
    }

    @JvmStatic
    fun getString(name: String): String {
        return properties!!.getProperty(name)
    }

    @JvmStatic
    fun getInteger(name: String): Int {
        return properties!!.getProperty(name).toInt()
    }

    fun getArray(name: String): Array<String> {
        val joinedItems = properties!!.getProperty(name)

        return joinedItems.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    fun getBoolean(name: String): Boolean {
        return properties!!.getProperty(name).toBoolean()
    }

    /**
     * Returns a human friendly text with a list of all variables and their values
     */
    fun dump(): String {
        val value = StringBuilder()
        value.appendLine("YATE CONFIGURATION")
        value.appendLine("------------------")

        if (properties == null) {
            value.appendLine("-> No configuration properties found.")
        } else {
            properties!!.forEach { (key, varValue) ->
                value.appendLine("-> $key = $varValue")
            }
        }

        return value.toString()
    }
}
