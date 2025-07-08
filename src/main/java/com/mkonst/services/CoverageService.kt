package com.mkonst.services

import com.mkonst.config.ConfigYate
import com.mkonst.helpers.YateIO
import com.mkonst.types.MethodPosition
import com.mkonst.types.coverage.*
import org.jsoup.Jsoup
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object CoverageService {

    fun getMissingCoverageForClass(repositoryPath: String, className: String? = null): MissingCoverage? {
        val missingCoverages = getMissingCoverage(repositoryPath, className)

        return missingCoverages.firstOrNull()
    }

    fun getMissingCoverageForMethod(repositoryPath: String, className: String, methodPosition: MethodPosition): MissingCoverage? {
        val missingCoverages = getMissingCoverage(repositoryPath, className, methodPosition)

        return missingCoverages.firstOrNull()
    }

    fun getMissingCoverage(repositoryPath: String, className: String? = null, methodPosition: MethodPosition? = null): List<MissingCoverage> {
        val xmlFile = Paths.get(repositoryPath, "/target/site/jacoco/jacoco.xml").toFile()
        if (!xmlFile.exists()) throw IllegalArgumentException("Coverage file not found: $xmlFile")

        val factory = DocumentBuilderFactory.newInstance()

        // Disable DTD loading to avoid "report.dtd" error
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.isValidating = false
        factory.isNamespaceAware = false

        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        doc.documentElement.normalize()

        val missingCoveragePerClass = mutableListOf<MissingCoverage>()
        val sourceFileNodes = doc.getElementsByTagName("sourcefile")
        for (i in 0 until sourceFileNodes.length) {
            val elSourceFile = sourceFileNodes.item(i) as Element
            val sourceFileName = YateIO.getNameWithoutExtension(elSourceFile.getAttribute("name"))
            val missingCoverage = MissingCoverage(key = sourceFileName)

            // Filter out source files with different class name (if specified)
            if (className !== null && className != sourceFileName) {
                continue
            }

            // Find missing lines and branches and append accordingly to list of results
            val lines = elSourceFile.getElementsByTagName("line")
            for (k in 0 until lines.length) {
                val line = lines.item(k) as Element
                val lineNumber = line.getAttribute("nr").toInt()

                // Filter out missing lines/branches outside the given method position
                if (methodPosition !== null && lineNumber !in methodPosition.startLine..methodPosition.endLine) {
                    continue
                }

                val cb = line.getAttribute("cb").toInt()
                val mi = line.getAttribute("mi").toInt()
                val missedBranches = line.getAttribute("mb").toInt()

                if (missedBranches > 0) {
                    missingCoverage.missedBranches.add(BranchCoverage(lineNumber, missedBranches, coveredBranches = cb))
                }

                if (mi > 0) {
                    missingCoverage.missedLines.add(lineNumber.toString())
                }
            }

            // Append to the list of missing coverage only if branch/line coverage is not fully covered
            if (!missingCoverage.isFullyBranchCovered() || !missingCoverage.isFullyLineCovered()) {
                missingCoverage.mergeMissedLines()
                missingCoveragePerClass.add(missingCoverage)
            }
        }

        return missingCoveragePerClass
    }

    /**
     * Reads the jacoco html file for a specific class and returns a list of MethodCoverage instances that contain
     * information about methods with less than 100% branch coverage
     */
    fun getNotFullyCoveredMethodsForClass(repositoryPath: String, classQualifiedName: String): List<MethodCoverage> {
        val results: MutableList<MethodCoverage> = mutableListOf()

        val className = classQualifiedName.substringAfterLast(".")
        val packageName = classQualifiedName.substringBeforeLast(".")
        val file = Paths.get(repositoryPath, "/target/site/jacoco/", packageName, "$className.html").toFile()
        if (!file.exists()) throw IllegalArgumentException("Coverage file not found: $file")

        val doc = Jsoup.parse(file, "UTF-8")
        val table = doc.selectFirst("table.coverage") ?: error("Coverage table not found")

        table.select("tbody tr").mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size < 5) return@mapNotNull null

            // Get method name, and convert it into a YATE-friendly name (e.g. remove parenthesis)
            val link = cols[0].selectFirst("a")
            val methodNameSignature: String = link?.text()?.trim() ?: return@mapNotNull null
            var methodName = methodNameSignature.substringBefore("(")
            if (methodName == className) {
                methodName = "(Constructor) $methodNameSignature"
            }

            // Branch coverage: may be absent
            val missedBranches: Int = cols[3].select("img[alt]").firstOrNull { it.attr("src").contains("redbar") }
                ?.attr("alt")?.toIntOrNull() ?: 0

            // Coverage %
            val coveragePercent: Float = cols[2].text().trim().replace("%", "").toFloat()

            if (coveragePercent < 100 || missedBranches > 0) {
                results.add(MethodCoverage(methodName, coveragePercent, missedBranches))
            } else {
                null
            }
        }

        return results
    }

    /**
     * Reads the jacoco index file of the given repository, and returns a map with line-branch-method-class coverage
     */
    fun getJacocoCoverages(repositoryPath: String): JacocoCoverageHolder {
        val htmlFile = Paths.get(repositoryPath, "target/site/jacoco/index.html").toFile()
        val doc = Jsoup.parse(htmlFile, "UTF-8")

        val tfoot = doc.select("tfoot").firstOrNull()
            ?: throw IllegalStateException("tfoot not found in coverage report")
        val totalRow = tfoot.select("tr").firstOrNull()
            ?: throw IllegalStateException("tfoot row not found")

        val cols = totalRow.select("td")

        // Helper to clean and convert string to Int
        fun parseInt(text: String): Int =
            text.trim().replace(",", "").toInt()

        // Line coverage
        val missedLines = parseInt(cols[7].text())
        val allLines = parseInt(cols[8].text())
        val lineCoverage = BinaryCoverage(allLines - missedLines, allLines)

        // Branch coverage
        val (missedBranchesStr, allBranchesStr) = cols[3].text().trim().split("of")
        val missedBranches = parseInt(missedBranchesStr)
        val allBranches = parseInt(allBranchesStr)
        val branchCoverage = BinaryCoverage(allBranches - missedBranches, allBranches)

        // Method coverage
        val missedMethods = parseInt(cols[9].text())
        val allMethods = parseInt(cols[10].text())
        val methodCoverage = BinaryCoverage(allMethods - missedMethods, allMethods)

        // Class coverage
        val missedClasses = parseInt(cols[11].text())
        val allClasses = parseInt(cols[12].text())
        val classCoverage = BinaryCoverage(allClasses - missedClasses, allClasses)

        return JacocoCoverageHolder(lineCoverage, branchCoverage, methodCoverage, classCoverage)
    }
}