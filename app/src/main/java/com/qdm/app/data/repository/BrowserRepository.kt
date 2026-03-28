package com.qdm.app.data.repository

import com.qdm.app.data.local.dao.BrowserHistoryDao
import com.qdm.app.data.local.entity.BrowserHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserRepository @Inject constructor(
    private val dao: BrowserHistoryDao
) {
    fun getHistory(): Flow<List<BrowserHistoryEntity>> = dao.getHistory()

    suspend fun addHistory(url: String, title: String) {
        dao.insertHistory(
            BrowserHistoryEntity(
                url = url,
                title = title,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun search(query: String) = dao.search(query)
}
