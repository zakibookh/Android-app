package com.example.model

import java.util.UUID

data class PointF(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PenType(val displayName: String) {
    BALLPOINT("Ballpoint Pen"),
    ARROW_NIB("Qalin Arrow / Chisel Nib"),
    CALLIGRAPHY("Qalin Calligraphy / Jaba"),
    FOUNTAIN("Qalin Fountain / Assist"),
    NEON("Neon Glow Pen"),
    MARKER("Marker"),
    HIGHLIGHTER("Highlighter"),
    ERASER("Eraser")
}

data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointF>,
    val color: Long,
    val strokeWidth: Float,
    val penType: PenType,
    val isHighlighter: Boolean = false
)

enum class ShapeType(val displayName: String) {
    LINE("Straight Line"),
    ARROW("Arrow"),
    DOUBLE_ARROW("Double Arrow"),
    RECTANGLE("Rectangle"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    ELLIPSE("Ellipse"),
    TRIANGLE("Triangle"),
    PARALLELOGRAM("Parallelogram"),
    COORDINATE_AXES("Coordinate Axes")
}

data class ShapeObject(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val strokeColor: Long,
    val strokeWidth: Float = 4f,
    val fillColor: Long = 0x00000000L,
    val isFilled: Boolean = false
)

data class TextBlockObject(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    val y: Float,
    val fontSize: Float = 22f,
    val color: Long = 0xFFFFFFFFL,
    val isMath: Boolean = false,
    val latex: String = ""
)

enum class ChartType(val displayName: String) {
    FUNCTION("Function Curve"),
    BAR("Bar Chart"),
    LINE("Line Chart"),
    PIE("Pie Chart")
}

data class GraphObject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Plot",
    val formula: String = "y = x^2",
    val chartType: ChartType = ChartType.FUNCTION,
    val xMin: Float = -10f,
    val xMax: Float = 10f,
    val yMin: Float = -10f,
    val yMax: Float = 10f,
    val dataLabels: List<String> = listOf("A", "B", "C", "D"),
    val dataValues: List<Float> = listOf(25f, 40f, 15f, 60f),
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 400f,
    val height: Float = 300f
)

enum class BackgroundType(val displayName: String) {
    GRID("Grid Paper"),
    LINED("Lined Paper"),
    DOTTED("Dotted Grid"),
    ISOMETRIC("Isometric 3D"),
    BLANK("Blank Canvas")
}

enum class BoardTheme(val displayName: String, val bgHex: Long, val gridHex: Long, val defaultPenHex: Long) {
    DARK_SLATE("Dark Slate", 0xFF0F172AL, 0xFF1E293BL, 0xFF38BDF8L),
    BLACKBOARD("Chalkboard Green", 0xFF0B2516L, 0xFF143B25L, 0xFFFDE047L),
    WHITE("Classic Whiteboard", 0xFFFAFAFAC, 0xFFE2E8F0L, 0xFF0F172AL),
    SEPIA("Warm Sepia", 0xFFFDF6E2L, 0xFFE6D6B8L, 0xFF4A3728L),
    BLUEPRINT("Blueprint Blue", 0xFF0A192FL, 0xFF172A45L, 0xFF64FFDAL)
}

enum class ToolMode(val displayName: String) {
    PEN("Digital Pen"),
    SHAPE("Smart Shape"),
    ERASER("Eraser"),
    LASSO("Lasso Select"),
    TEXT("Text Card"),
    GRAPH("Graph Generator"),
    RULER("Ruler"),
    PROTRACTOR("Protractor"),
    COMPASS("Compass"),
    LASER_POINTER("Laser Pointer")
}

data class LassoSelection(
    val boundingBox: RectF? = null,
    val strokeIds: Set<String> = emptySet(),
    val shapeIds: Set<String> = emptySet(),
    val textBlockIds: Set<String> = emptySet()
)

data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    fun intersects(other: RectF): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }
}
