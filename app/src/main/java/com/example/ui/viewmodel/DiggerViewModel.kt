package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.MediaMetadata
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TrackEntity
import com.example.data.repository.TrackRepository
import com.example.data.remote.ArchiveApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class DiggerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DiggerViewModel"
    private val context = application.applicationContext
    private val repository: TrackRepository
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null

    private val CHANNEL_ID = "sample_digger_channel"
    private val NOTIFICATION_ID = 8128

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                Log.d(TAG, "Notification action received: $action")
                when (action) {
                    "com.example.ACTION_TOGGLE_PLAY_PAUSE" -> {
                        togglePlayback()
                    }
                    "com.example.ACTION_NEXT_TRACK" -> {
                        performShuffle()
                    }
                    "com.example.ACTION_STOP" -> {
                        pausePlayback()
                        cancelNotification()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Цифровой Диггер Плеер"
            val descriptionText = "Управление воспроизведением винила"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification() {
        val track = currentTrack ?: return
        createNotificationChannel()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, Class.forName("com.example.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent("com.example.ACTION_TOGGLE_PLAY_PAUSE")
        val togglePendingIntent = PendingIntent.getBroadcast(
            context, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent("com.example.ACTION_NEXT_TRACK")
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent("com.example.ACTION_STOP")
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseText = if (isPlaying) "Пауза" else "Старт"
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title ?: "Без названия")
            .setContentText("${track.artist ?: "Неизвестный исполнитель"} • ${track.bpm?.let { "$it BPM" } ?: ""}")
            .setSubText("Цифровой Диггер")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseIcon, playPauseText, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Искать", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Закрыть", stopPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.let { session ->
                builder.setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(session.sessionToken))
                        .setShowActionsInCompactView(0, 1)
                )
            }
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun updateMediaSessionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val session = mediaSession ?: return
            val track = currentTrack
            if (track != null) {
                val metadataBuilder = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, track.title ?: "Без названия")
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist ?: "Неизвестный исполнитель")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)
                session.setMetadata(metadataBuilder.build())
            }

            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or 
                          PlaybackState.ACTION_PAUSE or 
                          PlaybackState.ACTION_PLAY_PAUSE or 
                          PlaybackState.ACTION_SKIP_TO_NEXT or 
                          PlaybackState.ACTION_SEEK_TO

            val stateBuilder = PlaybackState.Builder()
                .setState(state, currentPositionMs, 1.0f)
                .setActions(actions)

            session.setPlaybackState(stateBuilder.build())
        }
    }

    // Room Database init
    init {
        val database = AppDatabase.getDatabase(context)
        repository = TrackRepository(database.trackDao())

        // Initial Seed in background
        viewModelScope.launch {
            repository.seedPopularTracks()
        }

        // Initialize Android native MediaSession for system drawer media controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = MediaSession(context, "SampleDiggerSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        if (!isPlaying) {
                            togglePlayback()
                        }
                    }
                    override fun onPause() {
                        if (isPlaying) {
                            togglePlayback()
                        }
                    }
                    override fun onSkipToNext() {
                        performShuffle()
                    }
                    override fun onSeekTo(pos: Long) {
                        if (durationMs > 0) {
                            seekToFraction(pos.toFloat() / durationMs.toFloat())
                        }
                    }
                })
                isActive = true
            }
        }

        // Register Broadcast Receiver for Notification player actions
        val filter = IntentFilter().apply {
            addAction("com.example.ACTION_TOGGLE_PLAY_PAUSE")
            addAction("com.example.ACTION_NEXT_TRACK")
            addAction("com.example.ACTION_STOP")
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Exposed DB Streams
    val playedHistory: StateFlow<List<TrackEntity>> = repository.playedHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = repository.favoriteTracks
        .map { tracks -> tracks.mapNotNull { it.folder }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    var currentTrack by mutableStateOf<TrackEntity?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isScrubbing by mutableStateOf(false)
        private set

    var playbackProgress by mutableStateOf(0f)
        private set

    var currentPositionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf("Готов к поиску сэмплов!")
        private set

    var showFilterDialog by mutableStateOf(false)
    var showFolderDialog by mutableStateOf(false)

    // Sleep Timer (seconds remaining, null if disabled)
    var sleepTimerRemaining by mutableStateOf<Int?>(null)
        private set

    var generationMode by mutableStateOf("Random") // Random or Continuous

    // Active Filters (State)
    var filterGenre by mutableStateOf("All")
    var filterStyle by mutableStateOf("All")
    var filterRegion by mutableStateOf("All")
    var filterKey by mutableStateOf("All")
    var filterMinBpm by mutableStateOf(0f)
    var filterMaxBpm by mutableStateOf(0f)
    var filterMinYear by mutableStateOf(0f)
    var filterMaxYear by mutableStateOf(0f)
    var filterMinViews by mutableStateOf(0f)
    var filterOnlyTopicChannels by mutableStateOf(false)
    var filterTagsText by mutableStateOf("")

    data class FilterPreset(
        val name: String,
        val genre: String,
        val style: String,
        val region: String,
        val musicKey: String,
        val minBpm: Float,
        val maxBpm: Float,
        val minYear: Float,
        val maxYear: Float,
        val minViews: Float,
        val onlyTopic: Boolean,
        val tags: String
    ) {
        fun serialize(): String {
            return listOf(name, genre, style, region, musicKey, minBpm.toString(), maxBpm.toString(), minYear.toString(), maxYear.toString(), minViews.toString(), onlyTopic.toString(), tags).joinToString("|||")
        }

        companion object {
            fun deserialize(str: String): FilterPreset? {
                val parts = str.split("|||")
                if (parts.size >= 12) {
                    return FilterPreset(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5].toFloatOrNull() ?: 60f, parts[6].toFloatOrNull() ?: 160f, parts[7].toFloatOrNull() ?: 1800f, parts[8].toFloatOrNull() ?: 2026f, parts[9].toFloatOrNull() ?: 0f, parts[10].toBooleanStrictOrNull() ?: false, parts[11])
                }
                return null
            }
        }
    }

    var savedPresets by mutableStateOf<List<FilterPreset>>(emptyList())

    init {
        // Start with clean/unrestricted filters on app launch so digging is wide and accessible initially
        resetFilters()
        loadPresets()
    }

    // Coroutine Jobs
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    /**
     * CENTRAL PINK SHUFFLE FUNCTION (Logical Core)
     */
    fun performShuffle() {
        if (isLoading) return
        isLoading = true
        statusMessage = "Ищем в Archive.org"

        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Stop current player first to avoid audio overlap
                stopPlayback()

                val track = repository.getRandomTrackWithFilters(
                    genre = if (filterGenre == "All") null else filterGenre,
                    style = if (filterStyle == "All") null else filterStyle,
                    region = if (filterRegion == "All") null else filterRegion,
                    musicKey = if (filterKey == "All") null else filterKey,
                    minBpm = if (filterMinBpm <= 0f) null else filterMinBpm.toInt(),
                    maxBpm = if (filterMaxBpm <= 0f) null else filterMaxBpm.toInt(),
                    minYear = if (filterMinYear <= 0f) null else filterMinYear.toInt(),
                    maxYear = if (filterMaxYear <= 0f) null else filterMaxYear.toInt(),
                    minViews = filterMinViews.toInt(),
                    onlyTopicChannels = filterOnlyTopicChannels,
                    tagText = if (filterTagsText.isBlank()) null else filterTagsText,
                    playedTrackIds = playedHistory.value.map { it.id },
                    currentTrackId = currentTrack?.id
                )

                if (track != null) {
                    currentTrack = track
                    statusMessage = "Загружено: ${track.title}"
                    
                    if (!track.audioUrl.isNullOrBlank()) {
                        prepareAndPlay(track.audioUrl)
                    } else {
                        statusMessage = "Аудиопоток недоступен. Пропуск..."
                        isPlaying = false
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(150)
                            performShuffle()
                        }
                    }
                } else {
                    statusMessage = "Больше ничего нет. Все треки по вашим фильтрам уже были прослушаны, либо ничего не найдено."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing shuffle: ", e)
                statusMessage = "Ошибка: не удалось загрузить трек. Попробуйте еще раз."
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Start/Resume Playback
     */
    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (isPlaying) {
            player.pause()
            isPlaying = false
            statusMessage = "Пауза"
            updateNotification()
            updateMediaSessionState()
        } else {
            player.start()
            isPlaying = true
            statusMessage = "Воспроизведение: ${currentTrack?.title}"
            startProgressUpdateLoop()
            updateNotification()
            updateMediaSessionState()
        }
    }

    /**
     * Seek player to specific progress fraction (0f to 1f)
     */
    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        val targetMs = (fraction * durationMs).toInt()
        player.seekTo(targetMs)
        currentPositionMs = targetMs.toLong()
        playbackProgress = fraction
        updateMediaSessionState()
    }

    /**
     * Scrubbing methods for fluid, SoundCloud-like seek drag gestures
     */
    fun startScrubbing() {
        isScrubbing = true
    }

    fun scrubTo(fraction: Float) {
        if (durationMs > 0) {
            playbackProgress = fraction
            currentPositionMs = (fraction * durationMs).toLong()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSession?.let { session ->
                    val stateBuilder = PlaybackState.Builder()
                        .setState(if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, currentPositionMs, 1.0f)
                        .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SEEK_TO)
                    session.setPlaybackState(stateBuilder.build())
                }
            }
        }
    }

    fun endScrubbing(fraction: Float) {
        isScrubbing = false
        seekToFraction(fraction)
    }

    /**
     * Toggle Favorite
     */
    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val updated = track.copy(isFavorite = !track.isFavorite)
            repository.updateTrack(updated)
            if (currentTrack?.id == track.id) {
                currentTrack = updated
            }
        }
    }

    fun saveTrackToFolder(track: TrackEntity, folderName: String) {
        viewModelScope.launch {
            val updated = track.copy(folder = folderName, isFavorite = true)
            repository.updateTrack(updated)
            if (currentTrack?.id == track.id) {
                currentTrack = updated
            }
        }
    }

    /**
     * Update user note
     */
    fun saveUserNote(track: TrackEntity, note: String) {
        viewModelScope.launch {
            val updated = track.copy(userNotes = note)
            repository.updateTrack(updated)
            if (currentTrack?.id == track.id) {
                currentTrack = updated
            }
        }
    }

    /**
     * Sleep Timer Action
     */
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            sleepTimerRemaining = null
            return
        }

        sleepTimerRemaining = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemaining != null && sleepTimerRemaining!! > 0) {
                delay(1000)
                sleepTimerRemaining = sleepTimerRemaining!! - 1
            }
            if (sleepTimerRemaining == 0) {
                Log.d(TAG, "Sleep timer expired. Pausing playback.")
                pausePlayback()
                statusMessage = "Таймер сна истек"
                sleepTimerRemaining = null
            }
        }
    }

    /**
     * Prepare MediaPlayer and begin play
     */
    private fun prepareAndPlay(url: String) {
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { player ->
                    val dur = player.duration.toLong()
                    if (dur <= 0) {
                        Log.w(TAG, "Track has invalid duration ($dur). Skipping.")
                        this@DiggerViewModel.statusMessage = "Недопустимый аудиофайл. Пропуск..."
                        performShuffle()
                        return@setOnPreparedListener
                    }
                    this@DiggerViewModel.durationMs = dur
                    player.start()
                    this@DiggerViewModel.isPlaying = true
                    this@DiggerViewModel.statusMessage = "Воспроизведение: ${currentTrack?.title}"
                    startProgressUpdateLoop()
                    updateNotification()
                    updateMediaSessionState()
                }
                setOnCompletionListener { player ->
                    if (player.currentPosition < 1000 && player.duration <= 0) {
                        Log.w(TAG, "Completed immediately. Invalid track.")
                        this@DiggerViewModel.statusMessage = "Недопустимый аудиофайл. Пропуск..."
                        performShuffle()
                        return@setOnCompletionListener
                    }
                    
                    this@DiggerViewModel.isPlaying = false
                    this@DiggerViewModel.playbackProgress = 1.0f
                    this@DiggerViewModel.currentPositionMs = this@DiggerViewModel.durationMs
                    progressJob?.cancel()
                    cancelNotification()
                    
                    // Continuous mode support
                    if (generationMode == "Continuous") {
                        this@DiggerViewModel.statusMessage = "Трек завершен. Ищем следующий..."
                        performShuffle()
                    } else {
                        this@DiggerViewModel.statusMessage = "Трек завершен"
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    this@DiggerViewModel.statusMessage = "Ошибка воспроизведения. Поиск следующего..."
                    this@DiggerViewModel.isPlaying = false
                    cancelNotification()
                    performShuffle()
                    true // Handled
                }
                prepareAsync()
                this@DiggerViewModel.statusMessage = "Стриминг аудиофайла..."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception preparing MediaPlayer: ", e)
            statusMessage = "Ошибка настройки плеера."
        }
    }

    private fun startProgressUpdateLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isPlaying && mediaPlayer != null) {
                if (!isScrubbing) {
                    try {
                        val pos = mediaPlayer?.currentPosition ?: 0
                        currentPositionMs = pos.toLong()
                        if (durationMs > 0) {
                            playbackProgress = pos.toFloat() / durationMs.toFloat()
                        }
                        updateMediaSessionState()
                    } catch (e: Exception) {
                        // media player state might have shifted
                    }
                }
                delay(250)
            }
        }
    }

    fun selectTrackFromHistory(track: TrackEntity) {
        currentTrack = track
        if (!track.audioUrl.isNullOrBlank()) {
            prepareAndPlay(track.audioUrl)
        } else {
            // Resolve direct audio URL if missing
            isLoading = true
            viewModelScope.launch {
                val resolved = ArchiveApiClient.resolveTrackAudioAndMetadata(track)
                repository.updateTrack(resolved)
                currentTrack = resolved
                isLoading = false
                if (!resolved.audioUrl.isNullOrBlank()) {
                    prepareAndPlay(resolved.audioUrl)
                } else {
                    statusMessage = "Стриминговое аудио не найдено для этого трека."
                }
            }
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                statusMessage = "Пауза"
                updateNotification()
                updateMediaSessionState()
            }
        }
    }

    private fun stopPlayback() {
        progressJob?.cancel()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        mediaPlayer = null
        isPlaying = false
        playbackProgress = 0f
        currentPositionMs = 0L
        cancelNotification()
        updateMediaSessionState()
    }

    /**
     * Filter Preset Local Storage (SharedPreferences)
     */
    fun loadPresets() {
        val prefs = context.getSharedPreferences("sampleette_presets", Context.MODE_PRIVATE)
        val presetsSet = prefs.getStringSet("presets_list", emptySet()) ?: emptySet()
        savedPresets = presetsSet.mapNotNull { FilterPreset.deserialize(it) }.sortedBy { it.name }
    }

    fun saveFiltersPreset(name: String) {
        val newPreset = FilterPreset(
            name = name,
            genre = filterGenre,
            style = filterStyle,
            region = filterRegion,
            musicKey = filterKey,
            minBpm = filterMinBpm,
            maxBpm = filterMaxBpm,
            minYear = filterMinYear,
            maxYear = filterMaxYear,
            minViews = filterMinViews,
            onlyTopic = filterOnlyTopicChannels,
            tags = filterTagsText
        )
        val updated = (savedPresets.filter { it.name != name } + newPreset).sortedBy { it.name }
        savedPresets = updated
        val prefs = context.getSharedPreferences("sampleette_presets", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("presets_list", updated.map { it.serialize() }.toSet()).apply()
        statusMessage = "Пресет '$name' сохранен!"
    }

    fun saveCurrentFilters() {
        val prefs = context.getSharedPreferences("sampleette_presets", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("genre", filterGenre)
            putString("style", filterStyle)
            putString("region", filterRegion)
            putString("musicKey", filterKey)
            putFloat("minBpm", filterMinBpm)
            putFloat("maxBpm", filterMaxBpm)
            putFloat("minYear", filterMinYear)
            putFloat("maxYear", filterMaxYear)
            putFloat("minViews", filterMinViews)
            putBoolean("onlyTopic", filterOnlyTopicChannels)
            putString("tags", filterTagsText)
            apply()
        }
    }

    fun applyPreset(preset: FilterPreset) {
        filterGenre = preset.genre
        filterStyle = preset.style
        filterRegion = preset.region
        filterKey = preset.musicKey
        filterMinBpm = preset.minBpm
        filterMaxBpm = preset.maxBpm
        filterMinYear = preset.minYear
        filterMaxYear = preset.maxYear
        filterMinViews = preset.minViews
        filterOnlyTopicChannels = preset.onlyTopic
        filterTagsText = preset.tags
        saveCurrentFilters()
        statusMessage = "Пресет '${preset.name}' загружен."
    }

    fun resetFilters() {
        filterGenre = "All"
        filterStyle = "All"
        filterRegion = "All"
        filterKey = "All"
        filterMinBpm = 60f
        filterMaxBpm = 160f
        filterMinYear = 1800f
        filterMaxYear = 2026f
        filterMinViews = 0f
        filterOnlyTopicChannels = false
        filterTagsText = ""
        saveCurrentFilters()
        statusMessage = "Фильтров нет"
    }

    private fun loadFiltersPreset() {
        val prefs = context.getSharedPreferences("sampleette_presets", Context.MODE_PRIVATE)
        filterGenre = prefs.getString("genre", "All") ?: "All"
        filterStyle = prefs.getString("style", "All") ?: "All"
        filterRegion = prefs.getString("region", "All") ?: "All"
        filterKey = prefs.getString("musicKey", "All") ?: "All"
        filterMinBpm = prefs.getFloat("minBpm", 60f)
        filterMaxBpm = prefs.getFloat("maxBpm", 160f)
        filterMinYear = prefs.getFloat("minYear", 1800f)
        filterMaxYear = prefs.getFloat("maxYear", 2026f)
        filterMinViews = prefs.getFloat("minViews", 0f)
        filterOnlyTopicChannels = prefs.getBoolean("onlyTopic", false)
        filterTagsText = prefs.getString("tags", "") ?: ""
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        sleepTimerJob?.cancel()
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
        cancelNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        }
    }
}
