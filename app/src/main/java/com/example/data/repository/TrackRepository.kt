package com.example.data.repository

import android.util.Log
import com.example.data.local.TrackDao
import com.example.data.local.TrackEntity
import com.example.data.remote.ArchiveApiClient
import kotlinx.coroutines.flow.Flow

class TrackRepository(private val trackDao: TrackDao) {
    private val TAG = "TrackRepository"

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
    val playedHistory: Flow<List<TrackEntity>> = trackDao.getPlayedHistory()

    suspend fun getTrackById(id: String): TrackEntity? {
        return trackDao.getTrackById(id)
    }

    suspend fun insertTrack(track: TrackEntity) {
        trackDao.insertTrack(track)
    }

    suspend fun updateTrack(track: TrackEntity) {
        trackDao.updateTrack(track)
    }

    suspend fun deleteTrack(track: TrackEntity) {
        trackDao.deleteTrack(track)
    }

    /**
     * Performs initial seeding of the DB when the app starts
     */
    suspend fun seedPopularTracks() {
        try {
            val existing = trackDao.getRawTracksList()
            if (existing.isEmpty()) {
                Log.d(TAG, "Database is empty. Seeding popular archival tracks...")
                val jazzTracks = ArchiveApiClient.searchTracks(genre = "jazz", limit = 15)
                val soulTracks = ArchiveApiClient.searchTracks(genre = "soul", limit = 15)
                val funkTracks = ArchiveApiClient.searchTracks(genre = "funk", limit = 15)
                val allFetched = jazzTracks + soulTracks + funkTracks
                if (allFetched.isNotEmpty()) {
                    trackDao.insertTracks(allFetched)
                    Log.d(TAG, "Seeded ${allFetched.size} tracks successfully.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding database: ", e)
        }
    }

    /**
     * Returns a random track matching the current UI filter parameters.
     * If there are matching un-indexed tracks in the database, we can select one.
     * If no matching tracks exist in the database, we fetch them on-the-fly from Archive.org.
     */
    suspend fun getRandomTrackWithFilters(
        genre: String?,
        style: String?,
        region: String?,
        musicKey: String?,
        minBpm: Int?,
        maxBpm: Int?,
        minYear: Int?,
        maxYear: Int?,
        minViews: Int?,
        onlyTopicChannels: Boolean,
        tagText: String? = null,
        playedTrackIds: List<String> = emptyList(),
        currentTrackId: String? = null
    ): TrackEntity? {
        // Normalize empty filters to null for SQLite compatibility
        val cleanGenre = if (genre.isNullOrBlank() || genre == "All") null else genre
        val cleanStyle = if (style.isNullOrBlank() || style == "All") null else style
        val cleanRegion = if (region.isNullOrBlank() || region == "All") null else region
        val cleanKey = if (musicKey.isNullOrBlank() || musicKey == "All") null else musicKey
        val cleanMinBpm = if (minBpm == 60) null else minBpm
        val cleanMaxBpm = if (maxBpm == 160) null else maxBpm
        val cleanMinYear = if (minYear == 1800) null else minYear
        val cleanMaxYear = if (maxYear == 2026) null else maxYear

        Log.d(TAG, "Filtering with: Genre=$cleanGenre, Style=$cleanStyle, Region=$cleanRegion, Key=$cleanKey, BPM=[$cleanMinBpm-$cleanMaxBpm], Year=[$cleanMinYear-$cleanMaxYear]")

        // 1. Query the database first
        var dbMatches = trackDao.getTracksWithFilters(
            musicKey = if (cleanKey?.contains("|") == true) null else cleanKey, // If multi-select, filter in memory
            minBpm = cleanMinBpm,
            maxBpm = cleanMaxBpm,
            minYear = cleanMinYear,
            maxYear = cleanMaxYear,
            minViews = minViews,
            onlyTopicChannels = if (onlyTopicChannels) 1 else 0
        )

        // Filter multi-select items in memory
        if (onlyTopicChannels) {
            val topicCols = setOf("78rpm", "georgeblood", "hiphoparchive", "unlockedrecordings", "smithsonianfolkways")
            dbMatches = dbMatches.filter { track ->
                val trackCols = track.collection?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
                trackCols.any { col -> col in topicCols }
            }
        }

        if (!cleanGenre.isNullOrBlank()) {
            val terms = cleanGenre.split("|")
            dbMatches = dbMatches.filter { track ->
                terms.any { term ->
                    track.genres?.contains(term, ignoreCase = true) == true ||
                    track.styles?.contains(term, ignoreCase = true) == true
                }
            }
        }
        if (!cleanStyle.isNullOrBlank()) {
            val terms = cleanStyle.split("|")
            dbMatches = dbMatches.filter { track ->
                terms.any { term ->
                    track.styles?.contains(term, ignoreCase = true) == true ||
                    track.genres?.contains(term, ignoreCase = true) == true
                }
            }
        }
        if (!cleanRegion.isNullOrBlank()) {
            val terms = cleanRegion.split("|")
            dbMatches = dbMatches.filter { track -> terms.any { term -> track.regions?.contains(term, ignoreCase = true) == true } }
        }
        if (!cleanKey.isNullOrBlank()) {
            val terms = cleanKey.split("|")
            dbMatches = dbMatches.filter { track -> terms.any { term -> track.musicKey?.equals(term, ignoreCase = true) == true } }
        }

        // Filter by tags locally if specified
        if (!tagText.isNullOrBlank()) {
            val terms = tagText.lowercase().split(" ").filter { it.isNotBlank() }
            if (terms.isNotEmpty()) {
                dbMatches = dbMatches.filter { track ->
                    terms.all { term ->
                        track.genres?.lowercase()?.contains(term) == true ||
                        track.styles?.lowercase()?.contains(term) == true ||
                        track.regions?.lowercase()?.contains(term) == true ||
                        track.title?.lowercase()?.contains(term) == true ||
                        track.artist?.lowercase()?.contains(term) == true
                    }
                }
            }
        }

        // 2. If we don't have enough matches in local database, fetch online
        var candidatesPool = mutableListOf<TrackEntity>()
        var fetchAttempts = 0

        while (candidatesPool.isEmpty() && fetchAttempts < 3) {
            val unplayedCount = dbMatches.count { it.id !in playedTrackIds && it.id != currentTrackId }
            if (unplayedCount < 10) {
                Log.d(TAG, "Few local matches ($unplayedCount). Fetching from Archive.org (attempt ${fetchAttempts+1}/3)...")
                try {
                    // Pass first genre (if any) to online search
                    val primaryGenre = cleanGenre?.split("|")?.firstOrNull()
                    val primaryStyle = cleanStyle?.split("|")?.firstOrNull()
                    val primaryRegion = cleanRegion?.split("|")?.firstOrNull()
                    
                    val onlineTracks = ArchiveApiClient.searchTracks(
                        genre = primaryGenre,
                        style = primaryStyle,
                        region = primaryRegion,
                        tagText = tagText,
                        onlyTopicChannels = onlyTopicChannels,
                        minYear = cleanMinYear,
                        maxYear = cleanMaxYear,
                        limit = 50
                    )

                    if (onlineTracks.isNotEmpty()) {
                        trackDao.insertTracks(onlineTracks)
                        
                        // Re-query local DB after inserting online tracks
                        dbMatches = trackDao.getTracksWithFilters(
                            musicKey = if (cleanKey?.contains("|") == true) null else cleanKey,
                            minBpm = cleanMinBpm,
                            maxBpm = cleanMaxBpm,
                            minYear = cleanMinYear,
                            maxYear = cleanMaxYear,
                            minViews = minViews,
                            onlyTopicChannels = if (onlyTopicChannels) 1 else 0
                        )
                        
                        // Filter again
                        if (onlyTopicChannels) {
                            val topicCols = setOf("78rpm", "georgeblood", "hiphoparchive", "unlockedrecordings", "smithsonianfolkways")
                            dbMatches = dbMatches.filter { track ->
                                val trackCols = track.collection?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
                                trackCols.any { col -> col in topicCols }
                            }
                        }
                        if (!cleanGenre.isNullOrBlank()) {
                            val terms = cleanGenre.split("|")
                            dbMatches = dbMatches.filter { track ->
                                terms.any { term ->
                                    track.genres?.contains(term, ignoreCase = true) == true ||
                                    track.styles?.contains(term, ignoreCase = true) == true
                                }
                            }
                        }
                        if (!cleanStyle.isNullOrBlank()) {
                            val terms = cleanStyle.split("|")
                            dbMatches = dbMatches.filter { track ->
                                terms.any { term ->
                                    track.styles?.contains(term, ignoreCase = true) == true ||
                                    track.genres?.contains(term, ignoreCase = true) == true
                                }
                            }
                        }
                        if (!cleanRegion.isNullOrBlank()) {
                            val terms = cleanRegion.split("|")
                            dbMatches = dbMatches.filter { track -> terms.any { term -> track.regions?.contains(term, ignoreCase = true) == true } }
                        }
                        if (!cleanKey.isNullOrBlank()) {
                            val terms = cleanKey.split("|")
                            dbMatches = dbMatches.filter { track -> terms.any { term -> track.musicKey?.equals(term, ignoreCase = true) == true } }
                        }
                        if (!tagText.isNullOrBlank()) {
                            val terms = tagText.lowercase().split(" ").filter { it.isNotBlank() }
                            dbMatches = dbMatches.filter { track ->
                                terms.all { term ->
                                    track.genres?.lowercase()?.contains(term) == true ||
                                    track.styles?.lowercase()?.contains(term) == true ||
                                    track.regions?.lowercase()?.contains(term) == true ||
                                    track.title?.lowercase()?.contains(term) == true ||
                                    track.artist?.lowercase()?.contains(term) == true
                                }
                            }
                        }
                    } else if (unplayedCount == 0) {
                         // No local matches and no online tracks found
                         break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Online search failed, fallback to local database", e)
                }
            }

            // 3. PRIORITIZATION: Already-Indexed vs Unindexed candidates
            val perfectMatches = dbMatches.filter { track ->
                val matchesKey = cleanKey == null || (track.musicKey != null && cleanKey.split("|").any { it.equals(track.musicKey, ignoreCase = true) })
                val matchesMinBpm = cleanMinBpm == null || (track.bpm != null && track.bpm!! >= cleanMinBpm)
                val matchesMaxBpm = cleanMaxBpm == null || (track.bpm != null && track.bpm!! <= cleanMaxBpm)
                val matchesMinYear = cleanMinYear == null || (track.year != null && track.year!! >= cleanMinYear)
                val matchesMaxYear = cleanMaxYear == null || (track.year != null && track.year!! <= cleanMaxYear)
                
                track.isIndexed && matchesKey && matchesMinBpm && matchesMaxBpm && matchesMinYear && matchesMaxYear
            }

            val unplayedPerfect = perfectMatches.filter { it.id !in playedTrackIds && it.id != currentTrackId }.shuffled()
            val unindexed = dbMatches.filter { !it.isIndexed && it.id != currentTrackId }
            val unplayedUnindexed = unindexed.filter { it.id !in playedTrackIds }.shuffled()

            candidatesPool.clear()
            candidatesPool.addAll(unplayedPerfect)
            candidatesPool.addAll(unplayedUnindexed)

            if (candidatesPool.isNotEmpty() || unplayedCount >= 10) {
                break
            }
            fetchAttempts++
        }

        if (candidatesPool.isEmpty()) {
            Log.w(TAG, "No more unplayed tracks matching the criteria.")
            return null
        }

        var selectedTrack: TrackEntity? = null
        var analysisAttempts = 0

        for (candidate in candidatesPool) {
            var resolved = candidate

            val needsResolution = resolved.audioUrl.isNullOrBlank()
            val needsBpmKeyAnalysis = resolved.bpm == null || resolved.musicKey == null
            val needsAnalysis = !resolved.isIndexed || (needsBpmKeyAnalysis && !resolved.audioUrl.isNullOrBlank())

            if (needsResolution || needsAnalysis) {
                if (analysisAttempts >= 15) {
                    continue // Skip network-heavy processing if limit reached, try to find cached perfect matches
                }
                analysisAttempts++
            }

            // 4. Resolve direct audio URL if missing
            if (resolved.audioUrl.isNullOrBlank()) {
                Log.d(TAG, "Resolving direct audio URL for ${resolved.title}...")
                resolved = ArchiveApiClient.resolveTrackAudioAndMetadata(resolved)
                trackDao.updateTrack(resolved)
            }

            // Check duration (skip if under 30 seconds)
            if (resolved.duration != null && resolved.duration < 30) {
                Log.d(TAG, "Track ${resolved.title} is too short (${resolved.duration}s < 30s). Skipping...")
                continue
            }

            // 5. Dynamic/On-the-fly analysis (BPM, Key) if indexed is false or missing
            if (!resolved.isIndexed || (needsBpmKeyAnalysis && resolved.audioUrl != null)) {
                Log.d(TAG, "Performing progressive ETL audio analysis for ${resolved.title}...")
                resolved = ArchiveApiClient.analyzeAudio(resolved)
                trackDao.updateTrack(resolved)
            }

            // 6. Post-analysis filter verification!
            if (cleanKey != null) {
                if (resolved.musicKey == null || !cleanKey.split("|").any { it.equals(resolved.musicKey, ignoreCase = true) }) {
                    Log.d(TAG, "Track ${resolved.title} Key (${resolved.musicKey}) does not match $cleanKey. Skipping...")
                    continue
                }
            }
            if (cleanMinBpm != null) {
                if (resolved.bpm == null || resolved.bpm!! < cleanMinBpm) {
                    Log.d(TAG, "Track ${resolved.title} BPM (${resolved.bpm}) is below $cleanMinBpm. Skipping...")
                    continue
                }
            }
            if (cleanMaxBpm != null) {
                if (resolved.bpm == null || resolved.bpm!! > cleanMaxBpm) {
                    Log.d(TAG, "Track ${resolved.title} BPM (${resolved.bpm}) is above $cleanMaxBpm. Skipping...")
                    continue
                }
            }
            if (cleanMinYear != null) {
                if (resolved.year == null || resolved.year!! < cleanMinYear) {
                    Log.d(TAG, "Track ${resolved.title} Year (${resolved.year}) is below $cleanMinYear. Skipping...")
                    continue
                }
            }
            if (cleanMaxYear != null) {
                if (resolved.year == null || resolved.year!! > cleanMaxYear) {
                    Log.d(TAG, "Track ${resolved.title} Year (${resolved.year}) is above $cleanMaxYear. Skipping...")
                    continue
                }
            }

            // If we made it here, this candidate is a 100% valid match!
            selectedTrack = resolved
            break
        }

        if (selectedTrack == null) {
            Log.w(TAG, "No tracks matching filters with duration >= 30s found.")
            return null
        }

        // 7. Record that it has been played in history
        val playedTrack = selectedTrack.copy(lastPlayedAt = System.currentTimeMillis())
        trackDao.updateTrack(playedTrack)

        return playedTrack
    }
}
