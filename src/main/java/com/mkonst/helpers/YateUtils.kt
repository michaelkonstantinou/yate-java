package com.mkonst.helpers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.mkonst.analysis.ClassContainer
import com.mkonst.config.ConfigYate
import com.mkonst.types.DependencyTool
import com.mkonst.types.YateResponse
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object YateUtils {

    /**
     * Formats the given float value into a decimal number of 2 digits
     */
    fun formatDecimal(value: Float): String {
        return String.format("%.2f", value).replace(".", ConfigYate.getString("CHAR_COMMA"))
    }

    /**
     * Formats a time in milliseconds to Hours:Minutes:Seconds:Milliseconds format
     */
    fun formatMillisToMinSec(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        val ms = (millis % 1000) / 10

        return String.format("%02dh:%02dm:%02ds:%02dmi", hours, minutes, seconds, ms)
    }

    fun timestamp(): String {
        val now = LocalDateTime.now()
        val formatted = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))

        return formatted
    }

    /**
     * The method will move the generate test file, to the specified outputDirectory.
     * If the given response object does not contain a valid test class path, the method will do nothing
     */
    fun moveGeneratedTestClass(testClassContainer: ClassContainer, outputDirectory: String? = null) {
        if (outputDirectory === null) {
            return
        }

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

    /**
     * Returns the dependency tool used for the given repository based on the files that exist in the repository.
     */
    fun getDependencyTool(repositoryPath: String): DependencyTool {
        val pomFile = File(repositoryPath, "pom.xml")

        return if (pomFile.exists() && pomFile.isFile) DependencyTool.MAVEN else DependencyTool.GRADLE
    }
}