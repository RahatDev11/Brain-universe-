package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.GeneratingTokens
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CardEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardNode(
    card: CardEntity,
    isSelected: Boolean,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleChecklist: (String) -> Unit // Handles checkbox checking
) {
    var rawX by remember(card.id) { mutableStateOf(card.x) }
    var rawY by remember(card.id) { mutableStateOf(card.y) }

    Card(
        modifier = Modifier
            .offset { IntOffset(card.x.roundToInt(), card.y.roundToInt()) }
            .width(card.width.dp)
            .height(card.height.dp)
            .rotate(card.rotation)
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black,
                spotColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(card.id) {
                if (!card.isLocked) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rawX += dragAmount.x
                            rawY += dragAmount.y
                            onDrag(rawX, rawY)
                        }
                    )
                }
            }
            .combinedClickable(
                onClick = onTap,
                onDoubleClick = onEdit
            )
            .testTag("card_node_${card.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(card.color))
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (card.type) {
                        "AI" -> Icons.Outlined.GeneratingTokens
                        "BOOKMARK" -> Icons.Outlined.Link
                        "CHECKLIST" -> Icons.Outlined.CheckBox
                        else -> Icons.Outlined.Notes
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = card.type,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header Badges / Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (card.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (card.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Card Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (card.type == "CHECKLIST") {
                    // Render Checklist
                    val items = parseChecklistContent(card.content)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.take(4).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = "Check",
                                    tint = if (item.checked) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .combinedClickable(
                                            onClick = {
                                                val updatedList = toggleChecklistItem(items, item.text)
                                                onToggleChecklist(serializeChecklist(updatedList))
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.text,
                                    color = if (item.checked) Color.White.copy(alpha = 0.5f) else Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else null
                                )
                            }
                        }
                        if (items.size > 4) {
                            Text(
                                text = "+ ${items.size - 4} more tasks",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 22.dp)
                            )
                        }
                    }
                } else {
                    // Regular Text
                    Text(
                        text = card.content,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Quick hover actions in card footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Card",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Card",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// Checklist Helpers
data class ChecklistItem(val text: String, val checked: Boolean)

fun parseChecklistContent(content: String): List<ChecklistItem> {
    val lines = content.split("\n")
    val list = mutableListOf<ChecklistItem>()
    lines.forEach { line ->
        if (line.trim().startsWith("[x]")) {
            list.add(ChecklistItem(line.substringAfter("[x]").trim(), true))
        } else if (line.trim().startsWith("[ ]")) {
            list.add(ChecklistItem(line.substringAfter("[ ]").trim(), false))
        } else if (line.isNotBlank()) {
            list.add(ChecklistItem(line.trim(), false))
        }
    }
    return list
}

fun serializeChecklist(items: List<ChecklistItem>): String {
    return items.joinToString("\n") { item ->
        if (item.checked) "[x] ${item.text}" else "[ ] ${item.text}"
    }
}

fun toggleChecklistItem(items: List<ChecklistItem>, text: String): List<ChecklistItem> {
    return items.map {
        if (it.text == text) it.copy(checked = !it.checked) else it
    }
}
