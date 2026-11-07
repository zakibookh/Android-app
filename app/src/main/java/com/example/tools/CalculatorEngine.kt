package com.example.tools

import kotlin.math.*

object CalculatorEngine {

    fun evaluate(expression: String): String {
        val expr = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
            .replace(" ", "")

        return try {
            val result = parseExpression(expr)
            if (result.isNaN() || result.isInfinite()) {
                "Error"
            } else if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                String.format("%.6f", result).trimEnd('0').trimEnd('.')
            }
        } catch (_: Exception) {
            "Syntax Error"
        }
    }

    private fun parseExpression(expr: String): Double {
        var pos = -1
        var ch = -1

        fun nextChar() {
            ch = if (++pos < expr.length) expr[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression(expr.substring(pos, findClosingParen(expr, pos - 1)))
                pos = findClosingParen(expr, startPos)
                nextChar()
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = expr.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = expr.substring(startPos, pos)
                if (eat('('.code)) {
                    val closing = findClosingParen(expr, pos - 1)
                    val arg = parseExpression(expr.substring(pos, closing))
                    pos = closing
                    nextChar()
                    x = when (func) {
                        "sqrt" -> sqrt(arg)
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "ln" -> ln(arg)
                        "log" -> log10(arg)
                        "abs" -> abs(arg)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Expected ( after $func")
                }
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        nextChar()
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    private fun findClosingParen(str: String, openPos: Int): Int {
        var depth = 0
        for (i in openPos until str.length) {
            if (str[i] == '(') depth++
            else if (str[i] == ')') {
                depth--
                if (depth == 0) return i
            }
        }
        return str.length - 1
    }
}
