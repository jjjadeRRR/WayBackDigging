package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String, // Archive.org identifier
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val collection: String?,
    val views: Int,
    val genres: String?, // Comma-separated tags/genres
    val styles: String?,
    val regions: String?,
    val bpm: Int?,
    val musicKey: String?,
    val duration: Int?, // in seconds
    val audioUrl: String?,
    val isIndexed: Boolean = false,
    val isFavorite: Boolean = false,
    val folder: String? = null,
    val userNotes: String? = null,
    val lastPlayedAt: Long? = null // Timestamp of when it was played (for history)
)
