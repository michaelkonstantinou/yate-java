package com.mkonst.types.exceptions

import com.mkonst.types.ProgramLangType

class NotSupportedLanguageException(lang: ProgramLangType, operationDetails: String = ""): Exception("Programming language ${lang.name} is not supported for this operation. ($operationDetails)")