package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrackEntity
import com.example.ui.theme.CardCharcoal
import com.example.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun RightHistoryView(
    historyList: List<TrackEntity>,
    favoriteList: List<TrackEntity>,
    currentTrack: TrackEntity?,
    generationMode: String,
    onGenerationModeChange: (String) -> Unit,
    onSelectTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val folders = remember(favoriteList) {
        favoriteList.mapNotNull { it.folder }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 3 }
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // History vs Favorites vs Folders Tab Bar with click & animated scroll
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabHeader(
                label = "ИСТОРИЯ",
                isSelected = pagerState.currentPage == 0,
                count = historyList.size,
                modifier = Modifier.weight(1f)
            ) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
            TabHeader(
                label = "ИЗБРАННОЕ",
                isSelected = pagerState.currentPage == 1,
                count = favoriteList.size,
                modifier = Modifier.weight(1f)
            ) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }
            TabHeader(
                label = "ПАПКИ",
                isSelected = pagerState.currentPage == 2,
                count = folders.size,
                modifier = Modifier.weight(1f)
            ) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(2)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

        // Horizontal Pager allowing horizontal swiping between tabs
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    if (historyList.isEmpty()) {
                        EmptyPlaceholder(
                            text = "Вы еще не прослушали ни одного трека",
                            icon = Icons.Filled.QueueMusic
                        )
                    } else {
                        TrackList(
                            list = historyList,
                            currentTrack = currentTrack,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onSelectTrack = onSelectTrack,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
                1 -> {
                    if (favoriteList.isEmpty()) {
                        EmptyPlaceholder(
                            text = "Нет сохраненных закладок",
                            icon = Icons.Filled.StarBorder
                        )
                    } else {
                        TrackList(
                            list = favoriteList,
                            currentTrack = currentTrack,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onSelectTrack = onSelectTrack,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
                2 -> {
                    if (folders.isEmpty()) {
                        EmptyPlaceholder(
                            text = "Папки пока не созданы",
                            icon = Icons.Filled.Folder
                        )
                    } else {
                        FolderList(
                            folders = folders,
                            favoriteList = favoriteList,
                            currentTrack = currentTrack,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onSelectTrack = onSelectTrack,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = "Empty",
                tint = TextGray.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = TextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TrackList(
    list: List<TrackEntity>,
    currentTrack: TrackEntity?,
    primaryColor: Color,
    secondaryColor: Color,
    onSelectTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list, key = { it.id }) { track ->
            TrackItem(
                track = track,
                currentTrack = currentTrack,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                onSelectTrack = onSelectTrack,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
fun TrackItem(
    track: TrackEntity,
    currentTrack: TrackEntity?,
    primaryColor: Color,
    secondaryColor: Color,
    onSelectTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit
) {
    val isCurrent = track.id == currentTrack?.id
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) primaryColor.copy(alpha = 0.08f) else CardCharcoal)
            .border(
                1.dp,
                if (isCurrent) primaryColor.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelectTrack(track) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini record disc icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Text metadata column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title ?: "Unknown Track",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist ?: "Unknown Artist",
                color = TextGray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // BPM & Key tags
            if (track.bpm != null || !track.musicKey.isNullOrBlank() || !track.folder.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!track.folder.isNullOrBlank()) {
                        MiniBadge("📁 ${track.folder}", color = MaterialTheme.colorScheme.secondary)
                    }
                    if (track.bpm != null) {
                        MiniBadge("${track.bpm} BPM")
                    }
                    if (!track.musicKey.isNullOrBlank()) {
                        MiniBadge(track.musicKey)
                    }
                }
            }
        }

        // Star Bookmark Toggle Button
        IconButton(
            onClick = { onToggleFavorite(track) },
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) secondaryColor else TextGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun FolderList(
    folders: List<String>,
    favoriteList: List<TrackEntity>,
    currentTrack: TrackEntity?,
    primaryColor: Color,
    secondaryColor: Color,
    onSelectTrack: (TrackEntity) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit
) {
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(folders) { folderName ->
            val isExpanded = expandedFolders[folderName] ?: false
            val tracksInFolder = remember(favoriteList, folderName) {
                favoriteList.filter { it.folder == folderName }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardCharcoal)
                    .border(
                        1.dp,
                        if (isExpanded) primaryColor.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                // Folder Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFolders[folderName] = !isExpanded }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Folder",
                            tint = secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = folderName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tracksInFolder.size.toString(),
                                color = TextGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle",
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expanded Tracks List
                if (isExpanded) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tracksInFolder.isEmpty()) {
                            Text(
                                text = "Папка пуста",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            tracksInFolder.forEach { track ->
                                TrackItem(
                                    track = track,
                                    currentTrack = currentTrack,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    onSelectTrack = onSelectTrack,
                                    onToggleFavorite = onToggleFavorite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeader(
    label: String,
    isSelected: Boolean,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) primaryColor else TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = if (isSelected) secondaryColor else TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MiniBadge(text: String, color: Color = TextGray) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
