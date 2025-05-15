package com.mkonst.types.serializable

import kotlinx.serialization.Serializable

@Serializable
data class ClassContainerJson(val name: String,
                              val packageName: String,
                              val imports: MutableList<String>,
                              val methods: MutableMap<String, String>,
                              val content: String,
                              val hasConstructors: Boolean,
                              val pathCut: String?,
                              val pathTestClass: String?)
