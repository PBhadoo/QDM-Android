package com.qdm.app.di

import android.content.Context
import androidx.room.Room
import com.qdm.app.data.local.QdmDatabase
import com.qdm.app.data.local.dao.BrowserHistoryDao
import com.qdm.app.data.local.dao.DownloadDao
import com.qdm.app.data.local.dao.ScheduledDownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QdmDatabase =
        Room.databaseBuilder(context, QdmDatabase::class.java, "qdm.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDownloadDao(db: QdmDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideBrowserHistoryDao(db: QdmDatabase): BrowserHistoryDao = db.browserHistoryDao()

    @Provides
    fun provideScheduledDownloadDao(db: QdmDatabase): ScheduledDownloadDao = db.scheduledDownloadDao()
}
