package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.export.WhiteboardExporter
import kotlinx.coroutines.delay

@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allBoards by viewModel.allBoards.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeDialog by remember { mutableStateOf(false) }

    // Show status message snackbar
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Classroom stopwatch timer effect
    LaunchedEffect(uiState.isTimerRunning) {
        while (uiState.isTimerRunning) {
            delay(1000L)
            viewModel.setTimerSeconds(uiState.timerSeconds + 1)
        }
    }

    // Handle Android system back button when in presentation mode or dialogs
    BackHandler(enabled = uiState.isPresentationMode) {
        viewModel.togglePresentationMode()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Core Digital Whiteboard View (Ultra-smooth writing & gestures)
            WhiteboardView(
                uiState = uiState,
                onAddStroke = { viewModel.addStroke(it) },
                onAddShape = { viewModel.addShape(it) },
                onErase = { x, y -> viewModel.eraseAt(x, y) },
                onUpdateRuler = { pos, angle -> viewModel.updateRulerPos(pos, angle) },
                onUpdateProtractor = { pos -> viewModel.updateProtractorPos(pos) },
                onUpdateCompass = { pos, r -> viewModel.updateCompass(pos, r) },
                onEditTextBlock = { block -> viewModel.updateTextBlock(block.id, block.text) },
                onDeleteTextBlock = { id -> viewModel.deleteTextBlock(id) },
                onDeleteGraph = { id -> viewModel.deleteGraph(id) },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Whiteboard Top Navigation & Board Info (animated hide in presentation mode)
            AnimatedVisibility(
                visible = !uiState.isPresentationMode,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                WhiteboardTopBar(
                    uiState = uiState,
                    onToggleBoardManager = { viewModel.toggleBoardManager(true) },
                    onToggleExport = { viewModel.toggleExportDialog(true) },
                    onToggleThemeMenu = { showThemeDialog = true },
                    onTogglePresentation = { viewModel.togglePresentationMode() },
                    onToggleTimer = { viewModel.toggleTimer() },
                    onClearBoard = { viewModel.clearBoard() }
                )
            }

            // 3. Floating Modern Drawing Toolbar
            AnimatedVisibility(
                visible = !uiState.isPresentationMode,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingDrawingToolbar(
                    uiState = uiState,
                    onSetTool = { viewModel.setTool(it) },
                    onSetPenType = { viewModel.setPenType(it) },
                    onSetColor = { viewModel.setPenColor(it) },
                    onSetStrokeWidth = { viewModel.setStrokeWidth(it) },
                    onSetShapeType = { viewModel.setShapeType(it) },
                    onToggleAutoShape = { viewModel.toggleAutoShapeRecognition() },
                    onToggleStylusOnly = { viewModel.toggleStylusOnlyMode() },
                    onConvertHandwriting = { viewModel.triggerHandwritingRecognition() },
                    onOpenAiAssistant = { viewModel.toggleAiAssistant(true) },
                    onToggleRuler = { viewModel.toggleRuler() },
                    onToggleProtractor = { viewModel.toggleProtractor() },
                    onToggleCompass = { viewModel.toggleCompass() },
                    onOpenCalculator = { viewModel.toggleCalculator(true) },
                    onOpenPeriodicTable = { viewModel.togglePeriodicTable(true) },
                    onOpenFormulaLibrary = { viewModel.toggleFormulaLibrary(true) },
                    onOpenGraphGenerator = { viewModel.toggleGraphGenerator(true) },
                    onToggleAutoConvert = { viewModel.toggleAutoHandwritingConvert() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )
            }

            // Floating exit button when in Presentation Mode
            if (uiState.isPresentationMode) {
                FloatingActionButton(
                    onClick = { viewModel.togglePresentationMode() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .testTag("exit_presentation_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("Exit Presentation", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }

            // 4. Floating Classroom Timer Overlay
            if (uiState.showTimer) {
                ClassroomTimerOverlay(
                    seconds = uiState.timerSeconds,
                    isRunning = uiState.isTimerRunning,
                    onToggleRunning = { viewModel.setTimerRunning(it) },
                    onReset = { viewModel.setTimerSeconds(0) },
                    onClose = { viewModel.toggleTimer() },
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                )
            }

            // ---------------------------------------------------------
            // Modals & Bottom Sheets
            // ---------------------------------------------------------

            // AI Mathematics Assistant Sheet
            if (uiState.showAiAssistant) {
                AiMathAssistantSheet(
                    aiState = uiState.aiState,
                    onSolve = { viewModel.askAiToSolve(it) },
                    onExplain = { viewModel.askAiToExplain(it) },
                    onPractice = { viewModel.askAiForPractice(it) },
                    onCorrect = { viewModel.askAiToCorrectMistakes(it) },
                    onInsertSolution = { viewModel.insertAssistantSolutionToCanvas(it) },
                    onDismiss = { viewModel.toggleAiAssistant(false) }
                )
            }

            // Scientific Calculator Dialog
            if (uiState.showCalculator) {
                ScientificCalculatorDialog(
                    onInsertResult = { res ->
                        viewModel.addTextBlock(
                            com.example.model.TextBlockObject(
                                text = res,
                                x = 200f,
                                y = 300f,
                                color = uiState.theme.defaultPenHex,
                                isMath = true
                            )
                        )
                    },
                    onDismiss = { viewModel.toggleCalculator(false) }
                )
            }

            // Periodic Table Dialog
            if (uiState.showPeriodicTable) {
                PeriodicTableDialog(
                    onInsertElement = { elem ->
                        viewModel.addTextBlock(
                            com.example.model.TextBlockObject(
                                text = "${elem.number} ${elem.symbol} (${elem.name})\nMass: ${elem.mass} u",
                                x = 180f,
                                y = 240f,
                                color = uiState.theme.defaultPenHex,
                                isMath = false
                            )
                        )
                    },
                    onDismiss = { viewModel.togglePeriodicTable(false) }
                )
            }

            // Formula Library Dialog
            if (uiState.showFormulaLibrary) {
                FormulaLibraryDialog(
                    onSelectFormula = { formula -> viewModel.insertFormula(formula) },
                    onDismiss = { viewModel.toggleFormulaLibrary(false) }
                )
            }

            // Graph & Chart Generator Dialog
            if (uiState.showGraphGenerator) {
                GraphGeneratorDialog(
                    onInsertGraph = { graph -> viewModel.addGraph(graph) },
                    onDismiss = { viewModel.toggleGraphGenerator(false) }
                )
            }

            // Saved Boards & Notebooks Manager
            if (uiState.showBoardManager) {
                BoardManagerDialog(
                    boards = allBoards,
                    currentBoardId = uiState.currentBoardId,
                    onSelectBoard = { viewModel.loadBoard(it) },
                    onCreateNewBoard = { title, subject -> viewModel.createNewBoard(title, subject) },
                    onDeleteBoard = { id -> viewModel.deleteBoard(id) },
                    onDismiss = { viewModel.toggleBoardManager(false) }
                )
            }

            // Theme & Paper Pattern Picker
            if (showThemeDialog) {
                ThemePatternPickerDialog(
                    currentTheme = uiState.theme,
                    currentPattern = uiState.backgroundType,
                    onSelectTheme = { viewModel.setTheme(it) },
                    onSelectPattern = { viewModel.setBackgroundType(it) },
                    onDismiss = { showThemeDialog = false }
                )
            }

            // Export Dialog
            if (uiState.showExportDialog) {
                ExportDialog(
                    onExportPdf = {
                        viewModel.toggleExportDialog(false)
                        viewModel.exportAsPdf { file ->
                            if (file != null) {
                                WhiteboardExporter.shareFile(context, file, "application/pdf", "SmartBoard PDF Lesson")
                                Toast.makeText(context, "PDF Exported Successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportPng = {
                        viewModel.toggleExportDialog(false)
                        viewModel.exportAsImage(isPng = true) { file ->
                            if (file != null) {
                                WhiteboardExporter.shareFile(context, file, "image/png", "SmartBoard PNG Lesson")
                                Toast.makeText(context, "PNG Image Exported!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to export Image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportJpg = {
                        viewModel.toggleExportDialog(false)
                        viewModel.exportAsImage(isPng = false) { file ->
                            if (file != null) {
                                WhiteboardExporter.shareFile(context, file, "image/jpeg", "SmartBoard JPEG Lesson")
                                Toast.makeText(context, "JPEG Image Exported!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to export Image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDismiss = { viewModel.toggleExportDialog(false) }
                )
            }
        }
    }
}
