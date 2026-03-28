package com.qdm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qdm.app.data.local.dao.BrowserHistoryDao
import com.qdm.app.data.local.dao.DownloadDao
import com.qdm.app.data.local.dao.ScheduledDownloadDao
import com.qdm.app.data.local.entity.BrowserHistoryEntity
import com.qdm.app.data.local.entity.DownloadEntity
import com.qdm.app.data.local.entity.ScheduledDownloadEntity

@Database(
    entities = [
        DownloadEntity::class,
        BrowserHistoryEntity::class,
        ScheduledDownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QdmDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun scheduledDownloadDao(): ScheduledDownloadDao
}
