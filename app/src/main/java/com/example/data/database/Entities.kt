package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String // "Personal", "Study", "Research", "Project", "Business"
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val parentFolderId: Long? = null,
    val name: String
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val folderId: Long? = null,
    val type: String, // "TEXT", "CHECKLIST", "IMAGE", "AI", "BOOKMARK", "SHAPE"
    val title: String,
    val content: String, // Markdown/rich text, checklist items raw string, URL, or shape JSON
    val x: Float,
    val y: Float,
    val width: Float = 220f,
    val height: Float = 160f,
    val rotation: Float = 0f,
    val color: String = "#FF2D3748", // Dark slate background or elegant pastel colors
    val isLocked: Boolean = false,
    val isPinned: Boolean = false,
    val coverUri: String? = null,
    val iconName: String? = null
)

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val sourceCardId: Long,
    val targetCardId: Long,
    val label: String = "",
    val color: String = "#FF805AD5", // Purple connection default
    val thickness: Float = 3f,
    val lineStyle: String = "CURVED" // "STRAIGHT", "CURVED", "ARROW", "BIDIRECTIONAL", "DOTTED"
)
