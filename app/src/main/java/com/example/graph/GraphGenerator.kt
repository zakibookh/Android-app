package com.example.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ChartType
import com.example.model.GraphObject
import kotlin.math.*

object GraphGenerator {

    /**
     * Evaluates standard mathematical functions f(x).
     */
    fun evaluateFunction(formula: String, x: Float): Float? {
        val clean = formula.replace("y", "")
            .replace("=", "")
            .replace("f(x)", "")
            .replace(" ", "")
            .lowercase()

        return try {
            when {
                // Trigonometric
                clean.startsWith("sin(x)") || clean == "sin" -> sin(x)
                clean.startsWith("cos(x)") || clean == "cos" -> cos(x)
                clean.startsWith("tan(x)") || clean == "tan" -> {
                    val t = tan(x)
                    if (abs(t) > 30f) null else t
                }
                // Exponential & Log
                clean.startsWith("exp(x)") || clean == "e^x" -> exp(x.coerceIn(-5f, 5f))
                clean.startsWith("ln(x)") || clean == "log(x)" -> if (x > 0f) ln(x) else null
                clean.startsWith("sqrt(x)") -> if (x >= 0f) sqrt(x) else null
                clean == "1/x" -> if (abs(x) < 0.05f) null else 1f / x
                clean == "|x|" || clean == "abs(x)" -> abs(x)

                // Powers
                clean == "x^2" || clean == "x*x" -> x * x
                clean == "x^3" -> x * x * x
                clean.contains("x^2") -> {
                    // Try parsing quadratic ax^2 + bx + c
                    evaluatePolynomial(clean, x)
                }
                clean.contains("x") -> {
                    // Try parsing linear mx + c
                    evaluateLinear(clean, x)
                }
                else -> x * x // default quadratic
            }
        } catch (_: Exception) {
            x * x
        }
    }

    private fun evaluatePolynomial(poly: String, x: Float): Float {
        // e.g., "x^2-4" or "2x^2+3x-1"
        var a = 1f
        var b = 0f
        var c = 0f

        val parts = poly.replace("-", "+-").split("+").filter { it.isNotBlank() }
        for (part in parts) {
            when {
                part.contains("x^2") -> {
                    val coeffStr = part.replace("x^2", "")
                    a = when (coeffStr) {
                        "", "+" -> 1f
                        "-" -> -1f
                        else -> coeffStr.toFloatOrNull() ?: 1f
                    }
                }
                part.contains("x") -> {
                    val coeffStr = part.replace("x", "")
                    b = when (coeffStr) {
                        "", "+" -> 1f
                        "-" -> -1f
                        else -> coeffStr.toFloatOrNull() ?: 0f
                    }
                }
                else -> {
                    c = part.toFloatOrNull() ?: 0f
                }
            }
        }
        return a * x * x + b * x + c
    }

    private fun evaluateLinear(line: String, x: Float): Float {
        // e.g. "2x+3" or "-x+4"
        val parts = line.replace("-", "+-").split("+").filter { it.isNotBlank() }
        var m = 1f
        var c = 0f

        for (part in parts) {
            if (part.contains("x")) {
                val coeffStr = part.replace("x", "")
                m = when (coeffStr) {
                    "", "+" -> 1f
                    "-" -> -1f
                    else -> coeffStr.toFloatOrNull() ?: 1f
                }
            } else {
                c = part.toFloatOrNull() ?: 0f
            }
        }
        return m * x + c
    }

    /**
     * Draws the complete graph or chart within DrawScope inside bounds (x, y, width, height).
     */
    fun drawGraph(drawScope: DrawScope, graph: GraphObject) {
        val left = graph.x
        val top = graph.y
        val w = graph.width
        val h = graph.height

        // Background card
        drawScope.drawRect(
            color = Color(0xDD111827),
            topLeft = Offset(left, top),
            size = Size(w, h)
        )
        // Card border
        drawScope.drawRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(left, top),
            size = Size(w, h),
            style = Stroke(width = 2f)
        )

        when (graph.chartType) {
            ChartType.FUNCTION -> drawFunctionGraph(drawScope, graph, left, top, w, h)
            ChartType.BAR -> drawBarChart(drawScope, graph, left, top, w, h)
            ChartType.LINE -> drawLineChart(drawScope, graph, left, top, w, h)
            ChartType.PIE -> drawPieChart(drawScope, graph, left, top, w, h)
        }
    }

    private fun drawFunctionGraph(
        drawScope: DrawScope,
        graph: GraphObject,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ) {
        val pad = 30f
        val plotW = w - pad * 2
        val plotH = h - pad * 2

        val xMin = graph.xMin
        val xMax = graph.xMax
        val yMin = graph.yMin
        val yMax = graph.yMax

        val toScreenX: (Float) -> Float = { x -> left + pad + ((x - xMin) / (xMax - xMin)) * plotW }
        val toScreenY: (Float) -> Float = { y -> top + pad + ((yMax - y) / (yMax - yMin)) * plotH }

        // Grid lines
        for (i in -10..10 step 2) {
            val gx = toScreenX(i.toFloat())
            if (gx in (left + pad)..(left + w - pad)) {
                drawScope.drawLine(
                    color = Color(0x3364748B),
                    start = Offset(gx, top + pad),
                    end = Offset(gx, top + h - pad),
                    strokeWidth = 1f
                )
            }
            val gy = toScreenY(i.toFloat())
            if (gy in (top + pad)..(top + h - pad)) {
                drawScope.drawLine(
                    color = Color(0x3364748B),
                    start = Offset(left + pad, gy),
                    end = Offset(left + w - pad, gy),
                    strokeWidth = 1f
                )
            }
        }

        // Coordinate Axes (X and Y)
        val originX = toScreenX(0f).coerceIn(left + pad, left + w - pad)
        val originY = toScreenY(0f).coerceIn(top + pad, top + h - pad)

        // X-axis
        drawScope.drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(left + pad, originY),
            end = Offset(left + w - pad, originY),
            strokeWidth = 2f
        )
        // Y-axis
        drawScope.drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(originX, top + pad),
            end = Offset(originX, top + h - pad),
            strokeWidth = 2f
        )

        // Curve path
        val path = Path()
        var isPathStarted = false
        val steps = 150
        val dx = (xMax - xMin) / steps

        for (i in 0..steps) {
            val currX = xMin + i * dx
            val currY = evaluateFunction(graph.formula, currX)
            if (currY != null && !currY.isNaN() && !currY.isInfinite()) {
                val scrX = toScreenX(currX)
                val scrY = toScreenY(currY).coerceIn(top + pad - 20f, top + h - pad + 20f)
                if (!isPathStarted) {
                    path.moveTo(scrX, scrY)
                    isPathStarted = true
                } else {
                    path.lineTo(scrX, scrY)
                }
            } else {
                isPathStarted = false
            }
        }

        drawScope.drawPath(
            path = path,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 3.5f)
        )
    }

    private fun drawBarChart(
        drawScope: DrawScope,
        graph: GraphObject,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ) {
        val pad = 40f
        val plotW = w - pad * 2
        val plotH = h - pad * 2
        val maxVal = max(1f, graph.dataValues.maxOrNull() ?: 100f)

        val colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFFBBF24),
            Color(0xFFF87171),
            Color(0xFFA78BFA),
            Color(0xFFF472B6)
        )

        val n = graph.dataValues.size
        if (n == 0) return
        val barSlotWidth = plotW / n
        val barWidth = barSlotWidth * 0.65f

        for (i in 0 until n) {
            val value = graph.dataValues[i]
            val barHeight = (value / maxVal) * plotH
            val barLeft = left + pad + i * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val barTop = top + h - pad - barHeight

            drawScope.drawRect(
                color = colors[i % colors.size],
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight)
            )
        }

        // Base line
        drawScope.drawLine(
            color = Color.White,
            start = Offset(left + pad, top + h - pad),
            end = Offset(left + w - pad, top + h - pad),
            strokeWidth = 2f
        )
    }

    private fun drawLineChart(
        drawScope: DrawScope,
        graph: GraphObject,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ) {
        val pad = 40f
        val plotW = w - pad * 2
        val plotH = h - pad * 2
        val maxVal = max(1f, graph.dataValues.maxOrNull() ?: 100f)

        val n = graph.dataValues.size
        if (n < 2) return
        val dx = plotW / (n - 1)

        val path = Path()
        val points = mutableListOf<Offset>()

        for (i in 0 until n) {
            val v = graph.dataValues[i]
            val px = left + pad + i * dx
            val py = top + h - pad - (v / maxVal) * plotH
            points.add(Offset(px, py))
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        drawScope.drawPath(
            path = path,
            color = Color(0xFF34D399),
            style = Stroke(width = 3f)
        )

        for (pt in points) {
            drawScope.drawCircle(
                color = Color.White,
                radius = 5f,
                center = pt
            )
        }
    }

    private fun drawPieChart(
        drawScope: DrawScope,
        graph: GraphObject,
        left: Float,
        top: Float,
        w: Float,
        h: Float
    ) {
        val total = graph.dataValues.sum()
        if (total <= 0f) return

        val radius = min(w, h) * 0.35f
        val center = Offset(left + w / 2f, top + h / 2f)

        val colors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFFBBF24),
            Color(0xFFF87171),
            Color(0xFFA78BFA)
        )

        var currentAngle = -90f
        for (i in graph.dataValues.indices) {
            val sweep = (graph.dataValues[i] / total) * 360f
            drawScope.drawArc(
                color = colors[i % colors.size],
                startAngle = currentAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            currentAngle += sweep
        }
    }
}
