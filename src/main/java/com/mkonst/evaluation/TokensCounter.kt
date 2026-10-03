package com.mkonst.evaluation

data class TokensCounter(
    var input: Long = 0,
    var output: Long = 0,
    var total: Long = 0,
) {
    operator fun plus(other: TokensCounter): TokensCounter {
        return TokensCounter(
            input = this.input + other.input,
            output = this.output + other.output,
            total = this.total + other.total
        )
    }

    fun add(inputTokens: Int, outputTokens: Int, totalTokens: Int) {
        this.input += inputTokens
        this.output += outputTokens
        this.total += totalTokens
    }

    fun reset() {
        this.input = 0
        this.output = 0
        this.total = 0
    }
}
