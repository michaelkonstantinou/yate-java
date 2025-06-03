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
}
