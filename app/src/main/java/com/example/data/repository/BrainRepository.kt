package com.example.data.repository

import com.example.data.database.CardDao
import com.example.data.database.CardEntity
import com.example.data.database.ConnectionEntity
import com.example.data.database.FolderEntity
import com.example.data.database.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BrainRepository(private val cardDao: CardDao) {

    // Workspaces
    val allWorkspaces: Flow<List<WorkspaceEntity>> = cardDao.getAllWorkspaces()

    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long {
        return cardDao.insertWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspaceId: Long) {
        cardDao.deleteWorkspace(workspaceId)
    }

    // Folders
    fun getFoldersForWorkspace(workspaceId: Long): Flow<List<FolderEntity>> {
        return cardDao.getFoldersForWorkspace(workspaceId)
    }

    suspend fun insertFolder(folder: FolderEntity): Long {
        return cardDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folderId: Long) {
        cardDao.deleteFolder(folderId)
    }

    // Cards
    fun getCardsForWorkspace(workspaceId: Long): Flow<List<CardEntity>> {
        return cardDao.getCardsForWorkspace(workspaceId)
    }

    suspend fun getCardById(cardId: Long): CardEntity? {
        return cardDao.getCardById(cardId)
    }

    suspend fun insertCard(card: CardEntity): Long {
        return cardDao.insertCard(card)
    }

    suspend fun updateCard(card: CardEntity) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(cardId: Long) {
        cardDao.deleteConnectionsForCard(cardId)
        cardDao.deleteCard(cardId)
    }

    suspend fun clearWorkspaceCards(workspaceId: Long) {
        cardDao.clearWorkspaceCards(workspaceId)
    }

    // Connections
    fun getConnectionsForWorkspace(workspaceId: Long): Flow<List<ConnectionEntity>> {
        return cardDao.getConnectionsForWorkspace(workspaceId)
    }

    suspend fun insertConnection(connection: ConnectionEntity): Long {
        return cardDao.insertConnection(connection)
    }

    suspend fun deleteConnection(connectionId: Long) {
        cardDao.deleteConnection(connectionId)
    }
}
