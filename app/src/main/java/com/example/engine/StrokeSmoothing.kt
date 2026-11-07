package com.example.engine

import androidx.compose.ui.graphics.Path
import com.example.model.PointF

object StrokeSmoothing {

    /**
     * Converts a raw list of points into a smooth quadratic Bézier curve Path.
     */
    fun createSmoothPath(points: List<PointF>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points[0]
            path.moveTo(p.x, p.y)
            path.lineTo(p.x + 0.1f, p.y + 0.1f)
            return path
        }

        if (points.size == 2) {
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            path.quadraticTo(current.x, current.y, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.x, last.y)

        return path
    }

    /**
     * Resamples points to ensure even spacing for stroke analysis and smoothing.
     */
    fun resample(points: List<PointF>, interval: Float = 6f): List<PointF> {
        if (points.size <= 1) return points
        val resampled = mutableListOf<PointF>()
        resampled.add(points.first())
        var currentPoint = points.first()

        for (i in 1 until points.size) {
            val nextPoint = points[i]
            val dx = nextPoint.x - currentPoint.x
            val dy = nextPoint.y - currentPoint.y
            val dist = kotlin.math.hypot(dx, dy)

            if (dist >= interval) {
                val count = (dist / interval).toInt()
                for (j in 1..count) {
                    val t = (j * interval) / dist
                    val nx = currentPoint.x + dx * t
                    val ny = currentPoint.y + dy * t
                    val np = currentPoint.pressure + (nextPoint.pressure - currentPoint.pressure) * t
                    resampled.add(PointF(nx, ny, np))
                }
                currentPoint = nextPoint
            }
        }
        if (resampled.last() != points.last()) {
            resampled.add(points.last())
        }
        return resampled
    }
}
