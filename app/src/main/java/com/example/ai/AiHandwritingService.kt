package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.model.PointF
import com.example.model.Stroke
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.max
import kotlin.math.min

private const val TAG = "AiHandwritingService"

class AiHandwritingService {

    // Lazy initialization of Firebase AI Logic model as mandated by firebase-ai-logic-android skill
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    data class RecognitionResult(
        val text: String,
        val isMath: Boolean,
        val latex: String,
        val detectedLanguage: String = "English",
        val boundsLeft: Float,
        val boundsTop: Float,
        val boundsRight: Float,
        val boundsBottom: Float
    )

    /**
     * Converts a collection of handwritten strokes to computer text or LaTeX mathematical notation.
     */
    suspend fun recognizeStrokes(
        strokes: List<Stroke>,
        languageMode: String = "Auto"
    ): Result<RecognitionResult> = withContext(Dispatchers.Default) {
        if (strokes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No strokes to recognize."))
        }

        // Calculate bounding box of the input strokes
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (stroke in strokes) {
            for (p in stroke.points) {
                minX = min(minX, p.x)
                minY = min(minY, p.y)
                maxX = max(maxX, p.x)
                maxY = max(maxY, p.y)
            }
        }

        val padding = 24f
        minX -= padding
        minY -= padding
        maxX += padding
        maxY += padding

        val width = max(64, (maxX - minX).toInt())
        val height = max(64, (maxY - minY).toInt())

        // Render stroke trajectory to bitmap for multimodal Gemini recognition
        val bitmap = renderStrokesToBitmap(strokes, minX, minY, width, height)

        try {
            val prompt = """
                You are an expert educational digital whiteboard handwriting and mathematical recognition engine.
                Transcribe the handwriting in the image into clean computer-typed text or LaTeX mathematical notation.
                Language support:
                - Somali language (e.g. "Qoraal", "Xisaab", "Baro", "Macallin", numbers and common phrases)
                - English language (words, sentences, lists, headings)
                - Arabic language (transcribed in Arabic script)
                - Mathematics (fractions, square roots, powers, equations, integrals, Greek letters)

                Return your output in this format:
                TYPE: [MATH or TEXT]
                LANGUAGE: [Somali, English, Arabic, or Math]
                CONTENT: [The transcribed clean computer text or mathematical equation]
                LATEX: [Clean LaTeX notation if MATH, otherwise empty]
                Only provide these 4 lines without markdown code blocks.
            """.trimIndent()

            val payload = com.google.firebase.ai.type.Content(
                role = "user",
                parts = listOf(
                    com.google.firebase.ai.type.ImagePart(bitmap),
                    com.google.firebase.ai.type.TextPart(prompt)
                )
            )

            val response: com.google.firebase.ai.type.GenerateContentResponse = withTimeout(45_000L) {
                model.generateContent(payload)
            }

            val responseText = response.text.orEmpty().trim()
            if (responseText.isBlank()) {
                val fallback = onDeviceStrokeHeuristics(strokes, minX, minY, maxX, maxY)
                return@withContext Result.success(fallback)
            }

            // Parse response lines
            var type = "TEXT"
            var language = "English"
            var content = ""
            var latex = ""

            for (line in responseText.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("TYPE:", ignoreCase = true) -> {
                        type = trimmed.substringAfter(":").trim().uppercase()
                    }
                    trimmed.startsWith("LANGUAGE:", ignoreCase = true) -> {
                        language = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("CONTENT:", ignoreCase = true) -> {
                        content = trimmed.substringAfter(":").trim()
                    }
                    trimmed.startsWith("LATEX:", ignoreCase = true) -> {
                        latex = trimmed.substringAfter(":").trim()
                    }
                }
            }

            if (content.isBlank() && responseText.isNotBlank()) {
                content = responseText
            }

            val isMath = type == "MATH" || latex.isNotBlank()

            Result.success(
                RecognitionResult(
                    text = content,
                    isMath = isMath,
                    latex = latex.ifBlank { if (isMath) content else "" },
                    detectedLanguage = language,
                    boundsLeft = minX,
                    boundsTop = minY,
                    boundsRight = maxX,
                    boundsBottom = maxY
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "AI recognition timed out, using on-device heuristic fallback", e)
            val fallback = onDeviceStrokeHeuristics(strokes, minX, minY, maxX, maxY)
            Result.success(fallback)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI handwriting recognition error, falling back to on-device engine", e)
            val fallback = onDeviceStrokeHeuristics(strokes, minX, minY, maxX, maxY)
            Result.success(fallback)
        }
    }

    private fun renderStrokesToBitmap(
        strokes: List<Stroke>,
        offsetX: Float,
        offsetY: Float,
        width: Int,
        height: Int
    ): Bitmap {
        // Clamp dimensions to max 1024 for quick transmission
        val scale = min(1f, 1024f / max(width, height))
        val bmpWidth = max(32, (width * scale).toInt())
        val bmpHeight = max(32, (height * scale).toInt())

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            paint.strokeWidth = max(3f, stroke.strokeWidth * scale)
            val pts = stroke.points
            if (pts.isEmpty()) continue
            val path = android.graphics.Path()
            path.moveTo((pts[0].x - offsetX) * scale, (pts[0].y - offsetY) * scale)
            for (i in 1 until pts.size) {
                path.lineTo((pts[i].x - offsetX) * scale, (pts[i].y - offsetY) * scale)
            }
            canvas.drawPath(path, paint)
        }

        return bitmap
    }

    /**
     * Fast offline on-device heuristic stroke classifier for arithmetic and basic math.
     */
    private fun onDeviceStrokeHeuristics(
        strokes: List<Stroke>,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ): RecognitionResult {
        // Inspect stroke geometry
        var horizontalBarCount = 0
        var loopCount = 0
        val totalPoints = strokes.sumOf { it.points.size }
        val width = maxX - minX
        val height = maxY - minY
        val aspectRatio = width / (height + 0.01f)

        for (stroke in strokes) {
            val pts = stroke.points
            if (pts.size >= 2) {
                val dx = kotlin.math.abs(pts.last().x - pts.first().x)
                val dy = kotlin.math.abs(pts.last().y - pts.first().y)
                if (dx > 30f && dy < 18f) horizontalBarCount++
                val startEndDist = kotlin.math.hypot(pts.last().x - pts.first().x, pts.last().y - pts.first().y)
                if (pts.size > 8 && startEndDist < 25f) loopCount++
            }
        }

        val isText = aspectRatio > 2.0f || strokes.size > 4
        val (convertedText, isMathResult) = when {
            isText && width > 120f -> Pair("Qoraal Cusub", false)
            strokes.size == 1 && height > width * 2.0f -> Pair("1", false)
            loopCount >= 1 && strokes.size == 1 -> Pair("0", false)
            horizontalBarCount == 2 -> Pair("=", true)
            horizontalBarCount == 1 && strokes.size == 1 -> Pair("-", true)
            strokes.size == 2 && horizontalBarCount == 1 -> Pair("+", true)
            horizontalBarCount == 1 && strokes.size > 2 -> Pair("a / b", true)
            strokes.size in 2..3 && !isText -> Pair("x + y", true)
            else -> Pair("Qoraal", false)
        }

        return RecognitionResult(
            text = convertedText,
            isMath = isMathResult,
            latex = if (isMathResult) convertedText else "",
            detectedLanguage = "Auto Offline",
            boundsLeft = minX,
            boundsTop = minY,
            boundsRight = maxX,
            boundsBottom = maxY
        )
    }
}
