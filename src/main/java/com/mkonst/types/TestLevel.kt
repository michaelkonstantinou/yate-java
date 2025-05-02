package com.mkonst.types

/**
 * CLASS -> (Class level) Generates tests for the whole class
 * METHOD -> (Method level) Generates tests for each method individually + constructors
 * METHOD_RESTRICT -> (Method level without Constructors)
 * CONSTRUCTOR -> Generates tests only for the constructors (not each one individually)
 * HYBRID -> Creates class-level tests first, and then it creates tests for each method/constructor missing tests
 */
enum class TestLevel {
    CLASS, CONSTRUCTOR, METHOD, METHOD_RESTRICT, HYBRID
}