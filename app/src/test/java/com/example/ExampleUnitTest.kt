package com.example

import com.example.engine.ShapeRecognizer
import com.example.graph.GraphGenerator
import com.example.math.FormulaLibrary
import com.example.model.PointF
import com.example.model.ShapeType
import com.example.tools.CalculatorEngine
import com.example.tools.PeriodicTableData
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class ExampleUnitTest {

    @Test
    fun `test calculator arithmetic and trigonometry`() {
        assertEquals("14", CalculatorEngine.evaluate("2 + 3 * 4"))
        assertEquals("1", CalculatorEngine.evaluate("sin(90)"))
        assertEquals("12", CalculatorEngine.evaluate("sqrt(144)"))
        assertEquals("0", CalculatorEngine.evaluate("cos(90)"))
        assertEquals("8", CalculatorEngine.evaluate("2 ^ 3"))
    }

    @Test
    fun `test graph generator polynomial and linear evaluation`() {
        val quad = GraphGenerator.evaluateFunction("y = x^2 - 4", 2f)
        assertEquals(0f, quad ?: -1f, 0.01f)

        val linear = GraphGenerator.evaluateFunction("y = 3x + 5", 2f)
        assertEquals(11f, linear ?: -1f, 0.01f)
    }

    @Test
    fun `test shape recognizer detects straight lines`() {
        val points = (0..10).map { PointF(it * 15f, 50f) }
        val result = ShapeRecognizer.recognizeShape(points, 0xFFFFFFFFL, 4f)
        assertNotNull(result)
        assertEquals(ShapeType.LINE, result?.shape?.type)
    }

    @Test
    fun `test shape recognizer detects circles`() {
        val points = (0..360 step 10).map { deg ->
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            PointF(100f + 40f * cos(rad), 100f + 40f * sin(rad))
        }
        val result = ShapeRecognizer.recognizeShape(points, 0xFFFFFFFFL, 4f)
        assertNotNull(result)
        assertEquals(ShapeType.CIRCLE, result?.shape?.type)
    }

    @Test
    fun `test formula library contains real educational formulas`() {
        assertTrue(FormulaLibrary.formulas.size >= 20)
        assertTrue(FormulaLibrary.categories.contains("Calculus & Analysis"))
        assertTrue(FormulaLibrary.categories.contains("Geometry & Shapes"))
    }

    @Test
    fun `test periodic table data contains fundamental elements`() {
        val hydrogen = PeriodicTableData.elements.find { it.symbol == "H" }
        assertNotNull(hydrogen)
        assertEquals(1, hydrogen?.number)

        val gold = PeriodicTableData.elements.find { it.symbol == "Au" }
        assertNotNull(gold)
        assertEquals(79, gold?.number)
    }

    @Test
    fun `test ML Kit Digital Ink service builds valid Ink object from whiteboard strokes`() {
        val service = com.example.mlkit.DigitalInkRecognitionService()
        val points = listOf(
            PointF(10f, 20f, 1f, 1000L),
            PointF(15f, 25f, 1f, 1020L),
            PointF(20f, 30f, 1f, 1040L)
        )
        val stroke = com.example.model.Stroke(
            points = points,
            color = 0xFF38BDF8L,
            strokeWidth = 5f,
            penType = com.example.model.PenType.BALLPOINT
        )
        val ink = service.buildInk(listOf(stroke))
        assertNotNull(ink)
        assertEquals(1, ink.strokes.size)
        assertEquals(3, ink.strokes[0].points.size)
        assertEquals(10f, ink.strokes[0].points[0].x, 0.001f)
        assertEquals(20f, ink.strokes[0].points[0].y, 0.001f)
    }

    @Test
    fun `test ML Kit Digital Ink model identifier resolves tags`() {
        val service = com.example.mlkit.DigitalInkRecognitionService()
        val enModel = service.getModelIdentifier("en-US")
        assertNotNull(enModel)
        assertEquals("en-US", enModel?.languageTag)

        val autoDrawModel = service.getModelIdentifier("autodraw")
        assertNotNull(autoDrawModel)
        assertEquals("zxx-Zsym-x-autodraw", autoDrawModel?.languageTag)

        val somaliModel = service.getModelIdentifier("so")
        assertNotNull(somaliModel)
        assertEquals("so", somaliModel?.languageTag)

        val arabicModel = service.getModelIdentifier("ar")
        assertNotNull(arabicModel)
        assertEquals("ar", arabicModel?.languageTag)
    }
}

