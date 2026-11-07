package com.example.engine

import com.example.model.PointF
import com.example.model.ShapeObject
import com.example.model.ShapeType
import kotlin.math.*

object ShapeRecognizer {

    data class RecognitionResult(
        val shape: ShapeObject,
        val confidence: Float
    )

    fun recognizeShape(
        points: List<PointF>,
        strokeColor: Long,
        strokeWidth: Float
    ): RecognitionResult? {
        if (points.size < 6) return null

        val resampled = StrokeSmoothing.resample(points, 8f)
        if (resampled.size < 5) return null

        val start = resampled.first()
        val end = resampled.last()
        val startToEndDist = hypot(end.x - start.x, end.y - start.y)

        var totalPathLength = 0f
        for (i in 0 until resampled.size - 1) {
            totalPathLength += hypot(resampled[i + 1].x - resampled[i].x, resampled[i + 1].y - resampled[i].y)
        }
        if (totalPathLength <= 10f) return null

        val isClosed = (startToEndDist / totalPathLength) < 0.22f || startToEndDist < 35f

        // 1. Check for Straight Line / Arrow
        if (!isClosed && (startToEndDist / totalPathLength) > 0.88f) {
            // Check maximum deviation from the line segment start-end
            var maxDeviation = 0f
            for (p in resampled) {
                val dev = pointToLineDistance(p, start, end)
                if (dev > maxDeviation) maxDeviation = dev
            }

            if (maxDeviation < 24f || (maxDeviation / startToEndDist) < 0.12f) {
                // Snap nearly horizontal or vertical lines
                val dx = abs(end.x - start.x)
                val dy = abs(end.y - start.y)
                var finalEndX = end.x
                var finalEndY = end.y

                if (dy < 18f || (dy / (dx + 0.1f)) < 0.08f) {
                    finalEndY = start.y // horizontal snap
                } else if (dx < 18f || (dx / (dy + 0.1f)) < 0.08f) {
                    finalEndX = start.x // vertical snap
                }

                return RecognitionResult(
                    shape = ShapeObject(
                        type = ShapeType.LINE,
                        startX = start.x,
                        startY = start.y,
                        endX = finalEndX,
                        endY = finalEndY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.95f
                )
            }
        }

        // Calculate bounding box and centroid
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var sumX = 0f
        var sumY = 0f

        for (p in resampled) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            sumX += p.x
            sumY += p.y
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        if (boxWidth < 15f && boxHeight < 15f) return null

        val centerX = sumX / resampled.size
        val centerY = sumY / resampled.size

        // 2. Closed Shapes: Circle, Ellipse, Rectangle, Triangle
        if (isClosed) {
            // Check circle/ellipse fit
            val radii = resampled.map { hypot(it.x - centerX, it.y - centerY) }
            val avgRadius = radii.average().toFloat()
            val variance = radii.map { (it - avgRadius).toDouble().pow(2) }.average()
            val stdDev = sqrt(variance).toFloat()
            val coefficientOfVariation = stdDev / (avgRadius + 0.1f)

            val aspectRatio = boxWidth / (boxHeight + 0.001f)

            if (coefficientOfVariation < 0.18f && abs(1f - aspectRatio) < 0.25f) {
                // High confidence Circle
                val radius = (boxWidth + boxHeight) / 4f
                return RecognitionResult(
                    shape = ShapeObject(
                        type = ShapeType.CIRCLE,
                        startX = centerX - radius,
                        startY = centerY - radius,
                        endX = centerX + radius,
                        endY = centerY + radius,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.94f
                )
            }

            if (coefficientOfVariation < 0.25f) {
                // Ellipse
                return RecognitionResult(
                    shape = ShapeObject(
                        type = ShapeType.ELLIPSE,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.88f
                )
            }

            // Polygon corner analysis
            val corners = detectCorners(resampled)
            if (corners.size == 3) {
                return RecognitionResult(
                    shape = ShapeObject(
                        type = ShapeType.TRIANGLE,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.86f
                )
            }

            if (corners.size in 4..5) {
                val isSquare = abs(1f - aspectRatio) < 0.15f
                return RecognitionResult(
                    shape = ShapeObject(
                        type = if (isSquare) ShapeType.SQUARE else ShapeType.RECTANGLE,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.90f
                )
            }

            // Fallback for general rectangle if bounding box area roughly matches stroke area
            val boxArea = boxWidth * boxHeight
            if (boxArea > 100f) {
                val isSquare = abs(1f - aspectRatio) < 0.18f
                return RecognitionResult(
                    shape = ShapeObject(
                        type = if (isSquare) ShapeType.SQUARE else ShapeType.RECTANGLE,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth
                    ),
                    confidence = 0.80f
                )
            }
        } else {
            // Coordinate Axes detection: L-shape
            val corners = detectCorners(resampled)
            if (corners.size == 1) {
                val corner = corners.first()
                val isCornerNearOrigin = abs(corner.x - minX) < 30f && abs(corner.y - maxY) < 30f
                if (isCornerNearOrigin && boxWidth > 60f && boxHeight > 60f) {
                    return RecognitionResult(
                        shape = ShapeObject(
                            type = ShapeType.COORDINATE_AXES,
                            startX = minX,
                            startY = minY,
                            endX = maxX,
                            endY = maxY,
                            strokeColor = strokeColor,
                            strokeWidth = strokeWidth
                        ),
                        confidence = 0.88f
                    )
                }
            }
        }

        return null
    }

    private fun pointToLineDistance(p: PointF, a: PointF, b: PointF): Float {
        val l2 = (b.x - a.x).pow(2) + (b.y - a.y).pow(2)
        if (l2 == 0f) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
        val projX = a.x + t * (b.x - a.x)
        val projY = a.y + t * (b.y - a.y)
        return hypot(p.x - projX, p.y - projY)
    }

    private fun detectCorners(points: List<PointF>): List<PointF> {
        val corners = mutableListOf<PointF>()
        val step = 4
        if (points.size < step * 2 + 1) return corners

        for (i in step until points.size - step) {
            val pPrev = points[i - step]
            val pCurr = points[i]
            val pNext = points[i + step]

            val v1x = pPrev.x - pCurr.x
            val v1y = pPrev.y - pCurr.y
            val v2x = pNext.x - pCurr.x
            val v2y = pNext.y - pCurr.y

            val dot = v1x * v2x + v1y * v2y
            val mag1 = hypot(v1x, v1y)
            val mag2 = hypot(v2x, v2y)

            if (mag1 > 0f && mag2 > 0f) {
                val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
                val angleDeg = acos(cosTheta) * (180f / Math.PI.toFloat())
                // Sharp angle corner between 35 and 140 degrees
                if (angleDeg in 35f..140f) {
                    if (corners.isEmpty() || hypot(pCurr.x - corners.last().x, pCurr.y - corners.last().y) > 30f) {
                        corners.add(pCurr)
                    }
                }
            }
        }
        return corners
    }
}
