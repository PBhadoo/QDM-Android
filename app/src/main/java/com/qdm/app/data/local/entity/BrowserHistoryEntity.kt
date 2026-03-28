package com.qdm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val visitedAt: Long,
    val favicon: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserHistoryEntity) return false
        return id == other.id && url == other.url
    }

    override fun hashCode(): Int = 31 * id.hashCode() + url.hashCode()
}
