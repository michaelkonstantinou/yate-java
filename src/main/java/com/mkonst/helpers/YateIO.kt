package com.mkonst.helpers

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

    fun deleteFile(path: String): Boolean {
        val file = File(path)

        return file.delete()
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
     * Returns the name of the file without its file extension (e.g. filename.java will return filename)
     */
    fun getNameWithoutExtension(name: String): String {
        return name.substringBeforeLast(".")
    }

    /**
     * Removes the filename and its extension from the filepath to return the file's folder
     */
    fun getFolderFromPath(filepath: String): String {
        // Removes the filename to return the folder path
        return filepath.removeSuffix(getFilenameFromPath(filepath))
    }

    /**
     * Moves a file to a different directory even if the subdirectories of the new path do not exist
     *
     * Returns the new path of the file if the operation was successful
     */
    fun moveFileToDirectory(sourcePath: String, destinationDir: String): String? {
        try {
            val sourceFile = File(sourcePath)
            val targetDir = File(destinationDir)
            val targetFile = File(targetDir, sourceFile.name)

            // Ensure target directory exists. If not, create the missing directories
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Move the file
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            return targetFile.toPath().toString()
        } catch (e: Exception) {
            YateConsole.error("Exception thrown when moving file to directory")
            e.printStackTrace()
        }

        return null
    }
}
