package com.example.ui.components
import kotlinx.coroutines.launch
import android.app.Application
import android.util.Log
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.DiggerViewModel
import com.example.ui.theme.TextGray

class DiggerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        try {
            if (modelClass.isAssignableFrom(DiggerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DiggerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        } catch (e: Throwable) {
            Log.e("DiggerVMFactory", "CRITICAL ERROR: Failed to instantiate DiggerViewModel", e)
            throw RuntimeException("DiggerViewModel instantiation failed: ${e.message}", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiggerApp(
    viewModel: DiggerViewModel = viewModel(
        factory = DiggerViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.playedHistory.collectAsState()
    val favoriteList by viewModel.favoriteTracks.collectAsState()
    val foldersList by viewModel.folders.collectAsState()

    // Determine form factor dynamically
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMultiPane = screenWidth >= 900 // Wide screen (tablets, landscape)

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 1) { 3 }
    val coroutineScope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current

    // Trigger haptic feedback when page changes
    LaunchedEffect(pagerState.currentPage) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        bottomBar = {
            if (!isMultiPane) {
                // Mobile ergonomic Bottom Navigation Bar
                val primaryColor = MaterialTheme.colorScheme.primary
                NavigationBar(
                    containerColor = Color(0xFF161616),
                    contentColor = TextGray,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { 
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        label = { Text("Инфо", fontSize = 10.sp) },
                        icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Track Info Details") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            indicatorColor = primaryColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { 
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        label = { Text("Плеер", fontSize = 10.sp) },
                        icon = { Icon(imageVector = Icons.Filled.Radio, contentDescription = "Active Studio Player") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            indicatorColor = primaryColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = { 
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        label = { Text("История", fontSize = 10.sp) },
                        icon = { Icon(imageVector = Icons.Filled.History, contentDescription = "Digging History & Stats") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            indicatorColor = primaryColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        
        val infiniteTransition = rememberInfiniteTransition(label = "BgGradient")
        val color1 by infiniteTransition.animateColor(
            initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            targetValue = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
            animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "color1"
        )
        val color2 by infiniteTransition.animateColor(
            initialValue = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
            targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            animationSpec = infiniteRepeatable(animation = tween(11000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "color2"
        )

        // Main Responsive Layout Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F0F))
                .padding(innerPadding)
        ) {
            // Animated Gradient Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(color1, color2, Color.Transparent)
                        )
                    )
            )

            if (isMultiPane) {
                // High-End Studio Multi-Pane Layout (Side-By-Side sidebars + core player)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Left Panel (Metadata & Tags Info Card)
                    TrackDetailsView(
                        track = viewModel.currentTrack,
                        onSaveNotes = { track, notes -> viewModel.saveUserNote(track, notes) },
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    )

                    // Center Panel (Vinyl turntable + sliders + shuffle)
                    CenterPlayerView(
                        track = viewModel.currentTrack,
                        isPlaying = viewModel.isPlaying,
                        playbackProgress = viewModel.playbackProgress,
                        currentPositionMs = viewModel.currentPositionMs,
                        durationMs = viewModel.durationMs,
                        isLoading = viewModel.isLoading,
                        statusMessage = viewModel.statusMessage,
                        onTogglePlayback = { viewModel.togglePlayback() },
                        onSeekToFraction = { frac -> viewModel.seekToFraction(frac) },
                        onStartScrubbing = { viewModel.startScrubbing() },
                        onScrubToFraction = { frac -> viewModel.scrubTo(frac) },
                        onEndScrubbing = { frac -> viewModel.endScrubbing(frac) },
                        onShuffleClick = { viewModel.performShuffle() },
                        onFilterClick = { viewModel.showFilterDialog = true },
                        onSaveToFolderClick = { viewModel.showFolderDialog = true },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    )

                    // Right Panel (Segmented queue, modes switcher, and tempo analytics graph)
                    RightHistoryView(
                        historyList = historyList,
                        favoriteList = favoriteList,
                        currentTrack = viewModel.currentTrack,
                        generationMode = viewModel.generationMode,
                        onGenerationModeChange = { viewModel.generationMode = it },
                        onSelectTrack = { viewModel.selectTrackFromHistory(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Phone Ergonomic Mobile Layout - Viewport Switcher
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            TrackDetailsView(
                                track = viewModel.currentTrack,
                                onSaveNotes = { track, notes -> viewModel.saveUserNote(track, notes) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> {
                            CenterPlayerView(
                                track = viewModel.currentTrack,
                                isPlaying = viewModel.isPlaying,
                                playbackProgress = viewModel.playbackProgress,
                                currentPositionMs = viewModel.currentPositionMs,
                                durationMs = viewModel.durationMs,
                                isLoading = viewModel.isLoading,
                                statusMessage = viewModel.statusMessage,
                                onTogglePlayback = { viewModel.togglePlayback() },
                                onSeekToFraction = { frac -> viewModel.seekToFraction(frac) },
                                onStartScrubbing = { viewModel.startScrubbing() },
                                onScrubToFraction = { frac -> viewModel.scrubTo(frac) },
                                onEndScrubbing = { frac -> viewModel.endScrubbing(frac) },
                                onShuffleClick = { viewModel.performShuffle() },
                                onFilterClick = { viewModel.showFilterDialog = true },
                                onSaveToFolderClick = { viewModel.showFolderDialog = true },
                                onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                                modifier = Modifier.fillMaxSize(),
                                pagerState = pagerState
                            )
                        }
                        2 -> {
                            RightHistoryView(
                                historyList = historyList,
                                favoriteList = favoriteList,
                                currentTrack = viewModel.currentTrack,
                                generationMode = viewModel.generationMode,
                                onGenerationModeChange = { viewModel.generationMode = it },
                                onSelectTrack = { viewModel.selectTrackFromHistory(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Global Filter Configuration Dialog Popup
            FiltersDialog(
                showDialog = viewModel.showFilterDialog,
                onDismissRequest = { 
                    viewModel.showFilterDialog = false
                    viewModel.saveCurrentFilters()
                },
                genre = viewModel.filterGenre,
                onGenreChange = { viewModel.filterGenre = it },
                style = viewModel.filterStyle,
                onStyleChange = { viewModel.filterStyle = it },
                region = viewModel.filterRegion,
                onRegionChange = { viewModel.filterRegion = it },
                musicKey = viewModel.filterKey,
                onMusicKeyChange = { viewModel.filterKey = it },
                minBpm = viewModel.filterMinBpm,
                maxBpm = viewModel.filterMaxBpm,
                onBpmRangeChange = { min, max ->
                    viewModel.filterMinBpm = min
                    viewModel.filterMaxBpm = max
                },
                minYear = viewModel.filterMinYear,
                maxYear = viewModel.filterMaxYear,
                onYearRangeChange = { min, max ->
                    viewModel.filterMinYear = min
                    viewModel.filterMaxYear = max
                },
                minViews = viewModel.filterMinViews,
                onMinViewsChange = { viewModel.filterMinViews = it },
                onlyTopicChannels = viewModel.filterOnlyTopicChannels,
                onOnlyTopicChannelsChange = { viewModel.filterOnlyTopicChannels = it },
                tagsText = viewModel.filterTagsText,
                onTagsTextChange = { viewModel.filterTagsText = it },
                savedPresets = viewModel.savedPresets,
                onSavePreset = { name -> viewModel.saveFiltersPreset(name) },
                onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                onResetFilters = { viewModel.resetFilters() }
            )

            // Folder Configuration Dialog Popup
            FolderDialog(
                showDialog = viewModel.showFolderDialog,
                onDismissRequest = { viewModel.showFolderDialog = false },
                existingFolders = foldersList,
                onSaveToFolder = { folderName ->
                    viewModel.currentTrack?.let { track ->
                        viewModel.saveTrackToFolder(track, folderName)
                    }
                }
            )
        }
    }
}
