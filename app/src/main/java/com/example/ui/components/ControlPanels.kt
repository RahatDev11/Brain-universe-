package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CardEntity
import com.example.data.database.WorkspaceEntity
import com.example.ui.BrainViewModel

@Composable
fun TopControlBar(
    viewModel: BrainViewModel,
    onOpenWorkspaces: () -> Unit,
    onOpenAI: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenKanban: () -> Unit
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val userEmail by viewModel.currentUserEmail.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isConnectionMode by viewModel.isConnectionMode.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .statusBarsPadding(),
        color = Color(0xBB111827), // Faint glassy dark grey (Arc browser)
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Workspace selector button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenWorkspaces,
                    modifier = Modifier.testTag("workspaces_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Workspaces",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Brain Universe",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = syncStatus,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Canvas search bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search brain nodes...", fontSize = 12.sp, color = Color.Gray) },
                modifier = Modifier
                    .width(180.dp)
                    .height(38.dp)
                    .testTag("search_bar"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x11FFFFFF),
                    unfocusedContainerColor = Color(0x11FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                },
                singleLine = true
            )

            // Dynamic tool buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.toggleConnectionMode() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isConnectionMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        )
                        .testTag("link_tool")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = "Link Cards",
                        tint = if (isConnectionMode) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenAI,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Mind Map Generator",
                        tint = Color(0xFFA855F7), // Beautiful purple gradient
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerWorkspacePanel(
    viewModel: BrainViewModel,
    onDismiss: () -> Unit
) {
    val list by viewModel.workspaces.collectAsState()
    val activeId by viewModel.selectedWorkspaceId.collectAsState()
    var isCreatingWs by remember { mutableStateOf(false) }
    var newWsName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Personal") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Workspaces & Folders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = { isCreatingWs = !isCreatingWs },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create", fontSize = 11.sp)
                }
            }

            AnimatedVisibility(visible = isCreatingWs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    TextField(
                        value = newWsName,
                        onValueChange = { newWsName = it },
                        placeholder = { Text("Workspace Name (e.g., Thesis)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select Canvas Type", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Personal", "Study", "Research", "Project", "Business").forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newWsName.isNotBlank()) {
                                viewModel.createWorkspace(newWsName, selectedType)
                                newWsName = ""
                                isCreatingWs = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Workspace")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { ws ->
                    val isActive = ws.id == activeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectWorkspace(ws.id)
                                onDismiss()
                            }
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary else Color(0x11FFFFFF),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (ws.type) {
                                    "Study" -> Icons.Default.School
                                    "Research" -> Icons.Default.Science
                                    "Project" -> Icons.Default.Schedule
                                    "Business" -> Icons.Default.Work
                                    else -> Icons.Default.AccountBalance
                                },
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(ws.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(ws.type, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                        if (isActive) {
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerStatsPanel(
    viewModel: BrainViewModel,
    onDismiss: () -> Unit
) {
    val cards by viewModel.workspaceCards.collectAsState()
    val connections by viewModel.workspaceConnections.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Dashboard & Brain Growth",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Stat Counter Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Cards", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${cards.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Links", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${connections.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("Knowledge Density Matrix", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Growth Chart drawn dynamically in Compose Canvas!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = 45f
                    val spacing = 30f
                    val paddingLeft = 30f
                    val maxVal = if (cards.isEmpty()) 10f else cards.size.toFloat() * 1.5f
                    val values = floatArrayOf(2f, 4f, 5f, 7f, cards.size.toFloat() + 1)

                    values.forEachIndexed { i, value ->
                        val barHeight = (value / maxVal) * size.height
                        val left = paddingLeft + i * (barWidth + spacing)
                        val top = size.height - barHeight

                        drawRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Mon", "Tue", "Wed", "Thu", "Today").forEach { Day ->
                    Text(Day, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerKanbanPanel(
    viewModel: BrainViewModel,
    onDismiss: () -> Unit
) {
    val cards by viewModel.workspaceCards.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(450.dp)
        ) {
            Text(
                text = "Workspace Kanban Board",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Find checklist tasks and group by complete/pending
            val textCards = cards.filter { it.type != "CHECKLIST" }
            val checklistCards = cards.filter { it.type == "CHECKLIST" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1: Workspace Notes
                KanbanColumn(
                    title = "Concepts (${textCards.size})",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(textCards) { card ->
                            KanbanTaskCard(card.title, card.content)
                        }
                    }
                }

                // Column 2: Pending Tasks
                val pendingTasks = mutableListOf<String>()
                val completedTasks = mutableListOf<String>()

                checklistCards.forEach { card ->
                    val lines = card.content.split("\n")
                    lines.forEach { line ->
                        if (line.trim().startsWith("[ ]") || (!line.trim().startsWith("[x]") && line.isNotBlank())) {
                            pendingTasks.add(line.replace("[ ]", "").trim())
                        } else if (line.trim().startsWith("[x]")) {
                            completedTasks.add(line.replace("[x]", "").trim())
                        }
                    }
                }

                KanbanColumn(
                    title = "Pending (${pendingTasks.size})",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingTasks) { task ->
                            KanbanTaskCard(task, "Action required")
                        }
                    }
                }

                // Column 3: Completed
                KanbanColumn(
                    title = "Completed (${completedTasks.size})",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(completedTasks) { task ->
                            KanbanTaskCard(task, "Archived milestone", isCompleted = true)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF1F2937), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun KanbanTaskCard(title: String, subtitle: String, isCompleted: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                color = if (isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerAIPanel(
    viewModel: BrainViewModel,
    onDismiss: () -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18181B),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "✨ Gemini Brain Node Generator",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter a topic to automatically project structure directly onto your visual canvas.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(14.dp))

            TextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Quantum Computing, Cellular Biology, Roman Empire...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().testTag("ai_prompt_field"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Presets
            Text("Try presets:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PresetChip("Superposition") { promptInput = "Quantum Superposition" }
                }
                item {
                    PresetChip("Mitosis") { promptInput = "Cellular Division Mitosis" }
                }
                item {
                    PresetChip("Greek History") { promptInput = "Athenian Golden Age" }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (aiLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (aiResponse.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF27272A), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(aiResponse, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        viewModel.askAIGenerateMindMap(promptInput)
                    }
                },
                enabled = !aiLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_generate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inject Mind Map Structure")
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 9.sp, color = Color.White) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0x1F94A3B8)
        )
    )
}
