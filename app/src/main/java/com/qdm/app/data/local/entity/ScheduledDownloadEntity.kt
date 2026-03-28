package com.qdm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_downloads")
data class ScheduledDownloadEntity(
    @PrimaryKey val downloadId: String,
    val scheduledAt: Long,
    val workManagerId: String
)
