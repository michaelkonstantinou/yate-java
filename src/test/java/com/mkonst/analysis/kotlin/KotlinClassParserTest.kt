package com.mkonst.analysis.kotlin

import com.mkonst.helpers.YateIO
import com.mkonst.types.ClassBody
import org.junit.jupiter.api.Test
import kotlin.test.*

class KotlinClassParserTest {

    @Test
    fun `test can decode kotlin class`() {
        val parser: KotlinClassParser = KotlinClassParser()
        val content: ClassBody = parser.getBodyDecoded(YateIO.readFile("src/test/dummyrepo/src/main/java/dummy/MyDummyKotlinClass.kt"))

        assertEquals("dummy", content.packageName)
        assertContains(content.imports, "java.utils.String")
        assertFalse(content.hasConstructors)
    }
}