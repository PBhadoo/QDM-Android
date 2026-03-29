package com.parveenbhadoo.qdm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.parveenbhadoo.qdm.data.local.dao.BookmarkDao
import com.parveenbhadoo.qdm.data.local.dao.BrowserHistoryDao
import com.parveenbhadoo.qdm.data.local.dao.DownloadDao
import com.parveenbhadoo.qdm.data.local.dao.ScheduledDownloadDao
import com.parveenbhadoo.qdm.data.local.entity.BookmarkEntity
import com.parveenbhadoo.qdm.data.local.entity.BrowserHistoryEntity
import com.parveenbhadoo.qdm.data.local.entity.DownloadEntity
import com.parveenbhadoo.qdm.data.local.entity.ScheduledDownloadEntity

@Database(
    entities = [
        DownloadEntity::class,
        BrowserHistoryEntity::class,
        ScheduledDownloadEntity::class,
        BookmarkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class QdmDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun scheduledDownloadDao(): ScheduledDownloadDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bookmarks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "url TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "addedAt INTEGER NOT NULL)"
                )
            }
        }
    }
}
