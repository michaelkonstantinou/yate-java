package com.mkonst.helpers

import java.io.File

object YateIO {
    /**
     * Reads a file and returns its full content as a String
     */
    fun readFile(filename: String): String {
        return File(filename).readText()
    }

    fun writeFile(filepath: String, content: String) {
        val file = File(filepath)

        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun getClassNameFromPath(classPath: String, suffix: String = ".java"): String {
        return classPath.substringAfterLast("/").removeSuffix(suffix)
    }

    /**
     * Returns the filename without its extension (after the last '/')
     */
    fun getFilenameFromPath(filepath: String): String {
        return filepath.substringAfterLast('/')
    }

    /**
     * Removes the filename and its extension from the filepath to return the file's folder
     */
    fun getFolderFromPath(filepath: String): String {
        // Removes the filename to return the folder path
        return filepath.removeSuffix(getFilenameFromPath(filepath))
    }
}
