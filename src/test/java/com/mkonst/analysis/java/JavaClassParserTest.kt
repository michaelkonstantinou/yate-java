package com.mkonst.analysis.java

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JavaClassParserTest {

    @Test
    fun `test filterInvalidImports removes malformed imports`() {
        val rawImports = mutableListOf(
            "import com.example.Foo;",
            " import com.example.Bar;  ",
            "import ;",                          // malformed
            "import // broken",                  // malformed
            "// import commented out",           // comment
            "",                                  // blank line
            "import ",                          // invalid
            "import com.valid.Class;",             // valid
            "import okhttp3.Request\$Builder",
            "import // Ensure this import is correct import com.looks.good;" // invalid
        )

        val expected = listOf(
            "import com.example.Foo;",
            "import com.example.Bar;",
            "import com.valid.Class;"
        )

        var classContent: String = "package com.example;\n\n"
        classContent += rawImports.joinToString("\n")
        classContent += """

        public class Dummy {
            public void foo() {
                System.out.println("Just a test");
            }
        }
    """.trimIndent()

        val parser = JavaClassParser()
        val body = parser.getBodyDecoded(classContent)

        assertEquals(expected, body.imports)
    }
}