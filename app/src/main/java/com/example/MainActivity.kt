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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
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
                            onTap = { viewModel.handleCardTap(card.id) },
                            onDrag = { newX, newY -> viewModel.updateCardPosition(card.id, newX, newY) },
                            onEdit = { activeDialogCard = card },
                            onDelete = { viewModel.deleteCard(card.id) },
                            onToggleLock = {
                                viewModel.updateCardDetails(
                                    card.id, card.title, card.content, card.color,
                                    !card.isLocked, card.isPinned
                                )
                            },
                            onToggleChecklist = { updatedContent ->
                                viewModel.updateCardDetails(
                                    card.id, card.title, updatedContent, card.color,
                                    card.isLocked, card.isPinned
                                )
                            }
                        )
                    }
                }
            }

            // FLOATING OVERLAY MENUS & PANELS
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

            // Zoom helper controls on bottom left
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                color = Color(0x99111827),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.updateZoom(zoomScale - 0.1f) }) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        text = "${(zoomScale * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(42.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { viewModel.updateZoom(zoomScale + 0.1f) }) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.resetZoomPan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset viewport", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Quick Canvas Mode Action Buttons on bottom center
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                color = Color(0x99111827),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeDrawer = "STATS" }) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Stats", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { activeDrawer = "KANBAN" }) {
                        Icon(Icons.Default.ViewKanban, contentDescription = "Kanban tasks", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.forceSync() }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync Cloud Status", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Speed Dial Plus Selector on Bottom Right
            FloatingQuickAddPanel(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomEnd)
            )

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
