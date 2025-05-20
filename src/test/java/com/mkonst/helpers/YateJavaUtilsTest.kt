package com.mkonst.helpers

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class YateJavaUtilsTest {

    @Test
    fun classPackageExists() {
        val repositoryPath: String = "/Users/michael.konstantinou/Projects/yate-java/src/test/dummyrepo/"
        val qualifiedName: String = "dummy.MyDummyClass"

        assertTrue(YateJavaUtils.classPackageExists(repositoryPath, qualifiedName))
    }

    @Test
    fun classPackageExistsIsCaseSensitive() {
        val repositoryPath: String = "/Users/michael.konstantinou/Projects/yate-java/src/test/dummyrepo/"
        val qualifiedName: String = "dummy.mydummyclass"

        assertFalse(YateJavaUtils.classPackageExists(repositoryPath, qualifiedName))
    }
}