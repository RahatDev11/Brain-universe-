package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CardEntity
import com.example.ui.BrainViewModel
import com.example.ui.components.CanvasBackground
import com.example.ui.components.CardNode
import com.example.ui.components.ConnectionsCanvas
import com.example.ui.components.DrawerAIPanel
import com.example.ui.components.DrawerKanbanPanel
import com.example.ui.components.DrawerStatsPanel
import com.example.ui.components.DrawerWorkspacePanel
import com.example.ui.components.EditCardDialog
import com.example.ui.components.FloatingQuickAddPanel
import com.example.ui.components.TopControlBar
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: BrainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainCanvasScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainCanvasScreen(viewModel: BrainViewModel) {
    val filteredCards by viewModel.filteredCards.collectAsState()
    val connections by viewModel.workspaceConnections.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val selectedCardIds by viewModel.selectedCardIds.collectAsState()
    val activeMode by viewModel.currentMode.collectAsState()
    val isConnectionMode by viewModel.isConnectionMode.collectAsState()
    val connectionStartCardId by viewModel.connectionStartCardId.collectAsState()
    val isCardEditMode by viewModel.isCardEditMode.collectAsState()

    // Dialog state handlers
    var activeDialogCard by remember { mutableStateOf<CardEntity?>(null) }
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "WORKSPACES", "STATS", "KANBAN", "AI"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16) // Cybernetic Deep Space canvas background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // INFINITE ZOOMABLE/PANNABLE CANVAS CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.updatePan(pan)
                            viewModel.updateZoom(zoomScale * zoom)
                        }
                    }
                    .testTag("infinite_canvas")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                ) {
                    // 1. Grid Background
                    CanvasBackground()

                    // 2. Connector Lines
                    ConnectionsCanvas(
                        cards = filteredCards,
                        connections = connections,
                        onConnectionTap = { connId -> viewModel.deleteConnection(connId) }
                    )

                    // 3. Card Elements
                    filteredCards.forEach { card ->
                        val isSelected = selectedCardIds.contains(card.id)
                        CardNode(
                            card = card,
                            isSelected = isSelected,
                            isConnectionMode = isConnectionMode,
                            isConnectionStart = connectionStartCardId == card.id,
                            isEditMode = isCardEditMode,
                            onTap = { viewModel.handleCardTap(card.id) },
                            onDrag = { newX, newY -> viewModel.updateCardPosition(card.id, newX, newY) },
                            onEdit = { activeDialogCard = card },
                            onDelete = { viewModel.deleteCard(card.id) },
                            onToggleLock = {
                                viewModel.updateCardDetails(
                                    cardId = card.id,
                                    title = card.title,
                                    content = card.content,
                                    color = card.color,
                                    isLocked = !card.isLocked,
                                    isPinned = card.isPinned,
                                    showTitle = card.showTitle,
                                    showContent = card.showContent
                                )
                            },
                            onToggleChecklist = { updatedContent ->
                                viewModel.updateCardDetails(
                                    cardId = card.id,
                                    title = card.title,
                                    content = updatedContent,
                                    color = card.color,
                                    isLocked = card.isLocked,
                                    isPinned = card.isPinned,
                                    showTitle = card.showTitle,
                                    showContent = card.showContent
                                )
                            },
                            onStartConnection = { viewModel.startConnectionFromCard(card.id) }
                        )
                    }
                }
            }

            // FLOATING OVERLAY MENUS & PANELS
            if (isCardEditMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    // Top Search & Command Deck
                    TopControlBar(
                        viewModel = viewModel,
                        onOpenWorkspaces = { activeDrawer = "WORKSPACES" },
                        onOpenAI = { activeDrawer = "AI" },
                        onOpenStats = { activeDrawer = "STATS" },
                        onOpenKanban = { activeDrawer = "KANBAN" }
                    )

                    // Quick Canvas Mode Pill Selector
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .background(Color(0x99111827), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeSelectPill(
                            label = "Sandbox",
                            isActive = activeMode == "CANVAS",
                            onClick = { viewModel.setMode("CANVAS") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ModeSelectPill(
                            label = "Mind Map",
                            isActive = activeMode == "MIND_MAP",
                            onClick = { viewModel.setMode("MIND_MAP") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ModeSelectPill(
                            label = "Graph Net",
                            isActive = activeMode == "GRAPH",
                            onClick = { viewModel.setMode("GRAPH") }
                        )
                    }
                }
            }

            // Zoom Helper Control Pill (Aligned specifically to BottomStart)
            if (isCardEditMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp),
                    color = Color(0xE6111827), // Deep space slate glass background
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Zoom decrement
                        IconButton(
                            onClick = { viewModel.updateZoom(zoomScale - 0.1f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        // Zoom percent
                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                        // Zoom increment
                        IconButton(
                            onClick = { viewModel.updateZoom(zoomScale + 0.1f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        // Reset zoom viewport
                        IconButton(
                            onClick = { viewModel.resetZoomPan() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset viewport",
                                tint = Color.LightGray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Quick Canvas Mode Action Buttons (Aligned specifically to BottomCenter)
            if (isCardEditMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    color = Color(0xE6111827), // Deep space slate glass background
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeDrawer = "STATS" }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.QueryStats, contentDescription = "Stats", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { activeDrawer = "KANBAN" }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ViewKanban, contentDescription = "Kanban tasks", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.forceSync() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync Cloud Status", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color(0x33FFFFFF))
                        )

                        Button(
                            onClick = { viewModel.toggleCardEditMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Changes",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clickable { viewModel.toggleCardEditMode() },
                    color = Color(0xFF0F766E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Board",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Board",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Speed Dial Plus Selector on Bottom Right (Only in Edit Mode)
            if (isCardEditMode) {
                FloatingQuickAddPanel(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // DYNAMIC BOTTOM DRAWER SHEETS
            when (activeDrawer) {
                "WORKSPACES" -> DrawerWorkspacePanel(viewModel, onDismiss = { activeDrawer = null })
                "STATS" -> DrawerStatsPanel(viewModel, onDismiss = { activeDrawer = null })
                "KANBAN" -> DrawerKanbanPanel(viewModel, onDismiss = { activeDrawer = null })
                "AI" -> DrawerAIPanel(viewModel, onDismiss = { activeDrawer = null })
            }

            // ACTIVE CARD EDIT MODALS
            activeDialogCard?.let { card ->
                EditCardDialog(
                    card = card,
                    viewModel = viewModel,
                    onDismiss = { activeDialogCard = null }
                )
            }
        }
    }
}

@Composable
fun ModeSelectPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isActive) Color(0xFF8B5CF6) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
