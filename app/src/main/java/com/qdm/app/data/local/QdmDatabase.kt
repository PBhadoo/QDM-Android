package com.parveenbhadoo.qdm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.parveenbhadoo.qdm.data.local.dao.BrowserHistoryDao
import com.parveenbhadoo.qdm.data.local.dao.DownloadDao
import com.parveenbhadoo.qdm.data.local.dao.ScheduledDownloadDao
import com.parveenbhadoo.qdm.data.local.entity.BrowserHistoryEntity
import com.parveenbhadoo.qdm.data.local.entity.DownloadEntity
import com.parveenbhadoo.qdm.data.local.entity.ScheduledDownloadEntity

@Database(
    entities = [
        DownloadEntity::class,
        BrowserHistoryEntity::class,
        ScheduledDownloadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QdmDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun scheduledDownloadDao(): ScheduledDownloadDao
}
