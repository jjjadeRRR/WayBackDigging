package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun getPlayedHistory(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks")
    suspend fun getRawTracksList(): List<TrackEntity>

    @Query("""
        SELECT * FROM tracks 
        WHERE (:musicKey IS NULL OR musicKey IS NULL OR musicKey = :musicKey)
          AND (:minBpm IS NULL OR bpm IS NULL OR bpm >= :minBpm)
          AND (:maxBpm IS NULL OR bpm IS NULL OR bpm <= :maxBpm)
          AND (:minYear IS NULL OR year IS NULL OR year >= :minYear)
          AND (:maxYear IS NULL OR year IS NULL OR year <= :maxYear)
          AND (:minViews IS NULL OR views IS NULL OR views >= :minViews)
          AND (:onlyTopicChannels = 0 OR (
              collection LIKE '%78rpm%' OR 
              collection LIKE '%georgeblood%' OR 
              collection LIKE '%hiphoparchive%' OR 
              collection LIKE '%unlockedrecordings%' OR 
              collection LIKE '%smithsonianfolkways%'
          ))
          AND (duration IS NULL OR duration >= 30)
    """)
    suspend fun getTracksWithFilters(
        musicKey: String?,
        minBpm: Int?,
        maxBpm: Int?,
        minYear: Int?,
        maxYear: Int?,
        minViews: Int?,
        onlyTopicChannels: Int // 1 for true, 0 for false
    ): List<TrackEntity>
}
