package com.mkonst.helpers

import org.junit.jupiter.api.Test
import kotlin.test.*

class YateKotlinUtilsTest {

    val kotlinSampleClass = """
    import org.junit.Test

    class SampleTest {
    
        @Test
        fun testOne() {}

        fun helper() {}

        @Test
        fun testTwo() {}
    }
""".trimIndent()

    @Test
    fun `test can count test methods for class content`() {
        val count = YateKotlinUtils.countTestMethodsForContent(kotlinSampleClass)

        assertEquals(2, count)
    }
}