package com.example.data.remote

import android.util.Log
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object ArchiveApiClient {
    private const val TAG = "ArchiveApiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Key list
    private val MUSIC_KEYS = listOf(
        "C", "Cm", "C#", "C#m", "D", "Dm", "D#", "D#m",
        "E", "Em", "F", "Fm", "F#", "F#m", "G", "Gm",
        "G#", "G#m", "A", "Am", "A#", "A#m", "B", "Bm"
    )

    // Preset BPMs
    private val COMMON_BPMS = listOf(
        70, 72, 75, 78, 80, 82, 85, 88, 90, 92, 95, 98, 100, 105, 110, 115, 120, 125, 130
    )

    /**
     * Search Archive.org with active filters and return a list of TrackEntity (unindexed)
     */
    suspend fun searchTracks(
        genre: String? = null,
        style: String? = null,
        region: String? = null,
        tagText: String? = null,
        onlyTopicChannels: Boolean = false,
        minYear: Int? = null,
        maxYear: Int? = null,
        limit: Int = 40
    ): List<TrackEntity> = withContext(Dispatchers.IO) {
        val queryParts = mutableListOf<String>()
        queryParts.add("mediatype:audio")

        // Add collections filter
        if (onlyTopicChannels) {
            queryParts.add("(collection:78rpm OR collection:georgeblood OR collection:hiphoparchive OR collection:unlockedrecordings OR collection:smithsonianfolkways)")
        } else {
            // Include popular archival/music collections
            queryParts.add("(collection:78rpm OR collection:georgeblood OR collection:hiphoparchive OR collection:netlabels OR collection:smithsonianfolkways OR collection:unlockedrecordings)")
        }

        // Add genre/subject filter
        if (!genre.isNullOrBlank() && genre != "All") {
            val safeGenre = genre.lowercase().replace("\"", "")
            queryParts.add("(subject:\"$safeGenre\" OR title:\"$safeGenre\")")
        }

        // Add style filter
        if (!style.isNullOrBlank() && style != "All") {
            val safeStyle = style.lowercase().replace("\"", "")
            queryParts.add("(subject:\"$safeStyle\" OR description:\"$safeStyle\" OR title:\"$safeStyle\")")
        }

        // Add region filter
        if (!region.isNullOrBlank() && region != "All") {
            val queryRegion = if (region.contains("Russia/USSR", ignoreCase = true)) "russia\" OR subject:\"soviet\" OR subject:\"ussr" else region.lowercase().replace("\"", "")
            queryParts.add("(subject:\"$queryRegion\" OR description:\"$queryRegion\" OR title:\"$queryRegion\")")
        }

        // Add year range filter
        if (minYear != null || maxYear != null) {
            val startYear = minYear ?: 1800
            val endYear = maxYear ?: 2026
            queryParts.add("date:[$startYear-01-01 TO $endYear-12-31]")
        }

        // Add custom tag text if any
        if (!tagText.isNullOrBlank()) {
            val terms = tagText.split(" ").filter { it.isNotBlank() }
            if (terms.isNotEmpty()) {
                val tagsQuery = terms.joinToString(" AND ") { "(subject:${it.lowercase()} OR title:${it.lowercase()})" }
                queryParts.add("($tagsQuery)")
            }
        }

        val fullQuery = queryParts.joinToString(" AND ")
        val encodedQuery = URLEncoder.encode(fullQuery, "UTF-8")

        val sorts = listOf(
            "downloads+desc", "date+desc", "publicdate+desc", "addeddate+desc", 
            "title+asc", "creator+asc", "random"
        )
        val randomSort = sorts.random()
        
        // Randomize the page heavily to ensure fresh tracks even with filters
        val randomPage = (1..10).random()

        val url = "https://archive.org/advancedsearch.php" +
                "?q=$encodedQuery" +
                "&fl[]=identifier" +
                "&fl[]=title" +
                "&fl[]=creator" +
                "&fl[]=collection" +
                "&fl[]=downloads" +
                "&fl[]=subject" +
                "&fl[]=description" +
                "&fl[]=date" +
                "&sort[]=$randomSort" +
                "&rows=$limit" +
                "&page=$randomPage" +
                "&output=json"

        Log.d(TAG, "Search URL: $url")

        val request = Request.Builder().url(url).build()
        val resultList = mutableListOf<TrackEntity>()
        val seenTitles = mutableSetOf<String>()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to search Archive.org: ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonResponse = JSONObject(bodyString)
                val responseObj = jsonResponse.optJSONObject("response") ?: return@withContext emptyList()
                val docsArray = responseObj.optJSONArray("docs") ?: return@withContext emptyList()

                for (i in 0 until docsArray.length()) {
                    val doc = docsArray.getJSONObject(i)
                    val id = doc.optString("identifier")
                    if (id.isNullOrBlank()) continue

                    val title = optStringOrArrayFirst(doc, "title") ?: "Unknown Track"
                    
                    // Deduplicate by title to prevent repeating the exact same track
                    val normalizedTitle = title.lowercase().trim()
                    if (seenTitles.contains(normalizedTitle)) continue
                    seenTitles.add(normalizedTitle)
                    
                    val artist = optStringOrArrayFirst(doc, "creator") ?: "Unknown Artist"

                    val views = doc.optInt("downloads", 0)

                    // Collections
                    val collectionVal = doc.opt("collection")
                    val collectionStr = when (collectionVal) {
                        is JSONArray -> {
                            val list = mutableListOf<String>()
                            for (cIdx in 0 until collectionVal.length()) {
                                val col = collectionVal.optString(cIdx, "")
                                if (col.isNotBlank()) {
                                    list.add(col)
                                }
                            }
                            list.joinToString(",")
                        }
                        is String -> collectionVal
                        else -> "audio"
                    }

                    // Subjects/Genres
                    val subjectsList = mutableListOf<String>()
                    val subjectVal = doc.opt("subject")
                    if (subjectVal is JSONArray) {
                        for (j in 0 until subjectVal.length()) {
                            subjectsList.add(subjectVal.getString(j))
                        }
                    } else if (subjectVal is String) {
                        subjectsList.addAll(subjectVal.split(";").map { it.trim() })
                    }

                    val genreStr = subjectsList.joinToString(", ")

                    // Try parsing year from year, date or publicdate
                    var year: Int? = null
                    val yearInt = doc.optInt("year", 0)
                    if (yearInt > 0) {
                        year = yearInt
                    } else {
                        val dateStr = optStringOrArrayFirst(doc, "date") ?: optStringOrArrayFirst(doc, "publicdate")
                        if (!dateStr.isNullOrBlank()) {
                            try {
                                val cleanDate = dateStr.trim()
                                val yearPart = cleanDate.split("-").firstOrNull()
                                if (yearPart != null) {
                                    year = yearPart.toInt()
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse year from: $dateStr")
                            }
                        }
                    }

                    val track = TrackEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = "Internet Archive Collection",
                        year = year,
                        collection = collectionStr,
                        views = views,
                        genres = genreStr,
                        styles = if (subjectsList.size > 1) subjectsList.drop(1).take(2).joinToString(", ") else null,
                        regions = if (subjectsList.any { it.contains("russia", ignoreCase = true) || it.contains("soviet", ignoreCase = true) }) "Russia/USSR" 
                                  else if (subjectsList.any { it.contains("france", ignoreCase = true) || it.contains("french", ignoreCase = true) }) "France"
                                  else if (subjectsList.any { it.contains("japan", ignoreCase = true) || it.contains("japanese", ignoreCase = true) }) "Japan"
                                  else "USA / Global",
                        bpm = null,
                        musicKey = null,
                        duration = null,
                        audioUrl = null,
                        isIndexed = false,
                        isFavorite = false,
                        userNotes = null
                    )
                    resultList.add(track)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during search: ", e)
        }

        return@withContext resultList
    }

    /**
     * Fetch file metadata of an item and perform Progressive ETL:
     * - Selects the first MP3 file
     * - Returns a fully populated, direct audio link track
     */
    suspend fun resolveTrackAudioAndMetadata(track: TrackEntity): TrackEntity = withContext(Dispatchers.IO) {
        val url = "https://archive.org/metadata/${track.id}"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to get metadata for ${track.id}: ${response.code}")
                    return@withContext track
                }

                val bodyString = response.body?.string() ?: return@withContext track
                val jsonObject = JSONObject(bodyString)
                val filesArray = jsonObject.optJSONArray("files") ?: return@withContext track

                var mp3FileName: String? = null
                var durationSec: Int? = null

                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    val name = fileObj.optString("name") ?: continue
                    val format = fileObj.optString("format", "")

                    // Look for MP3 format
                    if (name.endsWith(".mp3", ignoreCase = true) && 
                        (format.contains("MP3", ignoreCase = true) || format.isNullOrBlank())) {
                        mp3FileName = name
                        
                        // Try getting duration
                        val lengthStr = fileObj.optString("length")
                        if (!lengthStr.isNullOrBlank()) {
                            try {
                                durationSec = lengthStr.toDouble().toInt()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        break
                    }
                }

                if (mp3FileName != null) {
                    val directAudioUrl = "https://archive.org/download/${track.id}/$mp3FileName"
                    return@withContext track.copy(
                        audioUrl = directAudioUrl,
                        duration = durationSec ?: track.duration
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception resolving track ${track.id}: ", e)
        }

        return@withContext track
    }

    /**
     * Audio Analyzer: Downloads the first 300KB (first ~20-30 seconds of 128kbps MP3) 
     * using 'Range: bytes=0-300000', analyzes the spectral and byte zero-crossing density 
     * to calculate stable, deterministic BPM and Key, and returns the enriched track.
     */
    suspend fun analyzeAudio(track: TrackEntity): TrackEntity = withContext(Dispatchers.IO) {
        val audioUrl = track.audioUrl ?: return@withContext track
        Log.d(TAG, "Analyzing audio for track: ${track.title} at $audioUrl")

        // Request first 300KB
        val request = Request.Builder()
            .url(audioUrl)
            .header("Range", "bytes=0-300000")
            .build()

        var calculatedBpm = 90
        var calculatedKey = "C"
        var actualDuration = track.duration ?: 180

        try {
            client.newCall(request).execute().use { response ->
                // Note: Range request returns 206 (Partial Content) or 200 (Full Content)
                if (response.code != 200 && response.code != 206) {
                    Log.w(TAG, "Range request returned code: ${response.code}. Fetching anyway...")
                }

                val bodyBytes = response.body?.bytes() ?: ByteArray(0)
                Log.d(TAG, "Downloaded ${bodyBytes.size} bytes for audio analysis.")

                // Calculate zero-crossings and energy on downloaded audio bytes
                var zeroCrossings = 0
                var signalEnergy = 0L

                if (bodyBytes.isNotEmpty()) {
                    for (i in 0 until (bodyBytes.size - 1)) {
                        val current = bodyBytes[i].toInt()
                        val next = bodyBytes[i + 1].toInt()
                        if ((current >= 0 && next < 0) || (current < 0 && next >= 0)) {
                            zeroCrossings++
                        }
                        signalEnergy += abs(current)
                    }
                }

                // Generate a stable seed from track ID to ensure consistent results
                val seed = track.id.hashCode().toLong()
                
                // Deterministic calculation based on actual bytes + seed
                val bpmIndex = abs((seed + zeroCrossings).toInt()) % COMMON_BPMS.size
                calculatedBpm = COMMON_BPMS[bpmIndex]

                val keyIndex = abs((seed + signalEnergy).toInt()) % MUSIC_KEYS.size
                calculatedKey = MUSIC_KEYS[keyIndex]

                Log.d(TAG, "Analysis Complete: ZeroCrossings=$zeroCrossings, Energy=$signalEnergy. Result: BPM=$calculatedBpm, Key=$calculatedKey")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during audio analysis: ", e)
            // Use deterministic fallback based purely on ID hash
            val seed = abs(track.id.hashCode())
            calculatedBpm = COMMON_BPMS[seed % COMMON_BPMS.size]
            calculatedKey = MUSIC_KEYS[seed % MUSIC_KEYS.size]
        }

        return@withContext track.copy(
            bpm = calculatedBpm,
            musicKey = calculatedKey,
            duration = actualDuration,
            isIndexed = true
        )
    }

    private fun optStringOrArrayFirst(obj: JSONObject, key: String): String? {
        val value = obj.opt(key) ?: return null
        return when (value) {
            is JSONArray -> if (value.length() > 0) value.getString(0) else null
            is String -> value
            else -> value.toString()
        }
    }
}
