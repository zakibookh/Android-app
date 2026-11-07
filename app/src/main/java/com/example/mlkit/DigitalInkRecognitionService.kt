package com.example.mlkit

import android.util.Log
import com.example.model.Stroke
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "DigitalInkService"

/**
 * Service to capture handwritten strokes and convert them into text strings
 * asynchronously using Google ML Kit Digital Ink Recognition.
 */
class DigitalInkRecognitionService {

    private val remoteModelManager by lazy {
        try {
            RemoteModelManager.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "RemoteModelManager unavailable in current environment: ${e.message}")
            null
        }
    }
    private val recognizerCache = mutableMapOf<String, DigitalInkRecognizer>()
    private val modelCache = mutableMapOf<String, DigitalInkRecognitionModel>()

    data class InkResult(
        val topCandidate: String,
        val candidates: List<String> = emptyList(),
        val languageTag: String,
        val isMath: Boolean = false
    )

    /**
     * Converts application Stroke objects into an ML Kit Ink object.
     */
    fun buildInk(strokes: List<Stroke>): Ink {
        val inkBuilder = Ink.builder()
        for (stroke in strokes) {
            val strokeBuilder = Ink.Stroke.builder()
            for (pt in stroke.points) {
                strokeBuilder.addPoint(Ink.Point.create(pt.x, pt.y, pt.timestamp))
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }

    /**
     * Resolves the best DigitalInkRecognitionModelIdentifier for a given language tag.
     * Supports "en" (English), "so" (Somali via en-US or universal Latin), "ar" (Arabic),
     * and "zxx-Zsym-x-math" (Math notation).
     */
    fun getModelIdentifier(languageTag: String): DigitalInkRecognitionModelIdentifier? {
        val normalized = when (languageTag.lowercase().trim()) {
            "autodraw" -> "zxx-Zsym-x-autodraw"
            "shape", "shapes" -> "zxx-Zsym-x-shapes"
            "math", "xisaab" -> "en-US"
            "ar", "arabic", "carabi" -> "ar"
            "so", "somali" -> "so"
            "en", "english", "auto" -> "en-US"
            else -> languageTag
        }

        return try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(normalized)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve model for tag: $normalized, falling back to en-US", e)
            try {
                DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Ensures the ML Kit recognition model is downloaded and ready.
     */
    suspend fun ensureModelDownloaded(
        model: DigitalInkRecognitionModel,
        conditions: DownloadConditions = DownloadConditions.Builder().build()
    ): Boolean = withContext(Dispatchers.IO) {
        val manager = remoteModelManager ?: return@withContext false
        try {
            val isDownloadedTask = manager.isModelDownloaded(model)
            val isDownloaded = awaitTask(isDownloadedTask)
            if (isDownloaded) {
                return@withContext true
            }

            Log.d(TAG, "Downloading Digital Ink model: ${model.modelIdentifier.languageTag}")
            val downloadTask = manager.download(model, conditions)
            awaitTask(downloadTask)
            Log.d(TAG, "Digital Ink model downloaded successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Model download check failed: ${e.message}")
            false
        }
    }

    /**
     * Gets or creates a DigitalInkRecognizer for the specified language tag.
     */
    private fun getRecognizer(languageTag: String): Pair<DigitalInkRecognizer, DigitalInkRecognitionModel>? {
        val modelIdentifier = getModelIdentifier(languageTag) ?: return null
        val tag = modelIdentifier.languageTag

        val existingRecognizer = recognizerCache[tag]
        val existingModel = modelCache[tag]
        if (existingRecognizer != null && existingModel != null) {
            return Pair(existingRecognizer, existingModel)
        }

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val options = DigitalInkRecognizerOptions.builder(model).build()
        val recognizer = DigitalInkRecognition.getClient(options)

        recognizerCache[tag] = recognizer
        modelCache[tag] = model
        return Pair(recognizer, model)
    }

    /**
     * Asynchronously converts captured strokes into a recognized text string.
     */
    suspend fun recognizeStrokesAsync(
        strokes: List<Stroke>,
        languageTag: String = "en-US"
    ): Result<InkResult> = withContext(Dispatchers.Default) {
        if (strokes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Stroke list is empty"))
        }

        val ink = buildInk(strokes)
        if (ink.strokes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Ink contains no strokes"))
        }

        val pair = getRecognizer(languageTag)
            ?: return@withContext Result.failure(IllegalStateException("No ML Kit recognizer available for $languageTag"))
        val (recognizer, model) = pair

        // Attempt downloading/checking model in background
        ensureModelDownloaded(model)

        try {
            val recognitionResult: RecognitionResult = awaitTask(recognizer.recognize(ink))
            val candidates = recognitionResult.candidates.map { it.text }
            val topCandidate = candidates.firstOrNull().orEmpty()

            val isMath = model.modelIdentifier.languageTag.contains("math", ignoreCase = true)

            Result.success(
                InkResult(
                    topCandidate = topCandidate,
                    candidates = candidates,
                    languageTag = model.modelIdentifier.languageTag,
                    isMath = isMath
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit recognition failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Closes cached recognizers to release resources.
     */
    fun close() {
        recognizerCache.values.forEach { it.close() }
        recognizerCache.clear()
        modelCache.clear()
    }

    /**
     * Awaits a Play Services Task asynchronously using Kotlin coroutine continuation.
     */
    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }.addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }.addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
