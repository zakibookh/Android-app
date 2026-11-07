package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.MathAssistantResponse
import com.example.data.entity.WhiteboardEntity
import com.example.math.FormulaItem
import com.example.math.FormulaLibrary
import com.example.model.ChartType
import com.example.model.GraphObject
import com.example.tools.CalculatorEngine
import com.example.tools.ElementInfo
import com.example.tools.PeriodicTableData

// ---------------------------------------------------------
// 1. AI MATHEMATICS ASSISTANT SHEET
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMathAssistantSheet(
    aiState: AiTaskState,
    onSolve: (String) -> Unit,
    onExplain: (String) -> Unit,
    onPractice: (String) -> Unit,
    onCorrect: (String) -> Unit,
    onInsertSolution: (MathAssistantResponse) -> Unit,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Solve Step-by-Step", "Explain Concept", "Practice Exercises", "Find Mistakes")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("ai_math_assistant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Mathematics Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pedagogical solver, explanations & practice powered by Gemini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val placeholder = when (selectedTab) {
                0 -> "Enter math equation (e.g. 2x^2 + 5x - 12 = 0, or ∫ x sin(x) dx)"
                1 -> "Enter concept or theorem (e.g. Fundamental Theorem of Calculus)"
                2 -> "Enter topic for practice quiz (e.g. Quadratic Inequalities, Matrix Mult)"
                else -> "Enter student derivation or answer to check for errors"
            }

            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                label = { Text("Problem or Question") },
                placeholder = { Text(placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_assistant_query_input"),
                trailingIcon = {
                    if (queryText.isNotBlank()) {
                        IconButton(onClick = { queryText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            when (selectedTab) {
                                0 -> onSolve(queryText)
                                1 -> onExplain(queryText)
                                2 -> onPractice(queryText)
                                3 -> onCorrect(queryText)
                            }
                        }
                    },
                    enabled = queryText.isNotBlank() && aiState !is AiTaskState.Loading,
                    modifier = Modifier.testTag("ai_assistant_submit_button")
                ) {
                    if (aiState is AiTaskState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (selectedTab) {
                                0 -> "Solve Problem"
                                1 -> "Explain"
                                2 -> "Generate Exercises"
                                else -> "Check Work"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Result Card
            when (aiState) {
                is AiTaskState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Analyzing mathematical structure...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                is AiTaskState.AssistantSuccess -> {
                    val sol = aiState.response
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sol.summary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onInsertSolution(sol) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("insert_solution_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Insert to Board", fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            }

                            items(sol.steps) { step ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = "Step ${step.stepNumber}: ${step.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (step.formulaLatex.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = step.formulaLatex,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = step.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (sol.finalAnswer.isNotBlank()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        Text(
                                            text = "Final Answer: ${sol.finalAnswer}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is AiTaskState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------
// 2. SCIENTIFIC CALCULATOR DIALOG
// ---------------------------------------------------------

@Composable
fun ScientificCalculatorDialog(
    onInsertResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .width(340.dp)
                .testTag("scientific_calculator_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scientific Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Display screen
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expression.ifBlank { " " },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = display,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scientific key grid
                val rows = listOf(
                    listOf("sin", "cos", "tan", "sqrt", "C"),
                    listOf("ln", "log", "^", "(", ")"),
                    listOf("7", "8", "9", "÷", "π"),
                    listOf("4", "5", "6", "×", "e"),
                    listOf("1", "2", "3", "-", "DEL"),
                    listOf("0", ".", "%", "+", "=")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (key in row) {
                            Button(
                                onClick = {
                                    when (key) {
                                        "C" -> {
                                            display = "0"
                                            expression = ""
                                        }
                                        "DEL" -> {
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                                display = expression.ifBlank { "0" }
                                            }
                                        }
                                        "=" -> {
                                            val res = CalculatorEngine.evaluate(expression)
                                            display = res
                                            expression = res
                                        }
                                        "sin", "cos", "tan", "sqrt", "ln", "log" -> {
                                            expression += "$key("
                                            display = expression
                                        }
                                        else -> {
                                            if (expression == "0" && key in "0123456789") {
                                                expression = key
                                            } else {
                                                expression += key
                                            }
                                            display = expression
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                colors = if (key == "=") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                else if (key in listOf("÷", "×", "-", "+")) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(key, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onInsertResult("Ans = $display")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Insert Result to Whiteboard")
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 3. PERIODIC TABLE DIALOG
// ---------------------------------------------------------

@Composable
fun PeriodicTableDialog(
    onInsertElement: (ElementInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedElement by remember { mutableStateOf<ElementInfo?>(PeriodicTableData.elements.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("periodic_table_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Periodic Table of Elements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, symbol, or atomic number...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Selected element inspector card
                val sel = selectedElement
                if (sel != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${sel.number}. ${sel.name} (${sel.symbol})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Atomic Mass: ${sel.mass} u  |  Group: ${sel.group}  |  ${sel.category}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    onInsertElement(sel)
                                    onDismiss()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Insert", fontSize = 12.sp)
                            }
                        }
                    }
                }

                val filtered = PeriodicTableData.elements.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.symbol.contains(searchQuery, ignoreCase = true) ||
                    it.number.toString() == searchQuery
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered) { elem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedElement == elem) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .height(56.dp)
                                .clickable { selectedElement = elem }
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = elem.number.toString(),
                                    fontSize = 9.sp,
                                    color = if (selectedElement == elem) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = elem.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (selectedElement == elem) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 4. FORMULA LIBRARY DIALOG
// ---------------------------------------------------------

@Composable
fun FormulaLibraryDialog(
    onSelectFormula: (FormulaItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCat by remember { mutableStateOf(FormulaLibrary.categories.first()) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("formula_library_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Formula Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                // Category scroll row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormulaLibrary.categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                val formulas = FormulaLibrary.formulas.filter {
                    (it.category == selectedCat || searchQuery.isNotBlank()) &&
                    (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.displayFormula.contains(searchQuery, ignoreCase = true))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(formulas) { formula ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFormula(formula) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formula.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "Insert ➔",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = formula.displayFormula,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Text(formula.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formula.variables, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 5. GRAPH AND CHART GENERATOR DIALOG
// ---------------------------------------------------------

@Composable
fun GraphGeneratorDialog(
    onInsertGraph: (GraphObject) -> Unit,
    onDismiss: () -> Unit
) {
    var chartType by remember { mutableStateOf(ChartType.FUNCTION) }
    var formula by remember { mutableStateOf("y = x^2 - 4") }
    var title by remember { mutableStateOf("Parabola Plot") }
    var labelsStr by remember { mutableStateOf("Jan, Feb, Mar, Apr, May") }
    var valuesStr by remember { mutableStateOf("25, 45, 30, 60, 50") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("graph_generator_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Graph & Chart Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChartType.values().forEach { type ->
                        FilterChip(
                            selected = chartType == type,
                            onClick = { chartType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Plot Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (chartType == ChartType.FUNCTION) {
                    OutlinedTextField(
                        value = formula,
                        onValueChange = { formula = it },
                        label = { Text("Mathematical Function f(x)") },
                        placeholder = { Text("e.g. y = x^2 - 4, sin(x), 2x+3") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("y = x^2", "y = x^2 - 4", "y = sin(x)", "y = cos(x)", "y = 2x + 1", "y = 1/x").forEach { p ->
                            SuggestionChip(
                                onClick = { formula = p },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = labelsStr,
                        onValueChange = { labelsStr = it },
                        label = { Text("Data Labels (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = valuesStr,
                        onValueChange = { valuesStr = it },
                        label = { Text("Data Values (comma-separated numbers)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val parsedLabels = labelsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val parsedValues = valuesStr.split(",").mapNotNull { it.trim().toFloatOrNull() }

                        val graphObj = GraphObject(
                            title = title,
                            formula = formula,
                            chartType = chartType,
                            dataLabels = if (parsedLabels.isNotEmpty()) parsedLabels else listOf("A", "B", "C"),
                            dataValues = if (parsedValues.isNotEmpty()) parsedValues else listOf(10f, 20f, 30f),
                            x = 120f,
                            y = 150f,
                            width = 460f,
                            height = 320f
                        )
                        onInsertGraph(graphObj)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Insert Plot to Whiteboard")
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 6. BOARD MANAGER DIALOG
// ---------------------------------------------------------

@Composable
fun BoardManagerDialog(
    boards: List<WhiteboardEntity>,
    currentBoardId: Long,
    onSelectBoard: (WhiteboardEntity) -> Unit,
    onCreateNewBoard: (String, String) -> Unit,
    onDeleteBoard: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "General")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("board_manager_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Whiteboard Lessons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                // New Board Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("New Lesson Title...") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                onCreateNewBoard(newTitle, selectedSubject)
                                newTitle = ""
                            }
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(boards) { board ->
                        Card(
                            colors = if (board.id == currentBoardId) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBoard(board) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(board.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "${board.subject} • ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(board.updatedAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (boards.size > 1) {
                                    IconButton(onClick = { onDeleteBoard(board.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// 7. EXPORT DIALOG
// ---------------------------------------------------------

@Composable
fun ExportDialog(
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onExportJpg: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Whiteboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select export format for students, printing, or sharing:")

                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.fillMaxWidth().testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as High-Res PDF Document")
                }

                OutlinedButton(
                    onClick = onExportPng,
                    modifier = Modifier.fillMaxWidth().testTag("export_png_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as PNG Image")
                }

                OutlinedButton(
                    onClick = onExportJpg,
                    modifier = Modifier.fillMaxWidth().testTag("export_jpg_button")
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export as JPEG Image")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
