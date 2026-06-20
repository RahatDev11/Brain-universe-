package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FirebaseSyncManager
import com.example.data.api.GeminiApiService
import com.example.data.database.AppDatabase
import com.example.data.database.CardEntity
import com.example.data.database.ConnectionEntity
import com.example.data.database.WorkspaceEntity
import com.example.data.repository.BrainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class BrainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "BrainViewModel"
    private val repository: BrainRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BrainRepository(database.cardDao())
        FirebaseSyncManager.initialize(application)
        seedInitialData()
    }

    // Workspaces
    val workspaces: StateFlow<List<WorkspaceEntity>> = repository.allWorkspaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWorkspaceId = MutableStateFlow<Long>(-1)
    val selectedWorkspaceId: StateFlow<Long> = _selectedWorkspaceId.asStateFlow()

    // Screen Modes: "CANVAS", "MIND_MAP", "GRAPH", "KANBAN"
    private val _currentMode = MutableStateFlow("CANVAS")
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    // Zoom and Pan
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    // Multi-Select or Active selection
    private val _selectedCardIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCardIds: StateFlow<Set<Long>> = _selectedCardIds.asStateFlow()

    // Connection Mode (Linking cards)
    private val _isConnectionMode = MutableStateFlow(false)
    val isConnectionMode: StateFlow<Boolean> = _isConnectionMode.asStateFlow()

    private val _connectionStartCardId = MutableStateFlow<Long?>(null)
    val connectionStartCardId: StateFlow<Long?> = _connectionStartCardId.asStateFlow()

    private val _isCardEditMode = MutableStateFlow(false)
    val isCardEditMode: StateFlow<Boolean> = _isCardEditMode.asStateFlow()

    fun toggleCardEditMode() {
        _isCardEditMode.value = !_isCardEditMode.value
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Cards
    val workspaceCards: StateFlow<List<CardEntity>> = _selectedWorkspaceId
        .flatMapLatest { wsId ->
            if (wsId == -1L) flowOf(emptyList())
            else repository.getCardsForWorkspace(wsId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Cards based on Search
    val filteredCards: StateFlow<List<CardEntity>> = combine(workspaceCards, _searchQuery) { cards, query ->
        if (query.isBlank()) cards
        else cards.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Connections
    val workspaceConnections: StateFlow<List<ConnectionEntity>> = _selectedWorkspaceId
        .flatMapLatest { wsId ->
            if (wsId == -1L) flowOf(emptyList())
            else repository.getConnectionsForWorkspace(wsId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Assistant States
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    // Firebase Auth and sync bridge
    val syncStatus: StateFlow<String> = FirebaseSyncManager.syncStatus
    val currentUserEmail: StateFlow<String?> = FirebaseSyncManager.currentUserEmail

    fun selectWorkspace(workspaceId: Long) {
        _selectedWorkspaceId.value = workspaceId
        // Reset canvas offsets
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
        _selectedCardIds.value = emptySet()
    }

    fun setMode(mode: String) {
        _currentMode.value = mode
        if (mode == "MIND_MAP") {
            autoLayoutMindMap()
        } else if (mode == "GRAPH") {
            autoLayoutForceDirectedGraph()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateZoom(zoom: Float) {
        _zoomScale.value = zoom.coerceIn(0.2f, 3.0f)
    }

    fun updatePan(offset: Offset) {
        _panOffset.value = _panOffset.value + offset
    }

    fun resetZoomPan() {
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
    }

    // Card CRUD
    fun addNewCard(
        type: String = "TEXT",
        title: String = "New Concept",
        content: String = "Write details...",
        color: String = "#FF1F2937",
        posX: Float? = null,
        posY: Float? = null
    ) {
        val wsId = _selectedWorkspaceId.value
        if (wsId == -1L) return

        viewModelScope.launch {
            // Place near center of viewport or slightly spread
            val finalX = posX ?: (500f - _panOffset.value.x + (Math.random() * 80).toFloat())
            val finalY = posY ?: (400f - _panOffset.value.y + (Math.random() * 80).toFloat())

            val card = CardEntity(
                workspaceId = wsId,
                type = type,
                title = title,
                content = content,
                x = finalX,
                y = finalY,
                color = color
            )
            repository.insertCard(card)
            triggerBackgroundCloudSync()
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
            _selectedCardIds.value = _selectedCardIds.value.filter { it != cardId }.toSet()
            triggerBackgroundCloudSync()
        }
    }

    fun updateCardPosition(cardId: Long, newX: Float, newY: Float) {
        viewModelScope.launch {
            val card = workspaceCards.value.find { it.id == cardId }
            if (card != null && !card.isLocked) {
                repository.updateCard(card.copy(x = newX, y = newY))
            }
        }
    }

    fun updateCardDetails(
        cardId: Long,
        title: String,
        content: String,
        color: String,
        isLocked: Boolean,
        isPinned: Boolean,
        showTitle: Boolean = true,
        showContent: Boolean = true
    ) {
        viewModelScope.launch {
            val card = workspaceCards.value.find { it.id == cardId }
            if (card != null) {
                repository.updateCard(card.copy(
                    title = title,
                    content = content,
                    color = color,
                    isLocked = isLocked,
                    isPinned = isPinned,
                    showTitle = showTitle,
                    showContent = showContent
                ))
                triggerBackgroundCloudSync()
            }
        }
    }

    // Connection CRUD
    fun toggleConnectionMode() {
        _isConnectionMode.value = !_isConnectionMode.value
        _connectionStartCardId.value = null
    }

    fun startConnectionFromCard(cardId: Long) {
        _isConnectionMode.value = true
        _connectionStartCardId.value = cardId
    }

    fun handleCardTap(cardId: Long) {
        if (_isConnectionMode.value) {
            val startId = _connectionStartCardId.value
            if (startId == null) {
                _connectionStartCardId.value = cardId
            } else {
                if (startId != cardId) {
                    addConnection(startId, cardId)
                }
                _connectionStartCardId.value = null
                _isConnectionMode.value = false
            }
        } else {
            // Toggle selection
            val current = _selectedCardIds.value.toMutableSet()
            if (current.contains(cardId)) {
                current.remove(cardId)
            } else {
                current.add(cardId)
            }
            _selectedCardIds.value = current
        }
    }

    fun clearSelections() {
        _selectedCardIds.value = emptySet()
    }

    fun addConnection(sourceId: Long, targetId: Long) {
        val wsId = _selectedWorkspaceId.value
        if (wsId == -1L) return

        viewModelScope.launch {
            // Prevent duplicates
            val exists = workspaceConnections.value.any {
                (it.sourceCardId == sourceId && it.targetCardId == targetId) ||
                (it.sourceCardId == targetId && it.targetCardId == sourceId)
            }
            if (!exists) {
                repository.insertConnection(
                    ConnectionEntity(
                        workspaceId = wsId,
                        sourceCardId = sourceId,
                        targetCardId = targetId,
                        lineStyle = "CURVED",
                        color = "#FF805AD5"
                    )
                )
                triggerBackgroundCloudSync()
            }
        }
    }

    fun deleteConnection(connectionId: Long) {
        viewModelScope.launch {
            repository.deleteConnection(connectionId)
            triggerBackgroundCloudSync()
        }
    }

    // Workspaces CRUD
    fun createWorkspace(name: String, type: String) {
        viewModelScope.launch {
            val id = repository.insertWorkspace(
                WorkspaceEntity(name = name, type = type)
            )
            selectWorkspace(id)
        }
    }

    // AI-Trigger Actions
    fun askAISummarize(card: CardEntity) {
        _aiLoading.value = true
        _aiResponse.value = ""
        viewModelScope.launch {
            val prompt = "Summarize the following note into three core points inside Brain Universe. Keep it elegant and professional.\n\nNote Title: ${card.title}\nContent:\n${card.content}"
            val summary = GeminiApiService.generate(prompt, jsonMode = false)
            _aiResponse.value = summary
            _aiLoading.value = false
        }
    }

    fun askAIExplain(card: CardEntity) {
        _aiLoading.value = true
        _aiResponse.value = ""
        viewModelScope.launch {
            val prompt = "Explain the concepts mentioned in this note in depth. Expand on hidden connections or implications:\n\nNote Title: ${card.title}\nContent:\n${card.content}"
            val explanation = GeminiApiService.generate(prompt, jsonMode = false)
            _aiResponse.value = explanation
            _aiLoading.value = false
        }
    }

    fun askAIGenerateMindMap(topic: String) {
        if (topic.isBlank() || _selectedWorkspaceId.value == -1L) return
        _aiLoading.value = true
        _aiResponse.value = "Generating conceptual mind map on: '$topic'..."
        viewModelScope.launch {
            val prompt = """
                Generate a hierarchical mind map structure for the topic "$topic" in JSON format.
                Provide structured fields.
                JSON Schema:
                {
                  "root": { "title": "Topic Name", "description": "Brief summary" },
                  "branches": [
                    {
                      "title": "Branch Name 1",
                      "description": "Branch description",
                      "subnodes": [
                        { "title": "Subnode 1", "description": "Subnode details" }
                      ]
                    }
                  ]
                }
                Return ONLY valid raw JSON conforming to this schema. No markdown formatting blocks or quotes.
            """.trimIndent()

            val response = GeminiApiService.generate(prompt, jsonMode = true)
            try {
                // Parse and map to actual cards
                val wsId = _selectedWorkspaceId.value
                val cleanJson = response.substringAfter("{").substringBeforeLast("}")
                val rootJson = "{$cleanJson}"
                val jsonObj = JSONObject(rootJson)
                
                val root = jsonObj.getJSONObject("root")
                val rootTitle = root.getString("title")
                val rootDesc = root.getString("description")

                // Create root card at center
                val rootX = 600f
                val rootY = 500f
                val rootId = repository.insertCard(CardEntity(
                    workspaceId = wsId, type = "AI", title = rootTitle, content = rootDesc,
                    x = rootX, y = rootY, color = "#FF553C9A" // Dark rich purple
                ))

                val branches = jsonObj.getJSONArray("branches")
                for (i in 0 until branches.length()) {
                    val branchObj = branches.getJSONObject(i)
                    val branchTitle = branchObj.getString("title")
                    val branchDesc = branchObj.getString("description")

                    // Distribute branch cards radially
                    val angle = (2 * Math.PI * i / branches.length()).toFloat()
                    val branchX = rootX + (350 * Math.cos(angle.toDouble())).toFloat()
                    val branchY = rootY + (300 * Math.sin(angle.toDouble())).toFloat()

                    val branchId = repository.insertCard(CardEntity(
                        workspaceId = wsId, type = "TEXT", title = branchTitle, content = branchDesc,
                        x = branchX, y = branchY, color = "#FF319795" // Teal
                    ))

                    // Connect root to branch
                    repository.insertConnection(ConnectionEntity(
                        workspaceId = wsId, sourceCardId = rootId, targetCardId = branchId, lineStyle = "CURVED", color = "#FF553C9A"
                    ))

                    val subnodes = branchObj.optJSONArray("subnodes") ?: JSONArray()
                    for (j in 0 until subnodes.length()) {
                        val subnodeObj = subnodes.getJSONObject(j)
                        val subnodeTitle = subnodeObj.getString("title")
                        val subnodeDesc = subnodeObj.getString("description")

                        // Distribute subnode cards radial from branch
                        val subAngle = angle + (j - (subnodes.length() - 1) / 2f) * 0.4f
                        val subnodeX = branchX + (220 * Math.cos(subAngle.toDouble())).toFloat()
                        val subnodeY = branchY + (180 * Math.sin(subAngle.toDouble())).toFloat()

                        val subId = repository.insertCard(CardEntity(
                            workspaceId = wsId, type = "TEXT", title = subnodeTitle, content = subnodeDesc,
                            x = subnodeX, y = subnodeY, color = "#FF2D3748" // Charcoal
                        ))

                        // Connect branch to subnode
                        repository.insertConnection(ConnectionEntity(
                            workspaceId = wsId, sourceCardId = branchId, targetCardId = subId, lineStyle = "DOTTED", color = "#FF319795"
                        ))
                    }
                }

                _aiResponse.value = "Mind Map for '$topic' populated on the canvas!"
                resetZoomPan()
            } catch (e: Exception) {
                Log.e(TAG, "Fail parsing AI MindMap response: $response", e)
                _aiResponse.value = "AI Generated mind-map, but parsing failed. Live text:\n\n$response"
            }
            _aiLoading.value = false
            triggerBackgroundCloudSync()
        }
    }

    // Auto layout generators (Heptabase and Miro layouts)
    fun autoLayoutMindMap() {
        val cards = workspaceCards.value
        if (cards.isEmpty()) return
        Log.d(TAG, "Triggering Mind Map hierarchical layout")
        viewModelScope.launch {
            // Place central card, spread others vertically/horizontally
            val startX = 200f
            var currentY = 150f
            cards.sortedBy { it.id }.forEachIndexed { index, card ->
                repository.updateCard(card.copy(x = startX + (index % 3) * 260f, y = currentY + (index / 3) * 200f))
            }
        }
    }

    fun autoLayoutForceDirectedGraph() {
        val cards = workspaceCards.value
        val connections = workspaceConnections.value
        if (cards.isEmpty()) return
        Log.d(TAG, "Triggering force-directed layout")
        // Basic physics simulation (simplistic for instant Compose placement)
        viewModelScope.launch {
            val numCards = cards.size
            val centerX = 600f
            val centerY = 500f
            cards.forEachIndexed { i, card ->
                val angle = (2 * Math.PI * i / numCards).toFloat()
                val radius = 250f + (Math.random() * 80).toFloat()
                val targetX = centerX + (radius * Math.cos(angle.toDouble())).toFloat()
                val targetY = centerY + (radius * Math.sin(angle.toDouble())).toFloat()
                repository.updateCard(card.copy(x = targetX, y = targetY))
            }
        }
    }

    // Firebase Auth functions
    fun loginWithEmail(email: String) {
        viewModelScope.launch {
            FirebaseSyncManager.loginWithEmail(email, "pass123")
            triggerBackgroundCloudSync()
        }
    }

    fun logout() {
        FirebaseSyncManager.logout()
    }

    fun forceSync() {
        triggerBackgroundCloudSync()
    }

    private fun triggerBackgroundCloudSync() {
        val ws = workspaces.value
        val cd = workspaceCards.value
        val cn = workspaceConnections.value
        viewModelScope.launch {
            FirebaseSyncManager.syncToCloud(ws, cd, cn)
        }
    }

    // Populate standard visual knowledge cards on first start
    private fun seedInitialData() {
        viewModelScope.launch {
            // Check if workspaces are empty
            repository.allWorkspaces.collect { list ->
                if (list.isEmpty()) {
                    Log.i(TAG, "Seeding default Brain Universe database.")
                    val ws1Id = repository.insertWorkspace(WorkspaceEntity(name = "Brain Center", type = "Personal"))
                    val ws2Id = repository.insertWorkspace(WorkspaceEntity(name = "Study Node", type = "Study"))

                    // Seeding first workspace
                    val c1 = repository.insertCard(CardEntity(
                        workspaceId = ws1Id, type = "AI", title = "🚀 Brain Universe Hub",
                        content = "Welcome to your digital brain center!\n\nThis app combines the infinite board layout of **Heptabase/Miro** with the structured linkages of **Obsidian**.\n\n- Drag any card to reposition it.\n- Pinch or slide to zoom and pan.\n- Check Kanban/Dashboard via drawers below.",
                        x = 550f, y = 300f, color = "#FF553C9A", isPinned = true
                    ))

                    val c2 = repository.insertCard(CardEntity(
                        workspaceId = ws1Id, type = "TEXT", title = "💡 Connections Engine",
                        content = "Build pathways between concepts!\n\n1. Press the 'Link' button on the toolbar.\n2. Tap Card A, then tap Card B.\n3. A beautiful logical relation line will bind them in the network instantly.",
                        x = 150f, y = 100f, color = "#FF1F2937"
                    ))

                    val c3 = repository.insertCard(CardEntity(
                        workspaceId = ws1Id, type = "CHECKLIST", title = "✅ Project Milestones",
                        content = "[x] Learn to drag canvas\n[ ] Generate an AI Mind Map\n[x] Setup Google Firebase\n[ ] Create 10 connections",
                        x = 950f, y = 100f, color = "#FF22543D" // Deep forest green
                    ))

                    val c4 = repository.insertCard(CardEntity(
                        workspaceId = ws1Id, type = "BOOKMARK", title = "🔗 Arc & Heptabase Design",
                        content = "https://heptabase.com/ - Visual memory mapping models.\nExplore linear curves, transparency sliders, and cards snapping.",
                        x = 550f, y = 650f, color = "#FF1A365D" // Midnight blue
                    ))

                    // Connect them!
                    repository.insertConnection(ConnectionEntity(workspaceId = ws1Id, sourceCardId = c1, targetCardId = c2, label = "Requires", lineStyle = "CURVED", color = "#FF553C9A"))
                    repository.insertConnection(ConnectionEntity(workspaceId = ws1Id, sourceCardId = c1, targetCardId = c3, label = "Executes", lineStyle = "ARROW", color = "#FF319795"))
                    repository.insertConnection(ConnectionEntity(workspaceId = ws1Id, sourceCardId = c1, targetCardId = c4, label = "Refers to", lineStyle = "DOTTED", color = "#FF805AD5"))

                    // Seeding second workspace
                    val sc1 = repository.insertCard(CardEntity(
                        workspaceId = ws2Id, type = "TEXT", title = "Biology: Neurotransmitters",
                        content = "Summary of neurobiology research nodes.\n\n- Dopamine (Reward/Motivation)\n- Serotonin (Mood stabilization)\n- GABA (CNS inhibitor)",
                        x = 400f, y = 350f, color = "#FF1F2937"
                    ))

                    selectWorkspace(ws1Id)
                } else if (_selectedWorkspaceId.value == -1L) {
                    selectWorkspace(list.first().id)
                }
            }
        }
    }
}
