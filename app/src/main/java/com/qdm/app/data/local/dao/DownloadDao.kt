package com.qdm.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qdm.app.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY addedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE stateName = :state ORDER BY addedAt DESC")
    fun getDownloadsByState(state: String): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET stateName = :state, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateState(id: String, state: String, errorMessage: String? = null)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, speedBytesPerSec = :speed, etaSeconds = :eta WHERE id = :id")
    suspend fun updateProgress(id: String, downloadedBytes: Long, speed: Long, eta: Long)

    @Query("UPDATE downloads SET stateName = 'Completed', completedAt = :completedAt, downloadedBytes = totalBytes WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("SELECT COUNT(*) FROM downloads WHERE stateName = 'Downloading'")
    suspend fun getActiveDownloadCount(): Int

    @Query("SELECT * FROM downloads WHERE stateName IN ('Pending', 'Paused', 'Downloading') ORDER BY addedAt ASC")
    suspend fun getPendingDownloads(): List<DownloadEntity>
}
