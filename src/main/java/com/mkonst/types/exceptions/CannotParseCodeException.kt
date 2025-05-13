package com.mkonst.types.exceptions

class CannotParseCodeException(reason: String = "Unknown"): Exception("Parser cannot compile/parse the given code content. Reason: $reason")