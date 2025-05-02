package com.mkonst.types

data class ClassBody(
        var packageName: String? = null,
        val imports: MutableList<String> = mutableListOf(),
        val methods: MutableMap<String, String> = mutableMapOf(),
        val content: String = "",
        val hasConstructors: Boolean = false)