package com.mkonst.interfaces.analysis

import com.mkonst.types.ClassBody

interface CodeClassParserInterface {
    fun getBodyDecoded(classContent: String): ClassBody
}