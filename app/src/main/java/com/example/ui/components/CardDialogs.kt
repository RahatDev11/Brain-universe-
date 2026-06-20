package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.CardEntity
import com.example.ui.BrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: CardEntity,
    viewModel: BrainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(card.title) }
    var content by remember { mutableStateOf(card.content) }
    var colorSelected by remember { mutableStateOf(card.color) }
    var isLocked by remember { mutableStateOf(card.isLocked) }
    var isPinned by remember { mutableStateOf(card.isPinned) }

    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

    // Pastel/Sleek color palette compatible with dark canvas
    val colorOptions = listOf(
        "#FF1F2937", // Lead slate
        "#FF1E3A8A", // Indigo
        "#FF14532D", // Forest Green
        "#FF581C87", // Plum Purple
        "#FF701A75", // Wine Red
        "#03254A"    // Midnight Blue
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1F2937),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Core Node Properties",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Node Title") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_card_title"),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White)
                )

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content Description / Markdown Items") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("edit_card_content"),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White),
                    maxLines = 8
                )

                // Color Picker Row
                Column {
                    Text("Canvas Color Theme", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        width = if (colorSelected == hex) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { colorSelected = hex }
                            )
                        }
                    }
                }

                // Tweak states (Lock, Pin)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isLocked,
                            onCheckedChange = { isLocked = it }
                        )
                        Text("Lock Location", color = Color.White, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                        Text("Pin Board", color = Color.White, fontSize = 11.sp)
                    }
                }

                Divider(color = Color(0x11FFFFFF))

                // AI Assist Action row inside the dialogue!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Copilot assist", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { viewModel.askAISummarize(card) }) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Summarize", tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { viewModel.askAIExplain(card) }) {
                            Icon(Icons.Outlined.School, contentDescription = "Explain", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (aiLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                AnimatedVisibility(visible = aiResponse.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111827), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = aiResponse,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.updateCardDetails(card.id, title, content, colorSelected, isLocked, isPinned)
                            onDismiss()
                        }
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingQuickAddPanel(
    viewModel: BrainViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text node spawn
                FloatingActionButton(
                    onClick = {
                        viewModel.addNewCard(type = "TEXT", title = "New Concept", content = "Write concept explanation here...")
                        isExpanded = false
                    },
                    containerColor = Color(0xFF312E81),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Notes, contentDescription = "Add Text Card", tint = Color.White)
                }

                // Checklist task spawn
                FloatingActionButton(
                    onClick = {
                        viewModel.addNewCard(type = "CHECKLIST", title = "Action Items", content = "[ ] Task 1\n[ ] Task 2\n[ ] Task 3")
                        isExpanded = false
                    },
                    containerColor = Color(0xFF064E3B),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.CheckBox, contentDescription = "Add Task Checklist", tint = Color.White)
                }

                // Link web reference spawn
                FloatingActionButton(
                    onClick = {
                        viewModel.addNewCard(type = "BOOKMARK", title = "Reference bookmark", content = "https://wikipedia.org/")
                        isExpanded = false
                    },
                    containerColor = Color(0xFF1E3A8A),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = "Add URL Card", tint = Color.White)
                }
            }
        }

        // Core Plus Button Dial
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("fab_plus_dial")
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Add else Icons.Default.Add,
                contentDescription = "Expand controls",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
