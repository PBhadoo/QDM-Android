package com.parveenbhadoo.qdm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parveenbhadoo.qdm.data.local.entity.ScheduledDownloadEntity

@Dao
interface ScheduledDownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledDownloadEntity)

    @Query("SELECT * FROM scheduled_downloads WHERE downloadId = :downloadId")
    suspend fun getByDownloadId(downloadId: String): ScheduledDownloadEntity?

    @Query("DELETE FROM scheduled_downloads WHERE downloadId = :downloadId")
    suspend fun delete(downloadId: String)

    @Query("SELECT * FROM scheduled_downloads WHERE scheduledAt <= :now")
    suspend fun getDueSchedules(now: Long): List<ScheduledDownloadEntity>
}
