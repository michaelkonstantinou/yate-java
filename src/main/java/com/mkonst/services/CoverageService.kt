package com.mkonst.services

import com.mkonst.helpers.YateIO
import com.mkonst.types.coverage.BranchCoverage
import com.mkonst.types.coverage.MethodCoverage
import com.mkonst.types.coverage.MissingCoverage
import org.jsoup.Jsoup
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object CoverageService {

    fun getMissingCoverageForClass(repositoryPath: String, className: String? = null): MissingCoverage? {
        val missingCoverages = getMissingCoverage(repositoryPath, className)

        return missingCoverages.firstOrNull()
    }

    fun getMissingCoverage(repositoryPath: String, className: String? = null): List<MissingCoverage> {
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
    fun getJacocoCoverages(repositoryPath: String): Map<String, String> {
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
        val lineCoverage = (allLines - missedLines).toDouble() / allLines

        // Branch coverage
        val (missedBranchesStr, allBranchesStr) = cols[3].text().trim().split("of")
        val missedBranches = parseInt(missedBranchesStr)
        val allBranches = parseInt(allBranchesStr)
        val branchCoverage = (allBranches - missedBranches).toDouble() / allBranches

        // Method coverage
        val missedMethods = parseInt(cols[9].text())
        val allMethods = parseInt(cols[10].text())
        val methodCoverage = (allMethods - missedMethods).toDouble() / allMethods

        // Class coverage
        val missedClasses = parseInt(cols[11].text())
        val allClasses = parseInt(cols[12].text())
        val classCoverage = (allClasses - missedClasses).toDouble() / allClasses

        return mapOf(
            "branch_coverage" to format(branchCoverage, allBranches - missedBranches, allBranches),
            "line_coverage" to format(lineCoverage, allLines - missedLines, allLines),
            "method_coverage" to format(methodCoverage, allMethods - missedMethods, allMethods),
            "class_coverage" to format(classCoverage, allClasses - missedClasses, allClasses),
        )
    }

    // Format to string with percent and comma decimal
    private fun format(coverage: Double, covered: Int, total: Int): String =
        String.format("%.2f%% (%d / %d)", coverage * 100, covered, total).replace('.', ',')
}