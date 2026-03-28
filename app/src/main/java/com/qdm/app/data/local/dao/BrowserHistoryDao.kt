package com.parveenbhadoo.qdm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parveenbhadoo.qdm.data.local.entity.BrowserHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserHistoryDao {
    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 500")
    fun getHistory(): Flow<List<BrowserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteEntry(id: Int)

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM browser_history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 50")
    suspend fun search(query: String): List<BrowserHistoryEntity>
}
