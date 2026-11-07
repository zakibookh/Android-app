package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val TAG = "WhiteboardExporter"

object WhiteboardExporter {

    /**
     * Renders whiteboard canvas to high-resolution Bitmap.
     */
    fun renderToBitmap(
        strokes: List<Stroke>,
        shapes: List<ShapeObject>,
        textBlocks: List<TextBlockObject>,
        theme: BoardTheme,
        width: Int = 1920,
        height: Int = 1080
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(theme.bgHex.toInt())

        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Draw shapes
        for (shape in shapes) {
            strokePaint.color = shape.strokeColor.toInt()
            strokePaint.strokeWidth = shape.strokeWidth
            when (shape.type) {
                ShapeType.LINE -> canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, strokePaint)
                ShapeType.RECTANGLE, ShapeType.SQUARE -> {
                    val l = minOf(shape.startX, shape.endX)
                    val t = minOf(shape.startY, shape.endY)
                    val r = maxOf(shape.startX, shape.endX)
                    val b = maxOf(shape.startY, shape.endY)
                    canvas.drawRect(l, t, r, b, strokePaint)
                }
                ShapeType.CIRCLE, ShapeType.ELLIPSE -> {
                    val l = minOf(shape.startX, shape.endX)
                    val t = minOf(shape.startY, shape.endY)
                    val r = maxOf(shape.startX, shape.endX)
                    val b = maxOf(shape.startY, shape.endY)
                    canvas.drawOval(l, t, r, b, strokePaint)
                }
                ShapeType.TRIANGLE -> {
                    val path = android.graphics.Path()
                    path.moveTo((shape.startX + shape.endX) / 2f, shape.startY)
                    path.lineTo(shape.startX, shape.endY)
                    path.lineTo(shape.endX, shape.endY)
                    path.close()
                    canvas.drawPath(path, strokePaint)
                }
                else -> canvas.drawLine(shape.startX, shape.startY, shape.endX, shape.endY, strokePaint)
            }
        }

        // Draw strokes
        for (stroke in strokes) {
            val color = if (stroke.isHighlighter) {
                val base = stroke.color.toInt()
                Color.argb(90, Color.red(base), Color.green(base), Color.blue(base))
            } else {
                stroke.color.toInt()
            }
            strokePaint.color = color
            strokePaint.strokeWidth = stroke.strokeWidth

            val pts = stroke.points
            if (pts.isEmpty()) continue
            val path = android.graphics.Path()
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                path.lineTo(pts[i].x, pts[i].y)
            }
            canvas.drawPath(path, strokePaint)
        }

        // Draw text blocks
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 28f
        }
        for (textBlock in textBlocks) {
            textPaint.color = textBlock.color.toInt()
            textPaint.textSize = max(18f, textBlock.fontSize * 1.5f)
            canvas.drawText(textBlock.text, textBlock.x, textBlock.y, textPaint)
        }

        return bitmap
    }

    /**
     * Exports canvas to PDF file and returns File Uri.
     */
    fun exportToPdf(
        context: Context,
        title: String,
        strokes: List<Stroke>,
        shapes: List<ShapeObject>,
        textBlocks: List<TextBlockObject>,
        theme: BoardTheme
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 1920
            val pageHeight = 1080

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val bitmap = renderToBitmap(strokes, shapes, textBlocks, theme, pageWidth, pageHeight)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdfDocument.finishPage(page)

            val safeTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "SmartBoard" }
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "${safeTitle}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF", e)
            null
        }
    }

    /**
     * Exports canvas as PNG image file.
     */
    fun exportToImage(
        context: Context,
        title: String,
        strokes: List<Stroke>,
        shapes: List<ShapeObject>,
        textBlocks: List<TextBlockObject>,
        theme: BoardTheme,
        isPng: Boolean = true
    ): File? {
        return try {
            val bitmap = renderToBitmap(strokes, shapes, textBlocks, theme)
            val safeTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "Whiteboard" }
            val ext = if (isPng) "png" else "jpg"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "${safeTitle}_${System.currentTimeMillis()}.$ext")
            val fos = FileOutputStream(file)
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            bitmap.compress(format, 95, fos)
            fos.flush()
            fos.close()
            bitmap.recycle()

            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export Image", e)
            null
        }
    }

    /**
     * Shares exported file with students or other apps using Intent.ACTION_SEND.
     */
    fun shareFile(context: Context, file: File, mimeType: String, subject: String = "Whiteboard Lesson") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Whiteboard"))
        } catch (_: Exception) {
            // Direct share fallback
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, "Exported from SmartBoard AI: ${file.name}")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Whiteboard"))
        }
    }
}
