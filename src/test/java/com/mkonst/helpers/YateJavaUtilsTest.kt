package com.mkonst.helpers

import com.mkonst.types.MethodPosition
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

    @Test
    fun canExtractMethodPositions() {
        val classUnderTest = "/Users/michael.konstantinou/Projects/yate-java/src/test/dummyrepo/src/main/java/dummy/MyDummyClass.java"
        val classContent = YateIO.readFile(classUnderTest)

        val expectedResult: MutableList<MethodPosition> = mutableListOf(
            MethodPosition(name="foo", startLine=4, endLine=6),
            MethodPosition(name="bar", startLine=8, endLine=16)
        )

        assertEquals(expectedResult, YateJavaUtils.extractMethodPositions(classContent))
    }
}