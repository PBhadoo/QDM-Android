package com.parveenbhadoo.qdm.data.repository

import com.parveenbhadoo.qdm.data.local.dao.BookmarkDao
import com.parveenbhadoo.qdm.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val dao: BookmarkDao
) {
    fun getAll(): Flow<List<BookmarkEntity>> = dao.getAll()

    suspend fun add(url: String, title: String) =
        dao.insert(BookmarkEntity(url = url, title = title))

    suspend fun remove(url: String) = dao.deleteByUrl(url)

    suspend fun isBookmarked(url: String): Boolean = dao.isBookmarked(url) > 0
}
