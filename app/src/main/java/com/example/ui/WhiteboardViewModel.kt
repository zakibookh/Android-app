package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiHandwritingService
import com.example.ai.AiMathAssistantService
import com.example.ai.MathAssistantResponse
import com.example.data.WhiteboardDatabase
import com.example.data.WhiteboardRepository
import com.example.data.entity.WhiteboardEntity
import com.example.engine.ShapeRecognizer
import com.example.engine.StrokeSmoothing
import com.example.export.WhiteboardExporter
import com.example.math.FormulaItem
import com.example.mlkit.DigitalInkRecognitionService
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "WhiteboardViewModel"

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class RecognitionSuccess(val result: AiHandwritingService.RecognitionResult) : AiTaskState
    data class AssistantSuccess(val response: MathAssistantResponse) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

data class WhiteboardUiState(
    val currentBoardId: Long = 0L,
    val title: String = "Untitled Lesson",
    val subject: String = "Mathematics",
    val backgroundType: BackgroundType = BackgroundType.GRID,
    val theme: BoardTheme = BoardTheme.DARK_SLATE,
    val currentTool: ToolMode = ToolMode.PEN,
    val penType: PenType = PenType.BALLPOINT,
    val penColor: Long = 0xFF38BDF8L, // Cyan default
    val strokeWidth: Float = 5f,
    val selectedShapeType: ShapeType = ShapeType.RECTANGLE,
    val isShapeFilled: Boolean = false,
    val shapeFillColor: Long = 0x3338BDF8L,
    val autoShapeRecognition: Boolean = true,
    val stylusOnlyMode: Boolean = false,
    val recognitionLanguage: String = "Auto",
    val autoHandwritingConvert: Boolean = true,
    val isConvertingHandwriting: Boolean = false,
    val strokes: List<Stroke> = emptyList(),
    val shapes: List<ShapeObject> = emptyList(),
    val textBlocks: List<TextBlockObject> = emptyList(),
    val graphs: List<GraphObject> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // Overlays & Tools
    val showRuler: Boolean = false,
    val rulerAngle: Float = 0f,
    val rulerPos: PointF = PointF(200f, 400f),
    val showProtractor: Boolean = false,
    val protractorPos: PointF = PointF(300f, 400f),
    val showCompass: Boolean = false,
    val compassPos: PointF = PointF(300f, 400f),
    val compassRadius: Float = 120f,
    val showCalculator: Boolean = false,
    val showPeriodicTable: Boolean = false,
    val showFormulaLibrary: Boolean = false,
    val showGraphGenerator: Boolean = false,
    val showAiAssistant: Boolean = false,
    val showBoardManager: Boolean = false,
    val showExportDialog: Boolean = false,
    val showTimer: Boolean = false,
    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isPresentationMode: Boolean = false,
    val aiState: AiTaskState = AiTaskState.Idle,
    val statusMessage: String? = null
)

class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WhiteboardRepository
    private val digitalInkService = DigitalInkRecognitionService()
    private val handwritingService = AiHandwritingService()
    private val assistantService = AiMathAssistantService()

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<CanvasSnapshot>()
    private val redoStack = mutableListOf<CanvasSnapshot>()

    private var autoConvertJob: kotlinx.coroutines.Job? = null
    private val pendingStrokes = mutableListOf<Stroke>()

    private data class CanvasSnapshot(
        val strokes: List<Stroke>,
        val shapes: List<ShapeObject>,
        val textBlocks: List<TextBlockObject>,
        val graphs: List<GraphObject>
    )

    init {
        val db = WhiteboardDatabase.getDatabase(application)
        repository = WhiteboardRepository(db.whiteboardDao())
        loadInitialBoard()
    }

    val allBoards = repository.allBoards

    private fun loadInitialBoard() {
        viewModelScope.launch {
            repository.allBoards.collect { boards ->
                if (boards.isNotEmpty() && _uiState.value.currentBoardId == 0L) {
                    val first = boards.first()
                    loadBoard(first)
                } else if (boards.isEmpty() && _uiState.value.currentBoardId == 0L) {
                    createNewBoard("Calculus: Derivatives & Integrals", "Mathematics")
                }
            }
        }
    }

    private fun pushUndo() {
        undoStack.add(
            CanvasSnapshot(
                strokes = _uiState.value.strokes,
                shapes = _uiState.value.shapes,
                textBlocks = _uiState.value.textBlocks,
                graphs = _uiState.value.graphs
            )
        )
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = false
        )
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = CanvasSnapshot(
            strokes = _uiState.value.strokes,
            shapes = _uiState.value.shapes,
            textBlocks = _uiState.value.textBlocks,
            graphs = _uiState.value.graphs
        )
        redoStack.add(current)
        val prev = undoStack.removeAt(undoStack.size - 1)

        _uiState.value = _uiState.value.copy(
            strokes = prev.strokes,
            shapes = prev.shapes,
            textBlocks = prev.textBlocks,
            graphs = prev.graphs,
            canUndo = undoStack.isNotEmpty(),
            canRedo = true
        )
        autoSave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = CanvasSnapshot(
            strokes = _uiState.value.strokes,
            shapes = _uiState.value.shapes,
            textBlocks = _uiState.value.textBlocks,
            graphs = _uiState.value.graphs
        )
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.size - 1)

        _uiState.value = _uiState.value.copy(
            strokes = next.strokes,
            shapes = next.shapes,
            textBlocks = next.textBlocks,
            graphs = next.graphs,
            canUndo = true,
            canRedo = redoStack.isNotEmpty()
        )
        autoSave()
    }

    fun addStroke(stroke: Stroke) {
        pushUndo()

        // Automatic Shape Recognition if enabled
        if (_uiState.value.autoShapeRecognition && stroke.points.size > 8) {
            val recognized = ShapeRecognizer.recognizeShape(
                points = stroke.points,
                strokeColor = stroke.color,
                strokeWidth = stroke.strokeWidth
            )
            if (recognized != null && recognized.confidence >= 0.85f) {
                _uiState.value = _uiState.value.copy(
                    shapes = _uiState.value.shapes + recognized.shape,
                    statusMessage = "Snapped to ${recognized.shape.type.displayName}"
                )
                autoSave()
                return
            }
        }

        val updatedStrokes = _uiState.value.strokes + stroke
        _uiState.value = _uiState.value.copy(
            strokes = updatedStrokes
        )

        // Automatic handwriting-to-computer text conversion with 1.2s grouping debounce
        if (_uiState.value.autoHandwritingConvert) {
            pendingStrokes.add(stroke)
            autoConvertJob?.cancel()
            autoConvertJob = viewModelScope.launch {
                kotlinx.coroutines.delay(1200L)
                if (pendingStrokes.isNotEmpty()) {
                    val toConvert = pendingStrokes.toList()
                    pendingStrokes.clear()
                    _uiState.value = _uiState.value.copy(isConvertingHandwriting = true)
                    triggerHandwritingRecognition(toConvert)
                }
            }
        }

        autoSave()
    }

    fun addShape(shape: ShapeObject) {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            shapes = _uiState.value.shapes + shape
        )
        autoSave()
    }

    fun addTextBlock(textBlock: TextBlockObject) {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            textBlocks = _uiState.value.textBlocks + textBlock
        )
        autoSave()
    }

    fun updateTextBlock(id: String, newText: String) {
        pushUndo()
        val updated = _uiState.value.textBlocks.map {
            if (it.id == id) it.copy(text = newText) else it
        }
        _uiState.value = _uiState.value.copy(textBlocks = updated)
        autoSave()
    }

    fun deleteTextBlock(id: String) {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            textBlocks = _uiState.value.textBlocks.filterNot { it.id == id }
        )
        autoSave()
    }

    fun addGraph(graph: GraphObject) {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            graphs = _uiState.value.graphs + graph,
            showGraphGenerator = false
        )
        autoSave()
    }

    fun deleteGraph(id: String) {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            graphs = _uiState.value.graphs.filterNot { it.id == id }
        )
        autoSave()
    }

    fun eraseAt(x: Float, y: Float, radius: Float = 24f) {
        var modified = false
        // Filter strokes
        val remainingStrokes = _uiState.value.strokes.filterNot { stroke ->
            val hit = stroke.points.any { p ->
                kotlin.math.hypot(p.x - x, p.y - y) <= radius
            }
            if (hit) modified = true
            hit
        }
        // Filter shapes
        val remainingShapes = _uiState.value.shapes.filterNot { shape ->
            val cx = (shape.startX + shape.endX) / 2f
            val cy = (shape.startY + shape.endY) / 2f
            val hit = kotlin.math.hypot(cx - x, cy - y) <= (radius + 20f)
            if (hit) modified = true
            hit
        }
        if (modified) {
            pushUndo()
            _uiState.value = _uiState.value.copy(
                strokes = remainingStrokes,
                shapes = remainingShapes
            )
            autoSave()
        }
    }

    fun clearBoard() {
        pushUndo()
        _uiState.value = _uiState.value.copy(
            strokes = emptyList(),
            shapes = emptyList(),
            textBlocks = emptyList(),
            graphs = emptyList()
        )
        autoSave()
    }

    fun insertFormula(formula: FormulaItem) {
        pushUndo()
        val textBlock = TextBlockObject(
            text = formula.displayFormula,
            x = 180f,
            y = 260f,
            fontSize = 24f,
            color = _uiState.value.theme.defaultPenHex,
            isMath = true,
            latex = formula.latex
        )
        _uiState.value = _uiState.value.copy(
            textBlocks = _uiState.value.textBlocks + textBlock,
            showFormulaLibrary = false,
            statusMessage = "Inserted formula: ${formula.title}"
        )
        autoSave()
    }

    // --- Handwriting Recognition ---

    fun triggerHandwritingRecognition(strokesToRecognize: List<Stroke>? = null) {
        val targetStrokes = strokesToRecognize ?: _uiState.value.strokes
        if (targetStrokes.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "No handwriting to recognize.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiState = AiTaskState.Loading, isConvertingHandwriting = true)

            // Step 1: Capture strokes from WhiteboardView and convert via Google ML Kit Digital Ink Recognition asynchronously
            val mlKitResult = digitalInkService.recognizeStrokesAsync(
                strokes = targetStrokes,
                languageTag = _uiState.value.recognitionLanguage
            )

            var recognizedText: String? = null
            var isMathNotation = false

            mlKitResult.onSuccess { inkRes ->
                if (inkRes.topCandidate.isNotBlank()) {
                    recognizedText = inkRes.topCandidate
                    isMathNotation = inkRes.isMath
                    Log.d(TAG, "ML Kit Digital Ink recognized asynchronously: $recognizedText")
                }
            }.onFailure { e ->
                Log.w(TAG, "ML Kit Digital Ink recognition skipped or deferred: ${e.message}")
            }

            if (!recognizedText.isNullOrBlank()) {
                val bounds = calculateStrokeBounds(targetStrokes)
                val textBlock = TextBlockObject(
                    text = recognizedText!!,
                    x = bounds.minX,
                    y = bounds.minY,
                    fontSize = 24f,
                    color = _uiState.value.theme.defaultPenHex,
                    isMath = isMathNotation,
                    latex = if (isMathNotation) recognizedText!! else ""
                )
                pushUndo()
                val remainingStrokes = _uiState.value.strokes.filterNot { targetStrokes.contains(it) }
                _uiState.value = _uiState.value.copy(
                    strokes = remainingStrokes,
                    textBlocks = _uiState.value.textBlocks + textBlock,
                    isConvertingHandwriting = false,
                    aiState = AiTaskState.Idle,
                    statusMessage = "✓ ML Kit Digital Ink: \"$recognizedText\" loo beddelay Qoraal Computer!"
                )
                autoSave()
                return@launch
            }

            // Step 2: Fallback to Multimodal AI / heuristic handwriting recognition engine
            val result = handwritingService.recognizeStrokes(targetStrokes, _uiState.value.recognitionLanguage)
            result.onSuccess { rec ->
                _uiState.value = _uiState.value.copy(
                    aiState = AiTaskState.RecognitionSuccess(rec),
                    isConvertingHandwriting = false
                )
                val textBlock = TextBlockObject(
                    text = rec.text,
                    x = rec.boundsLeft,
                    y = rec.boundsTop,
                    fontSize = 24f,
                    color = _uiState.value.theme.defaultPenHex,
                    isMath = rec.isMath,
                    latex = rec.latex
                )
                pushUndo()
                // Replace recognized strokes with typed computer text block
                val remainingStrokes = _uiState.value.strokes.filterNot { targetStrokes.contains(it) }
                _uiState.value = _uiState.value.copy(
                    strokes = remainingStrokes,
                    textBlocks = _uiState.value.textBlocks + textBlock,
                    isConvertingHandwriting = false,
                    statusMessage = "✓ Qoraalka gacanta waxaa loo beddelay Qoraal Computer!"
                )
                autoSave()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    aiState = AiTaskState.Error(err.localizedMessage ?: "Aqoonsiga wuu guuldarreystay"),
                    isConvertingHandwriting = false
                )
            }
        }
    }

    fun toggleAutoHandwritingConvert() {
        val newState = !_uiState.value.autoHandwritingConvert
        _uiState.value = _uiState.value.copy(
            autoHandwritingConvert = newState,
            statusMessage = if (newState) "Auto Qoraal Computer: Shidan (ON)" else "Auto Qoraal Computer: Damiye (OFF - Qalin Caadi ah)"
        )
    }

    // --- AI Math Assistant Actions ---

    fun askAiToSolve(problem: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiState = AiTaskState.Loading)
            val res = assistantService.solveMathProblem(problem)
            res.onSuccess { response ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.AssistantSuccess(response))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.Error(e.localizedMessage ?: "Failed to solve"))
            }
        }
    }

    fun askAiToExplain(concept: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiState = AiTaskState.Loading)
            val res = assistantService.explainConcept(concept)
            res.onSuccess { response ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.AssistantSuccess(response))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.Error(e.localizedMessage ?: "Failed to explain"))
            }
        }
    }

    fun askAiForPractice(topic: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiState = AiTaskState.Loading)
            val res = assistantService.generatePracticeProblems(topic)
            res.onSuccess { response ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.AssistantSuccess(response))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.Error(e.localizedMessage ?: "Failed to generate exercises"))
            }
        }
    }

    fun askAiToCorrectMistakes(work: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aiState = AiTaskState.Loading)
            val res = assistantService.findAndCorrectMistakes(work)
            res.onSuccess { response ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.AssistantSuccess(response))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(aiState = AiTaskState.Error(e.localizedMessage ?: "Failed to check work"))
            }
        }
    }

    fun insertAssistantSolutionToCanvas(solution: MathAssistantResponse) {
        pushUndo()
        val formattedContent = buildString {
            appendLine("📐 ${solution.query}")
            appendLine("—".repeat(20))
            for (step in solution.steps) {
                appendLine("Step ${step.stepNumber}: ${step.title}")
                if (step.formulaLatex.isNotBlank()) appendLine("  ${step.formulaLatex}")
                appendLine("  ${step.explanation}")
            }
            if (solution.finalAnswer.isNotBlank()) {
                appendLine("Final Answer: ${solution.finalAnswer}")
            }
        }
        val textBlock = TextBlockObject(
            text = formattedContent,
            x = 100f,
            y = 150f,
            fontSize = 18f,
            color = _uiState.value.theme.defaultPenHex,
            isMath = true
        )
        _uiState.value = _uiState.value.copy(
            textBlocks = _uiState.value.textBlocks + textBlock,
            showAiAssistant = false,
            statusMessage = "Solution inserted to whiteboard."
        )
        autoSave()
    }

    // --- Tool Selection & UI Controls ---

    fun setTool(tool: ToolMode) {
        _uiState.value = _uiState.value.copy(currentTool = tool)
    }

    fun setPenType(type: PenType) {
        _uiState.value = _uiState.value.copy(
            penType = type,
            currentTool = ToolMode.PEN
        )
    }

    fun setPenColor(color: Long) {
        _uiState.value = _uiState.value.copy(penColor = color)
    }

    fun setStrokeWidth(width: Float) {
        _uiState.value = _uiState.value.copy(strokeWidth = width)
    }

    fun setShapeType(type: ShapeType) {
        _uiState.value = _uiState.value.copy(
            selectedShapeType = type,
            currentTool = ToolMode.SHAPE
        )
    }

    fun setShapeFilled(filled: Boolean) {
        _uiState.value = _uiState.value.copy(isShapeFilled = filled)
    }

    fun setBackgroundType(type: BackgroundType) {
        _uiState.value = _uiState.value.copy(backgroundType = type)
        autoSave()
    }

    fun setTheme(theme: BoardTheme) {
        _uiState.value = _uiState.value.copy(
            theme = theme,
            penColor = theme.defaultPenHex
        )
        autoSave()
    }

    fun toggleAutoShapeRecognition() {
        _uiState.value = _uiState.value.copy(autoShapeRecognition = !_uiState.value.autoShapeRecognition)
    }

    fun toggleStylusOnlyMode() {
        _uiState.value = _uiState.value.copy(stylusOnlyMode = !_uiState.value.stylusOnlyMode)
    }

    fun setRecognitionLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(recognitionLanguage = lang)
    }

    fun toggleRuler() {
        _uiState.value = _uiState.value.copy(showRuler = !_uiState.value.showRuler)
    }

    fun updateRulerPos(pos: PointF, angle: Float) {
        _uiState.value = _uiState.value.copy(rulerPos = pos, rulerAngle = angle)
    }

    fun toggleProtractor() {
        _uiState.value = _uiState.value.copy(showProtractor = !_uiState.value.showProtractor)
    }

    fun updateProtractorPos(pos: PointF) {
        _uiState.value = _uiState.value.copy(protractorPos = pos)
    }

    fun toggleCompass() {
        _uiState.value = _uiState.value.copy(showCompass = !_uiState.value.showCompass)
    }

    fun updateCompass(pos: PointF, radius: Float) {
        _uiState.value = _uiState.value.copy(compassPos = pos, compassRadius = radius)
    }

    fun toggleCalculator(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showCalculator = show ?: !_uiState.value.showCalculator)
    }

    fun togglePeriodicTable(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showPeriodicTable = show ?: !_uiState.value.showPeriodicTable)
    }

    fun toggleFormulaLibrary(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showFormulaLibrary = show ?: !_uiState.value.showFormulaLibrary)
    }

    fun toggleGraphGenerator(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showGraphGenerator = show ?: !_uiState.value.showGraphGenerator)
    }

    fun toggleAiAssistant(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showAiAssistant = show ?: !_uiState.value.showAiAssistant)
    }

    fun toggleBoardManager(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showBoardManager = show ?: !_uiState.value.showBoardManager)
    }

    fun toggleExportDialog(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(showExportDialog = show ?: !_uiState.value.showExportDialog)
    }

    fun togglePresentationMode() {
        _uiState.value = _uiState.value.copy(
            isPresentationMode = !_uiState.value.isPresentationMode,
            currentTool = if (!_uiState.value.isPresentationMode) ToolMode.LASER_POINTER else ToolMode.PEN
        )
    }

    fun toggleTimer() {
        _uiState.value = _uiState.value.copy(showTimer = !_uiState.value.showTimer)
    }

    fun setTimerRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isTimerRunning = running)
    }

    fun setTimerSeconds(secs: Int) {
        _uiState.value = _uiState.value.copy(timerSeconds = secs)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    // --- Board Persistence & Management ---

    fun createNewBoard(title: String = "New Board", subject: String = "Mathematics") {
        viewModelScope.launch {
            val id = repository.saveBoard(
                id = 0L,
                title = title,
                subject = subject,
                backgroundType = _uiState.value.backgroundType,
                theme = _uiState.value.theme,
                strokes = emptyList(),
                shapes = emptyList(),
                textBlocks = emptyList(),
                graphs = emptyList()
            )
            undoStack.clear()
            redoStack.clear()
            _uiState.value = _uiState.value.copy(
                currentBoardId = id,
                title = title,
                subject = subject,
                strokes = emptyList(),
                shapes = emptyList(),
                textBlocks = emptyList(),
                graphs = emptyList(),
                canUndo = false,
                canRedo = false,
                showBoardManager = false,
                statusMessage = "Created new board: $title"
            )
        }
    }

    fun loadBoard(entity: WhiteboardEntity) {
        val strokes = repository.deserializeStrokes(entity.strokesJson)
        val shapes = repository.deserializeShapes(entity.shapesJson)
        val textBlocks = repository.deserializeTextBlocks(entity.textBlocksJson)
        val graphs = repository.deserializeGraphs(entity.graphsJson)
        val bgType = try { BackgroundType.valueOf(entity.backgroundType) } catch (_: Exception) { BackgroundType.GRID }
        val theme = try { BoardTheme.valueOf(entity.themeName) } catch (_: Exception) { BoardTheme.DARK_SLATE }

        undoStack.clear()
        redoStack.clear()

        _uiState.value = _uiState.value.copy(
            currentBoardId = entity.id,
            title = entity.title,
            subject = entity.subject,
            backgroundType = bgType,
            theme = theme,
            strokes = strokes,
            shapes = shapes,
            textBlocks = textBlocks,
            graphs = graphs,
            canUndo = false,
            canRedo = false,
            showBoardManager = false,
            statusMessage = "Loaded: ${entity.title}"
        )
    }

    fun deleteBoard(id: Long) {
        viewModelScope.launch {
            repository.deleteBoard(id)
            if (_uiState.value.currentBoardId == id) {
                createNewBoard()
            }
        }
    }

    private fun autoSave() {
        val state = _uiState.value
        viewModelScope.launch {
            val savedId = repository.saveBoard(
                id = state.currentBoardId,
                title = state.title,
                subject = state.subject,
                backgroundType = state.backgroundType,
                theme = state.theme,
                strokes = state.strokes,
                shapes = state.shapes,
                textBlocks = state.textBlocks,
                graphs = state.graphs
            )
            if (state.currentBoardId == 0L) {
                _uiState.value = _uiState.value.copy(currentBoardId = savedId)
            }
        }
    }

    // --- Export Actions ---

    fun exportAsPdf(onComplete: (File?) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val file = WhiteboardExporter.exportToPdf(
                context = getApplication(),
                title = state.title,
                strokes = state.strokes,
                shapes = state.shapes,
                textBlocks = state.textBlocks,
                theme = state.theme
            )
            onComplete(file)
        }
    }

    fun exportAsImage(isPng: Boolean, onComplete: (File?) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            val file = WhiteboardExporter.exportToImage(
                context = getApplication(),
                title = state.title,
                strokes = state.strokes,
                shapes = state.shapes,
                textBlocks = state.textBlocks,
                theme = state.theme,
                isPng = isPng
            )
            onComplete(file)
        }
    }

    private data class StrokeBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

    private fun calculateStrokeBounds(strokes: List<Stroke>): StrokeBounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (s in strokes) {
            for (p in s.points) {
                minX = kotlin.math.min(minX, p.x)
                minY = kotlin.math.min(minY, p.y)
                maxX = kotlin.math.max(maxX, p.x)
                maxY = kotlin.math.max(maxY, p.y)
            }
        }
        if (minX == Float.MAX_VALUE) {
            minX = 100f
            minY = 100f
            maxX = 300f
            maxY = 200f
        }
        return StrokeBounds(minX, minY, maxX, maxY)
    }

    override fun onCleared() {
        super.onCleared()
        digitalInkService.close()
    }
}

