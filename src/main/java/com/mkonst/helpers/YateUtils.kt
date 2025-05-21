package com.mkonst.helpers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.mkonst.analysis.ClassContainer
import com.mkonst.types.YateResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object YateUtils {

    fun timestamp(): String {
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))

        return formatted
    }

    /**
     * The method will move the generate test file, to the specified outputDirectory.
     * If the given response object does not contain a valid test class path, the method will do nothing
     */
    fun moveGeneratedTestClass(testClassContainer: ClassContainer, outputDirectory: String) {
        val sourcePath: String? = testClassContainer.paths.testClass
        if (sourcePath !== null) {
            val directoriesAfterRepository: String = testClassContainer.paths.testClass!!.substringAfter("src/test").substringBefore(testClassContainer.className + testClassContainer.lang.extension)
            val newDir = outputDirectory + directoriesAfterRepository
            val newPath = YateIO.moveFileToDirectory(testClassContainer.paths.testClass!!, newDir)

            if (newPath !== null) {
                YateConsole.info("Generated test file has been moved. New path: $newPath")
            }
        }
    }

    /**
     * Checks whether the input string is actually a json compatible value and sanitize it to be used as a String
     * without changing its behaviour
     *
     * In case this is not a valid json string, it will return the input string as it is
     */
    fun sanitizeString(input: String): String {
        val mapper = ObjectMapper()
        val trimmed = input.trim()

        // Simple heuristic to detect JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return try {
                // Attempt to deserialize a Json value. If successful, remove the first and last quotes
                mapper.writeValueAsString(input).replaceFirst("\"", "").substringBeforeLast("\"")
            } catch (e: JsonProcessingException) {
                return input
            }
        }

        // Return quoted and escaped version of plain strings
        return input
    }
}