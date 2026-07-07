package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Safe equation evaluator tokenizer using BigDecimal for perfect financial precision.
 */
fun evaluateSimpleExpression(expr: String): Double? {
    try {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()
        for (char in sanitized) {
            if (char.isDigit() || char == '.') {
                currentNum.append(char)
            } else if (char in listOf('+', '-', '*', '/')) {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                }
                tokens.add(char.toString())
            }
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum.toString())
        }
        
        if (tokens.isEmpty()) return null
        
        // Product stage
        val intermediateTokens = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                if (intermediateTokens.isEmpty() || i + 1 >= tokens.size) return null
                val prev = BigDecimal(intermediateTokens.removeAt(intermediateTokens.size - 1))
                val next = BigDecimal(tokens[i + 1])
                val res = if (token == "*") {
                    prev.multiply(next)
                } else {
                    // Avoid Non-terminating decimal expansion exception by specifying scale and RoundingMode
                    prev.divide(next, 10, RoundingMode.HALF_UP)
                }
                intermediateTokens.add(res.stripTrailingZeros().toPlainString())
                i += 2
            } else {
                intermediateTokens.add(token)
                i++
            }
        }
        
        // Sum additions stage
        if (intermediateTokens.isEmpty()) return null
        var result = BigDecimal(intermediateTokens[0])
        var j = 1
        while (j < intermediateTokens.size) {
            val op = intermediateTokens[j]
            if (j + 1 >= intermediateTokens.size) break
            val nextVal = BigDecimal(intermediateTokens[j + 1])
            if (op == "+") {
                result = result.add(nextVal)
            } else if (op == "-") {
                result = result.subtract(nextVal)
            }
            j += 2
        }
        return result.toDouble()
    } catch (e: Exception) {
        return null
    }
}
