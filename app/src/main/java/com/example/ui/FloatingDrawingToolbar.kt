package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun WhiteboardTopBar(
    uiState: WhiteboardUiState,
    onToggleBoardManager: () -> Unit,
    onToggleExport: () -> Unit,
    onToggleThemeMenu: () -> Unit,
    onTogglePresentation: () -> Unit,
    onToggleTimer: () -> Unit,
    onClearBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("whiteboard_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Board Title & Notebooks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleBoardManager() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Notebooks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${uiState.subject} • ${uiState.theme.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Presentation, Timer, Export, Themes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Presentation Mode
                IconButton(
                    onClick = onTogglePresentation,
                    modifier = Modifier.testTag("presentation_mode_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isPresentationMode) Icons.Default.FullscreenExit else Icons.Default.CoPresent,
                        contentDescription = "Presentation Mode",
                        tint = if (uiState.isPresentationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Classroom Timer
                IconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier.testTag("classroom_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Classroom Timer",
                        tint = if (uiState.showTimer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Theme / Background Picker
                IconButton(
                    onClick = onToggleThemeMenu,
                    modifier = Modifier.testTag("theme_picker_button")
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Themes & Grid")
                }

                // Clear Board
                IconButton(
                    onClick = onClearBoard,
                    modifier = Modifier.testTag("clear_board_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Board")
                }

                // Export / Share
                Button(
                    onClick = onToggleExport,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun FloatingDrawingToolbar(
    uiState: WhiteboardUiState,
    onSetTool: (ToolMode) -> Unit,
    onSetPenType: (PenType) -> Unit,
    onSetColor: (Long) -> Unit,
    onSetStrokeWidth: (Float) -> Unit,
    onSetShapeType: (ShapeType) -> Unit,
    onToggleAutoShape: () -> Unit,
    onToggleStylusOnly: () -> Unit,
    onConvertHandwriting: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onToggleRuler: () -> Unit,
    onToggleProtractor: () -> Unit,
    onToggleCompass: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenPeriodicTable: () -> Unit,
    onOpenFormulaLibrary: () -> Unit,
    onOpenGraphGenerator: () -> Unit,
    onToggleAutoConvert: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPenMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showWidthMenu by remember { mutableStateOf(false) }
    var showShapeMenu by remember { mutableStateOf(false) }
    var showToolsMenu by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFFFFFFFFL, // White
        0xFF38BDF8L, // Bright Cyan
        0xFF34D399L, // Emerald Green
        0xFFFBBF24L, // Amber Yellow
        0xFFF87171L, // Coral Red
        0xFFA78BFAL, // Violet
        0xFFF472B6L, // Pink
        0xFFE2E8F0L  // Chalk Light Grey
    )

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .testTag("floating_drawing_toolbar")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Secondary expanded row (Color picker / Width slider / Shape picker)
            AnimatedVisibility(visible = showColorMenu) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .border(
                                    width = if (uiState.penColor == col) 3.dp else 1.dp,
                                    color = if (uiState.penColor == col) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSetColor(col)
                                    showColorMenu = false
                                }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showWidthMenu) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                        .widthIn(max = 260.dp)
                ) {
                    Text("Thickness:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = uiState.strokeWidth,
                        onValueChange = onSetStrokeWidth,
                        valueRange = 2f..36f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size((uiState.strokeWidth).coerceIn(4f, 28f).dp)
                            .clip(CircleShape)
                            .background(Color(uiState.penColor))
                    )
                }
            }

            AnimatedVisibility(visible = showPenMenu) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PenType.values().filterNot { it == PenType.ERASER }.forEach { pt ->
                        FilterChip(
                            selected = uiState.penType == pt && uiState.currentTool == ToolMode.PEN,
                            onClick = {
                                onSetPenType(pt)
                                showPenMenu = false
                            },
                            label = { Text(pt.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showShapeMenu) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShapeType.values().forEach { st ->
                        FilterChip(
                            selected = uiState.selectedShapeType == st && uiState.currentTool == ToolMode.SHAPE,
                            onClick = {
                                onSetShapeType(st)
                                showShapeMenu = false
                            },
                            label = { Text(st.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Primary toolbar icon row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // Undo / Redo
                IconButton(
                    onClick = onUndo,
                    enabled = uiState.canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }

                IconButton(
                    onClick = onRedo,
                    enabled = uiState.canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

                // Qalin Arrow / Chisel Nib Pen (Beautiful Calligraphy Assist)
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.ARROW_NIB,
                    onCheckedChange = {
                        onSetPenType(PenType.ARROW_NIB)
                        showPenMenu = !showPenMenu
                    },
                    modifier = Modifier.testTag("pen_arrow_nib_button")
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Qalin Arrow / Chisel Nib (Qoraal Qurxiye)")
                }

                // Qalin Calligraphy / Jaba
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.CALLIGRAPHY,
                    onCheckedChange = {
                        onSetPenType(PenType.CALLIGRAPHY)
                        showPenMenu = !showPenMenu
                    },
                    modifier = Modifier.testTag("pen_calligraphy_button")
                ) {
                    Icon(Icons.Default.Brush, contentDescription = "Qalin Calligraphy / Jaba")
                }

                // Qalin Fountain / Assist
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.FOUNTAIN,
                    onCheckedChange = {
                        onSetPenType(PenType.FOUNTAIN)
                        showPenMenu = !showPenMenu
                    },
                    modifier = Modifier.testTag("pen_fountain_button")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "Qalin Fountain Assist")
                }

                // Ballpoint Pen
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.BALLPOINT,
                    onCheckedChange = { onSetPenType(PenType.BALLPOINT) },
                    modifier = Modifier.testTag("pen_ballpoint_button")
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Ballpoint Pen")
                }

                // Neon Glow Pen
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.NEON,
                    onCheckedChange = { onSetPenType(PenType.NEON) },
                    modifier = Modifier.testTag("pen_neon_button")
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Neon Pen")
                }

                // Highlighter
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.PEN && uiState.penType == PenType.HIGHLIGHTER,
                    onCheckedChange = { onSetPenType(PenType.HIGHLIGHTER) },
                    modifier = Modifier.testTag("pen_highlighter_button")
                ) {
                    Icon(Icons.Default.Highlight, contentDescription = "Highlighter")
                }

                // Eraser
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.ERASER,
                    onCheckedChange = { onSetTool(ToolMode.ERASER) },
                    modifier = Modifier.testTag("eraser_button")
                ) {
                    Icon(Icons.Default.AutoFixNormal, contentDescription = "Eraser")
                }

                // Active Color indicator & picker button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color(uiState.penColor))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showColorMenu = !showColorMenu }
                        .testTag("color_picker_trigger")
                )

                // Thickness button
                IconButton(
                    onClick = { showWidthMenu = !showWidthMenu },
                    modifier = Modifier.testTag("thickness_button")
                ) {
                    Icon(Icons.Default.LineWeight, contentDescription = "Thickness")
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

                // Smart Shape Tool
                FilledIconToggleButton(
                    checked = uiState.currentTool == ToolMode.SHAPE,
                    onCheckedChange = {
                        onSetTool(ToolMode.SHAPE)
                        showShapeMenu = !showShapeMenu
                    },
                    modifier = Modifier.testTag("shape_tool_button")
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Smart Shapes")
                }

                // Auto Computer Text Toggle Chip (Qoraal Computer Toos ah)
                FilterChip(
                    selected = uiState.autoHandwritingConvert,
                    onClick = onToggleAutoConvert,
                    leadingIcon = {
                        Icon(
                            imageVector = if (uiState.autoHandwritingConvert) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.autoHandwritingConvert) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            text = if (uiState.autoHandwritingConvert) "Auto Computer: ON" else "Auto Computer: OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("auto_computer_text_toggle")
                )

                // Converting spinner indicator
                if (uiState.isConvertingHandwriting) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Qoraal Computer...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Manual Handwriting-to-Text OCR Convert Button
                Button(
                    onClick = onConvertHandwriting,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("convert_handwriting_button")
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OCR Text", fontSize = 12.sp)
                }

                // AI Mathematics Assistant (Gemini)
                Button(
                    onClick = onOpenAiAssistant,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("ai_math_assistant_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Math", fontSize = 12.sp)
                }

                // Educational Tools Dropdown Trigger
                Box {
                    IconButton(
                        onClick = { showToolsMenu = true },
                        modifier = Modifier.testTag("educational_tools_menu_button")
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = "Educational Toolkit")
                    }

                    DropdownMenu(
                        expanded = showToolsMenu,
                        onDismissRequest = { showToolsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Digital Acrylic Ruler") },
                            leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                            onClick = {
                                onToggleRuler()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Protractor (Angle Guide)") },
                            leadingIcon = { Icon(Icons.Default.ChangeHistory, contentDescription = null) },
                            onClick = {
                                onToggleProtractor()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Drawing Compass") },
                            leadingIcon = { Icon(Icons.Default.TripOrigin, contentDescription = null) },
                            onClick = {
                                onToggleCompass()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Scientific Calculator") },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            onClick = {
                                onOpenCalculator()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Periodic Table of Elements") },
                            leadingIcon = { Icon(Icons.Default.Science, contentDescription = null) },
                            onClick = {
                                onOpenPeriodicTable()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Formula Library (50+ Formulas)") },
                            leadingIcon = { Icon(Icons.Default.Functions, contentDescription = null) },
                            onClick = {
                                onOpenFormulaLibrary()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Function & Chart Plotter") },
                            leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                            onClick = {
                                onOpenGraphGenerator()
                                showToolsMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (uiState.stylusOnlyMode) "Stylus Mode: Active" else "Stylus Mode: Off") },
                            leadingIcon = { Icon(Icons.Default.BorderColor, contentDescription = null) },
                            onClick = {
                                onToggleStylusOnly()
                                showToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (uiState.autoShapeRecognition) "Auto-Shape Snap: ON" else "Auto-Shape Snap: OFF") },
                            leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                            onClick = {
                                onToggleAutoShape()
                                showToolsMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Theme & Paper Pattern Picker Dialog
// ---------------------------------------------------------

@Composable
fun ThemePatternPickerDialog(
    currentTheme: BoardTheme,
    currentPattern: BackgroundType,
    onSelectTheme: (BoardTheme) -> Unit,
    onSelectPattern: (BackgroundType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Whiteboard Background & Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Board Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardTheme.values().forEach { theme ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(theme.bgHex),
                            border = BorderStroke(
                                width = if (currentTheme == theme) 3.dp else 1.dp,
                                color = if (currentTheme == theme) MaterialTheme.colorScheme.primary else Color.Gray
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .height(64.dp)
                                .clickable { onSelectTheme(theme) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = theme.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(theme.defaultPenHex)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Paper Pattern", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BackgroundType.values().forEach { pattern ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectPattern(pattern) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentPattern == pattern,
                                onClick = { onSelectPattern(pattern) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pattern.displayName, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ---------------------------------------------------------
// Floating Classroom Timer Overlay
// ---------------------------------------------------------

@Composable
fun ClassroomTimerOverlay(
    seconds: Int,
    isRunning: Boolean,
    onToggleRunning: (Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 10.dp,
        modifier = modifier
            .padding(16.dp)
            .testTag("classroom_timer_overlay")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.inversePrimary)
            Spacer(modifier = Modifier.width(10.dp))

            val mins = seconds / 60
            val secs = seconds % 60
            Text(
                text = String.format("%02d:%02d", mins, secs),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = { onToggleRunning(!isRunning) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start"
                )
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}
