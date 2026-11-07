package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.StrokeSmoothing
import com.example.graph.GraphGenerator
import com.example.model.*
import kotlin.math.*

@Composable
fun WhiteboardCanvas(
    uiState: WhiteboardUiState,
    onAddStroke: (Stroke) -> Unit,
    onAddShape: (ShapeObject) -> Unit,
    onErase: (Float, Float) -> Unit,
    onUpdateRuler: (PointF, Float) -> Unit,
    onUpdateProtractor: (PointF) -> Unit,
    onUpdateCompass: (PointF, Float) -> Unit,
    onEditTextBlock: (TextBlockObject) -> Unit,
    onDeleteTextBlock: (String) -> Unit,
    onDeleteGraph: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Canvas Pan & Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Active in-progress stroke
    var activePoints by remember { mutableStateOf<List<PointF>>(emptyList()) }
    var activeShapeStart by remember { mutableStateOf<PointF?>(null) }
    var activeShapeCurrent by remember { mutableStateOf<PointF?>(null) }

    // Laser pointer position in presentation mode
    var laserPoint by remember { mutableStateOf<Offset?>(null) }

    // Text block editing dialog state
    var editingBlock by remember { mutableStateOf<TextBlockObject?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(uiState.theme.bgHex))
            .testTag("whiteboard_canvas_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("drawing_canvas")
                // Native Compose Pointer Gestures: Ultra-fast, zero event drops, seamless 1-finger draw & 2-finger zoom
                .pointerInput(uiState.currentTool, uiState.stylusOnlyMode, uiState.penColor, uiState.strokeWidth, panOffset, scale) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val isStylus = firstDown.type == PointerType.Stylus

                        // Palm rejection: If stylusOnlyMode is active, reject finger touches for drawing
                        if (uiState.stylusOnlyMode && !isStylus) {
                            return@awaitEachGesture
                        }

                        var isMultiTouch = false
                        var prevChanges: List<PointerInputChange> = listOf(firstDown)

                        // Convert first down to canvas coordinates
                        val startCanvasX = (firstDown.position.x - panOffset.x) / scale
                        val startCanvasY = (firstDown.position.y - panOffset.y) / scale
                        val pressure = firstDown.pressure.coerceIn(0.2f, 2.0f)

                        when (uiState.currentTool) {
                            ToolMode.ERASER -> onErase(startCanvasX, startCanvasY)
                            ToolMode.SHAPE -> {
                                activeShapeStart = PointF(startCanvasX, startCanvasY, pressure)
                                activeShapeCurrent = PointF(startCanvasX, startCanvasY, pressure)
                            }
                            ToolMode.LASER_POINTER -> laserPoint = Offset(startCanvasX, startCanvasY)
                            else -> activePoints = listOf(PointF(startCanvasX, startCanvasY, pressure))
                        }
                        firstDown.consume()

                        do {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }

                            if (pressedChanges.size > 1) {
                                // 2+ pointers: switch to smooth pan and zoom
                                if (!isMultiTouch) {
                                    isMultiTouch = true
                                    activePoints = emptyList()
                                    activeShapeStart = null
                                    activeShapeCurrent = null
                                }

                                val p1 = pressedChanges[0]
                                val p2 = pressedChanges[1]
                                val prevP1 = prevChanges.find { it.id == p1.id } ?: p1
                                val prevP2 = prevChanges.find { it.id == p2.id } ?: p2

                                val currDist = hypot(p1.position.x - p2.position.x, p1.position.y - p2.position.y)
                                val prevDist = hypot(prevP1.position.x - prevP2.position.x, prevP1.position.y - prevP2.position.y)

                                if (prevDist > 0f) {
                                    val factor = currDist / prevDist
                                    scale = (scale * factor).coerceIn(0.4f, 4f)
                                }

                                val currCenter = Offset((p1.position.x + p2.position.x) / 2f, (p1.position.y + p2.position.y) / 2f)
                                val prevCenter = Offset((prevP1.position.x + prevP2.position.x) / 2f, (prevP1.position.y + prevP2.position.y) / 2f)
                                panOffset += (currCenter - prevCenter)

                                p1.consume()
                                p2.consume()
                            } else if (pressedChanges.size == 1 && !isMultiTouch) {
                                val change = pressedChanges[0]
                                val canvasX = (change.position.x - panOffset.x) / scale
                                val canvasY = (change.position.y - panOffset.y) / scale
                                val p = change.pressure.coerceIn(0.2f, 2.0f)

                                when (uiState.currentTool) {
                                    ToolMode.ERASER -> onErase(canvasX, canvasY)
                                    ToolMode.SHAPE -> activeShapeCurrent = PointF(canvasX, canvasY, p)
                                    ToolMode.LASER_POINTER -> laserPoint = Offset(canvasX, canvasY)
                                    else -> activePoints = activePoints + PointF(canvasX, canvasY, p)
                                }
                                change.consume()
                            }
                            prevChanges = pressedChanges
                        } while (pressedChanges.isNotEmpty())

                        // Pointer lifted: commit committed stroke or shape
                        if (!isMultiTouch) {
                            if (uiState.currentTool == ToolMode.SHAPE) {
                                val s = activeShapeStart
                                val c = activeShapeCurrent
                                if (s != null && c != null && hypot(c.x - s.x, c.y - s.y) > 10f) {
                                    onAddShape(
                                        ShapeObject(
                                            type = uiState.selectedShapeType,
                                            startX = s.x,
                                            startY = s.y,
                                            endX = c.x,
                                            endY = c.y,
                                            strokeColor = uiState.penColor,
                                            strokeWidth = uiState.strokeWidth,
                                            fillColor = uiState.shapeFillColor,
                                            isFilled = uiState.isShapeFilled
                                        )
                                    )
                                }
                            } else if (activePoints.isNotEmpty()) {
                                onAddStroke(
                                    Stroke(
                                        points = activePoints,
                                        color = uiState.penColor,
                                        strokeWidth = uiState.strokeWidth,
                                        penType = uiState.penType,
                                        isHighlighter = uiState.penType == PenType.HIGHLIGHTER
                                    )
                                )
                            }
                        }

                        activePoints = emptyList()
                        activeShapeStart = null
                        activeShapeCurrent = null
                        laserPoint = null
                    }
                }
        ) {
            // Apply Canvas Zoom & Pan
            drawContext.canvas.save()
            drawContext.canvas.translate(panOffset.x, panOffset.y)
            drawContext.canvas.scale(scale, scale)

            // 1. Draw Background Pattern
            drawBackgroundPattern(
                backgroundType = uiState.backgroundType,
                gridColor = Color(uiState.theme.gridHex),
                width = size.width / scale,
                height = size.height / scale,
                panX = panOffset.x / scale,
                panY = panOffset.y / scale
            )

            // 2. Draw Committed Shapes
            for (shape in uiState.shapes) {
                drawShapeObject(shape)
            }

            // 3. Draw Committed Strokes
            for (stroke in uiState.strokes) {
                drawStrokeObject(stroke)
            }

            // 4. Draw In-Progress Shape
            val shapeStart = activeShapeStart
            val shapeCur = activeShapeCurrent
            if (shapeStart != null && shapeCur != null) {
                drawShapeObject(
                    ShapeObject(
                        type = uiState.selectedShapeType,
                        startX = shapeStart.x,
                        startY = shapeStart.y,
                        endX = shapeCur.x,
                        endY = shapeCur.y,
                        strokeColor = uiState.penColor,
                        strokeWidth = uiState.strokeWidth,
                        fillColor = uiState.shapeFillColor,
                        isFilled = uiState.isShapeFilled
                    )
                )
            }

            // 5. Draw In-Progress Active Stroke (Immediate visual feedback while finger/pen moves)
            if (activePoints.isNotEmpty()) {
                val activeStroke = Stroke(
                    points = activePoints,
                    color = uiState.penColor,
                    strokeWidth = uiState.strokeWidth,
                    penType = uiState.penType,
                    isHighlighter = uiState.penType == PenType.HIGHLIGHTER
                )
                drawStrokeObject(activeStroke)
            }

            // 6. Draw Graphs & Charts
            for (graph in uiState.graphs) {
                GraphGenerator.drawGraph(this, graph)
            }

            // 7. Draw Presentation Laser Pointer
            val laser = laserPoint
            if (laser != null) {
                drawCircle(color = Color(0x33FF0055), radius = 32f, center = laser)
                drawCircle(color = Color(0x88FF0055), radius = 18f, center = laser)
                drawCircle(color = Color(0xFFFF0055), radius = 8f, center = laser)
            }

            // 8. Draw Digital Ruler overlay if enabled
            if (uiState.showRuler) {
                drawRulerOverlay(uiState.rulerPos, uiState.rulerAngle)
            }

            // 9. Draw Protractor overlay if enabled
            if (uiState.showProtractor) {
                drawProtractorOverlay(uiState.protractorPos)
            }

            // 10. Draw Compass overlay if enabled
            if (uiState.showCompass) {
                drawCompassOverlay(uiState.compassPos, uiState.compassRadius)
            }

            drawContext.canvas.restore()
        }

        // Overlay Clean Computer Text Blocks
        for (block in uiState.textBlocks) {
            val screenX = (block.x * scale + panOffset.x)
            val screenY = (block.y * scale + panOffset.y)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .offset { IntOffset(screenX.toInt(), screenY.toInt()) }
                    .padding(4.dp)
                    .pointerInput(block.id) {
                        detectTapGestures(
                            onTap = { editingBlock = block },
                            onLongPress = { editingBlock = block },
                            onDoubleTap = { editingBlock = block }
                        )
                    }
                    .testTag("text_block_${block.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Surface(
                            color = if (block.isMath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (block.isMath) "📐 MATH" else "⌨️ COMPUTER TEXT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Text(
                        text = block.text,
                        color = Color(block.color),
                        fontSize = (block.fontSize * scale).coerceIn(14f, 48f).sp,
                        fontFamily = if (block.isMath) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Text Block Editor Dialog
        val editing = editingBlock
        if (editing != null) {
            var editText by remember(editing) { mutableStateOf(editing.text) }
            AlertDialog(
                onDismissRequest = { editingBlock = null },
                title = { Text(if (editing.isMath) "Edit Mathematical Equation" else "Edit Computer Text") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxSize(0.8f),
                        label = { Text("Content") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onEditTextBlock(editing.copy(text = editText))
                            editingBlock = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            onDeleteTextBlock(editing.id)
                            editingBlock = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            )
        }
    }
}

// --- Drawing Helper Functions ---

private fun DrawScope.drawBackgroundPattern(
    backgroundType: BackgroundType,
    gridColor: Color,
    width: Float,
    height: Float,
    panX: Float,
    panY: Float
) {
    when (backgroundType) {
        BackgroundType.GRID -> {
            val step = 40f
            var x = -panX % step
            while (x < width + 1000f) {
                drawLine(gridColor, Offset(x, -panY), Offset(x, height + 1000f), strokeWidth = 1f)
                x += step
            }
            var y = -panY % step
            while (y < height + 1000f) {
                drawLine(gridColor, Offset(-panX, y), Offset(width + 1000f, y), strokeWidth = 1f)
                y += step
            }
        }
        BackgroundType.LINED -> {
            val step = 44f
            var y = -panY % step
            while (y < height + 1000f) {
                drawLine(gridColor, Offset(-panX, y), Offset(width + 1000f, y), strokeWidth = 1.2f)
                y += step
            }
        }
        BackgroundType.DOTTED -> {
            val step = 40f
            var x = -panX % step
            while (x < width + 1000f) {
                var y = -panY % step
                while (y < height + 1000f) {
                    drawCircle(gridColor, radius = 1.8f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        BackgroundType.ISOMETRIC -> {
            val step = 50f
            var x = -panX % step
            while (x < width + 1000f) {
                drawLine(gridColor, Offset(x, -panY), Offset(x + (height + 1000f) * 0.577f, height + 1000f), strokeWidth = 0.8f)
                drawLine(gridColor, Offset(x, -panY), Offset(x - (height + 1000f) * 0.577f, height + 1000f), strokeWidth = 0.8f)
                x += step
            }
        }
        BackgroundType.BLANK -> {
            // Clean unruled canvas
        }
    }
}

private fun DrawScope.drawStrokeObject(stroke: Stroke) {
    if (stroke.points.isEmpty()) return

    val path = StrokeSmoothing.createSmoothPath(stroke.points)
    val baseColor = Color(stroke.color)

    when (stroke.penType) {
        PenType.HIGHLIGHTER -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = 0.35f),
                style = CanvasStroke(
                    width = stroke.strokeWidth * 3.2f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Square
                )
            )
        }
        PenType.NEON -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = 0.4f),
                style = CanvasStroke(width = stroke.strokeWidth * 2.5f)
            )
            drawPath(
                path = path,
                color = Color.White,
                style = CanvasStroke(width = stroke.strokeWidth * 0.8f)
            )
        }
        PenType.ARROW_NIB -> {
            drawCalligraphyNibStroke(stroke, baseColor, isArrowNib = true)
        }
        PenType.CALLIGRAPHY -> {
            drawCalligraphyNibStroke(stroke, baseColor, isArrowNib = false)
        }
        PenType.FOUNTAIN -> {
            drawFountainNibStroke(stroke, baseColor)
        }
        PenType.MARKER -> {
            drawPath(
                path = path,
                color = baseColor.copy(alpha = 0.88f),
                style = CanvasStroke(
                    width = stroke.strokeWidth * 1.8f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Square,
                    join = androidx.compose.ui.graphics.StrokeJoin.Bevel
                )
            )
        }
        else -> {
            drawPath(
                path = path,
                color = baseColor,
                style = CanvasStroke(
                    width = stroke.strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Draws real Calligraphy / Chisel Nib strokes with 45-degree angle stroke dynamics (thick & thin contrast).
 * Helps the hand write beautiful scripts (Arabic, Somali, English) & mathematical symbols.
 */
private fun DrawScope.drawCalligraphyNibStroke(stroke: Stroke, baseColor: Color, isArrowNib: Boolean) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    if (pts.size == 1) {
        drawCircle(baseColor, radius = stroke.strokeWidth * 0.9f, center = Offset(pts[0].x, pts[0].y))
        return
    }

    val resampled = StrokeSmoothing.resample(pts, interval = 3.5f)
    if (resampled.size < 2) {
        val path = StrokeSmoothing.createSmoothPath(pts)
        drawPath(path, baseColor, style = CanvasStroke(width = stroke.strokeWidth * 1.5f))
        return
    }

    val nibAngleRad = Math.toRadians(45.0).toFloat()
    val nibDx = cos(nibAngleRad) * (stroke.strokeWidth * 1.8f)
    val nibDy = sin(nibAngleRad) * (stroke.strokeWidth * 1.8f)

    val leftPoints = mutableListOf<Offset>()
    val rightPoints = mutableListOf<Offset>()

    for (p in resampled) {
        val wFactor = p.pressure.coerceIn(0.5f, 1.8f)
        val dx = nibDx * wFactor
        val dy = nibDy * wFactor
        leftPoints.add(Offset(p.x - dx / 2f, p.y - dy / 2f))
        rightPoints.add(Offset(p.x + dx / 2f, p.y + dy / 2f))
    }

    val ribbonPath = Path().apply {
        if (leftPoints.isNotEmpty()) {
            moveTo(leftPoints[0].x, leftPoints[0].y)
            for (i in 1 until leftPoints.size) {
                lineTo(leftPoints[i].x, leftPoints[i].y)
            }
            for (i in rightPoints.indices.reversed()) {
                lineTo(rightPoints[i].x, rightPoints[i].y)
            }
            close()
        }
    }

    drawPath(path = ribbonPath, color = baseColor, style = Fill)

    if (isArrowNib && resampled.size >= 3) {
        val lastP = resampled.last()
        val prevP = resampled[resampled.size - 3]
        val angle = atan2(lastP.y - prevP.y, lastP.x - prevP.x)
        val headLen = stroke.strokeWidth * 2.2f
        val p1 = Offset(lastP.x - headLen * cos(angle - 0.45f), lastP.y - headLen * sin(angle - 0.45f))
        val p2 = Offset(lastP.x - headLen * cos(angle + 0.45f), lastP.y - headLen * sin(angle + 0.45f))
        val arrowPath = Path().apply {
            moveTo(lastP.x, lastP.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawPath(path = arrowPath, color = baseColor, style = Fill)
    }
}

/**
 * Draws Fountain Nib strokes with handwriting-assistance shading and ink concentration.
 */
private fun DrawScope.drawFountainNibStroke(stroke: Stroke, baseColor: Color) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    val smoothPath = StrokeSmoothing.createSmoothPath(pts)

    drawPath(
        path = smoothPath,
        color = baseColor,
        style = CanvasStroke(
            width = stroke.strokeWidth * 1.4f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
    drawPath(
        path = smoothPath,
        color = baseColor.copy(alpha = 0.85f),
        style = CanvasStroke(
            width = stroke.strokeWidth * 0.7f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}

private fun DrawScope.drawShapeObject(shape: ShapeObject) {
    val strokeColor = Color(shape.strokeColor)
    val fillColor = Color(shape.fillColor)

    val minX = minOf(shape.startX, shape.endX)
    val maxX = maxOf(shape.startX, shape.endX)
    val minY = minOf(shape.startY, shape.endY)
    val maxY = maxOf(shape.startY, shape.endY)
    val w = maxX - minX
    val h = maxY - minY

    when (shape.type) {
        ShapeType.LINE -> {
            drawLine(
                color = strokeColor,
                start = Offset(shape.startX, shape.startY),
                end = Offset(shape.endX, shape.endY),
                strokeWidth = shape.strokeWidth
            )
        }
        ShapeType.ARROW -> {
            drawLine(
                color = strokeColor,
                start = Offset(shape.startX, shape.startY),
                end = Offset(shape.endX, shape.endY),
                strokeWidth = shape.strokeWidth
            )
            val angle = atan2(shape.endY - shape.startY, shape.endX - shape.startX)
            val headLen = 22f
            val p1 = Offset(
                shape.endX - headLen * cos(angle - Math.PI.toFloat() / 6f),
                shape.endY - headLen * sin(angle - Math.PI.toFloat() / 6f)
            )
            val p2 = Offset(
                shape.endX - headLen * cos(angle + Math.PI.toFloat() / 6f),
                shape.endY - headLen * sin(angle + Math.PI.toFloat() / 6f)
            )
            drawLine(strokeColor, Offset(shape.endX, shape.endY), p1, strokeWidth = shape.strokeWidth)
            drawLine(strokeColor, Offset(shape.endX, shape.endY), p2, strokeWidth = shape.strokeWidth)
        }
        ShapeType.DOUBLE_ARROW -> {
            drawLine(strokeColor, Offset(shape.startX, shape.startY), Offset(shape.endX, shape.endY), strokeWidth = shape.strokeWidth)
            val angle = atan2(shape.endY - shape.startY, shape.endX - shape.startX)
            val headLen = 20f
            drawLine(strokeColor, Offset(shape.endX, shape.endY), Offset(shape.endX - headLen * cos(angle - 0.5f), shape.endY - headLen * sin(angle - 0.5f)), strokeWidth = shape.strokeWidth)
            drawLine(strokeColor, Offset(shape.endX, shape.endY), Offset(shape.endX - headLen * cos(angle + 0.5f), shape.endY - headLen * sin(angle + 0.5f)), strokeWidth = shape.strokeWidth)
            drawLine(strokeColor, Offset(shape.startX, shape.startY), Offset(shape.startX + headLen * cos(angle - 0.5f), shape.startY + headLen * sin(angle - 0.5f)), strokeWidth = shape.strokeWidth)
            drawLine(strokeColor, Offset(shape.startX, shape.startY), Offset(shape.startX + headLen * cos(angle + 0.5f), shape.startY + headLen * sin(angle + 0.5f)), strokeWidth = shape.strokeWidth)
        }
        ShapeType.RECTANGLE -> {
            if (shape.isFilled) {
                drawRect(color = fillColor, topLeft = Offset(minX, minY), size = Size(w, h), style = Fill)
            }
            drawRect(color = strokeColor, topLeft = Offset(minX, minY), size = Size(w, h), style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.SQUARE -> {
            val side = maxOf(w, h)
            if (shape.isFilled) {
                drawRect(color = fillColor, topLeft = Offset(minX, minY), size = Size(side, side), style = Fill)
            }
            drawRect(color = strokeColor, topLeft = Offset(minX, minY), size = Size(side, side), style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.CIRCLE -> {
            val radius = maxOf(w, h) / 2f
            val center = Offset(minX + radius, minY + radius)
            if (shape.isFilled) {
                drawCircle(color = fillColor, radius = radius, center = center, style = Fill)
            }
            drawCircle(color = strokeColor, radius = radius, center = center, style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.ELLIPSE -> {
            if (shape.isFilled) {
                drawOval(color = fillColor, topLeft = Offset(minX, minY), size = Size(w, h), style = Fill)
            }
            drawOval(color = strokeColor, topLeft = Offset(minX, minY), size = Size(w, h), style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo((minX + maxX) / 2f, minY)
                lineTo(minX, maxY)
                lineTo(maxX, maxY)
                close()
            }
            if (shape.isFilled) {
                drawPath(path = path, color = fillColor, style = Fill)
            }
            drawPath(path = path, color = strokeColor, style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.PARALLELOGRAM -> {
            val skew = w * 0.25f
            val path = Path().apply {
                moveTo(minX + skew, minY)
                lineTo(maxX, minY)
                lineTo(maxX - skew, maxY)
                lineTo(minX, maxY)
                close()
            }
            if (shape.isFilled) {
                drawPath(path = path, color = fillColor, style = Fill)
            }
            drawPath(path = path, color = strokeColor, style = CanvasStroke(width = shape.strokeWidth))
        }
        ShapeType.COORDINATE_AXES -> {
            drawLine(strokeColor, Offset(minX, maxY), Offset(maxX, maxY), strokeWidth = shape.strokeWidth)
            drawLine(strokeColor, Offset(minX, maxY), Offset(minX, minY), strokeWidth = shape.strokeWidth)
            for (i in 1..5) {
                val tx = minX + i * (w / 5f)
                drawLine(strokeColor, Offset(tx, maxY - 6f), Offset(tx, maxY + 6f), strokeWidth = 2f)
                val ty = maxY - i * (h / 5f)
                drawLine(strokeColor, Offset(minX - 6f, ty), Offset(minX + 6f, ty), strokeWidth = 2f)
            }
        }
    }
}

private fun DrawScope.drawRulerOverlay(pos: PointF, angle: Float) {
    val rulerWidth = 600f
    val rulerHeight = 90f

    drawRect(
        color = Color(0x992563EB),
        topLeft = Offset(pos.x, pos.y),
        size = Size(rulerWidth, rulerHeight)
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(pos.x, pos.y),
        size = Size(rulerWidth, rulerHeight),
        style = CanvasStroke(width = 2f)
    )

    var cm = 0
    for (x in 0..rulerWidth.toInt() step 20) {
        val isMajor = (x % 60 == 0)
        val tickLen = if (isMajor) 24f else 12f
        drawLine(
            color = Color.White,
            start = Offset(pos.x + x, pos.y),
            end = Offset(pos.x + x, pos.y + tickLen),
            strokeWidth = if (isMajor) 2f else 1f
        )
        if (isMajor) cm++
    }
}

private fun DrawScope.drawProtractorOverlay(pos: PointF) {
    val radius = 220f
    val center = Offset(pos.x + radius, pos.y + radius)

    drawArc(
        color = Color(0x6610B981),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
    drawArc(
        color = Color.White,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = CanvasStroke(width = 2.5f)
    )

    for (deg in 0..180 step 10) {
        val rad = Math.toRadians((180 + deg).toDouble()).toFloat()
        val isMajor = deg % 30 == 0
        val tickLen = if (isMajor) 24f else 12f
        val startX = center.x + (radius - tickLen) * cos(rad)
        val startY = center.y + (radius - tickLen) * sin(rad)
        val endX = center.x + radius * cos(rad)
        val endY = center.y + radius * sin(rad)
        drawLine(Color.White, Offset(startX, startY), Offset(endX, endY), strokeWidth = if (isMajor) 2f else 1f)
    }
}

private fun DrawScope.drawCompassOverlay(pos: PointF, radius: Float) {
    val center = Offset(pos.x, pos.y)
    drawCircle(Color.Red, radius = 6f, center = center)
    drawCircle(
        color = Color(0x88F59E0B),
        radius = radius,
        center = center,
        style = CanvasStroke(width = 2f)
    )
    drawLine(
        color = Color(0xFFF59E0B),
        start = center,
        end = Offset(center.x + radius, center.y),
        strokeWidth = 3f
    )
    drawCircle(Color(0xFF38BDF8), radius = 7f, center = Offset(center.x + radius, center.y))
}

/**
 * WhiteboardView: Dedicated view component capturing strokes and gestures for digital ink conversion.
 */
@Composable
fun WhiteboardView(
    uiState: WhiteboardUiState,
    onAddStroke: (Stroke) -> Unit,
    onAddShape: (ShapeObject) -> Unit,
    onErase: (Float, Float) -> Unit,
    onUpdateRuler: (PointF, Float) -> Unit,
    onUpdateProtractor: (PointF) -> Unit,
    onUpdateCompass: (PointF, Float) -> Unit,
    onEditTextBlock: (TextBlockObject) -> Unit,
    onDeleteTextBlock: (String) -> Unit,
    onDeleteGraph: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    WhiteboardCanvas(
        uiState = uiState,
        onAddStroke = onAddStroke,
        onAddShape = onAddShape,
        onErase = onErase,
        onUpdateRuler = onUpdateRuler,
        onUpdateProtractor = onUpdateProtractor,
        onUpdateCompass = onUpdateCompass,
        onEditTextBlock = onEditTextBlock,
        onDeleteTextBlock = onDeleteTextBlock,
        onDeleteGraph = onDeleteGraph,
        modifier = modifier
    )
}

