package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    // Workspaces
    @Query("SELECT * FROM workspaces")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long

    @Query("DELETE FROM workspaces WHERE id = :workspaceId")
    suspend fun deleteWorkspace(workspaceId: Long)

    // Folders
    @Query("SELECT * FROM folders WHERE workspaceId = :workspaceId")
    fun getFoldersForWorkspace(workspaceId: Long): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    // Cards
    @Query("SELECT * FROM cards WHERE workspaceId = :workspaceId")
    fun getCardsForWorkspace(workspaceId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("DELETE FROM cards WHERE workspaceId = :workspaceId")
    suspend fun clearWorkspaceCards(workspaceId: Long)

    // Connections
    @Query("SELECT * FROM connections WHERE workspaceId = :workspaceId")
    fun getConnectionsForWorkspace(workspaceId: Long): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionEntity): Long

    @Query("DELETE FROM connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: Long)

    @Query("DELETE FROM connections WHERE sourceCardId = :cardId OR targetCardId = :cardId")
    suspend fun deleteConnectionsForCard(cardId: Long)
}
