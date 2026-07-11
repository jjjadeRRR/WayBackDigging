package com.example.ui.components
import androidx.compose.ui.graphics.graphicsLayer

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrackEntity
import com.example.ui.theme.CardCharcoal
import com.example.ui.theme.TextGray
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun CenterPlayerView(
    track: TrackEntity?,
    isPlaying: Boolean,
    playbackProgress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    isLoading: Boolean,
    statusMessage: String,
    onTogglePlayback: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onStartScrubbing: () -> Unit,
    onScrubToFraction: (Float) -> Unit,
    onEndScrubbing: (Float) -> Unit,
    onShuffleClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSaveToFolderClick: () -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: androidx.compose.foundation.pager.PagerState? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Standby phrases picker
    val standbyPhrase = remember(track) {
        if (track == null) {
            listOf(
                "Винил в ожидании поиска",
                "Пластинка ждет своего часа...",
                "Готов к поиску винтажных сэмплов",
                "Игла застыла над чистым винилом",
                "Какой раритет найдем сегодня?",
                "Сдуй пыль с пластинки и начни копать!",
                "Время найти золотой сэмпл!"
            ).random()
        } else {
            ""
        }
    }

    // Infinite rotation animation for Vinyl Record
    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val currentRotation = if (isPlaying) rotationAngle else 0f

    // Dynamic scratch gesture states
    var manualRotationOffset by remember { mutableStateOf(0f) }
    var isDraggingVinyl by remember { mutableStateOf(false) }

    val displayRotation = if (isDraggingVinyl) {
        manualRotationOffset
    } else {
        currentRotation + manualRotationOffset
    }

    // Stylus / Tonearm pivot angle depending on playback state and progress
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying || isDraggingVinyl) {
            18f + (16f * playbackProgress)
        } else {
            10f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tonearmAngle"
    )

    val currentProgressState = rememberUpdatedState(playbackProgress)
    var showActionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Bar at Top
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = statusMessage.uppercase(),
                    color = if (isLoading) MaterialTheme.colorScheme.primary else TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculate parallax/shared element offset based on pagerState
        val density = androidx.compose.ui.platform.LocalDensity.current
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
        
        val page1Offset = pagerState?.let {
            (it.currentPage - 1) + it.currentPageOffsetFraction
        } ?: 0f

        // Vinyl Record & Turntable Canvas Deck with interactive dragging/scratching
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .graphicsLayer {
                    if (pagerState != null && page1Offset != 0f) {
                        // Offset applied by Pager to this page is `-page1Offset * screenWidthPx`.
                        // To stick to screen: counteract with `page1Offset * screenWidthPx`.
                        val stickX = page1Offset * screenWidthPx
                        
                        if (page1Offset < 0f) {
                            // Swiping towards Info (Page 0)
                            // Absolute progress from 0 to 1
                            val progress = kotlin.math.abs(page1Offset)
                            
                            // Target: Scale down to 0.4, move to Top-Right
                            scaleX = 1f - (0.6f * progress)
                            scaleY = 1f - (0.6f * progress)
                            
                            // Move to top-right.
                            val targetX = screenWidthPx * 0.25f // move right
                            val targetY = -(this.size.height) * 0.35f // move up
                            
                            translationX = stickX + (targetX * progress)
                            translationY = targetY * progress
                            alpha = 1f
                        } else if (page1Offset > 0f) {
                            // Swiping towards History (Page 2)
                            // Fade out and translate left slightly
                            val progress = page1Offset
                            translationX = stickX - (screenWidthPx * 0.5f * progress)
                            alpha = 1f - progress
                            scaleX = 1f - (0.2f * progress)
                            scaleY = 1f - (0.2f * progress)
                        }
                    }
                }
                .aspectRatio(1.0f)
                .clip(RoundedCornerShape(16.dp))
                .background(CardCharcoal)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .pointerInput(track) {
                    if (track == null) return@pointerInput
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        onStartScrubbing()
                        isDraggingVinyl = true
                        
                        var lastAngle = getAngle(down.position.x, down.position.y, centerX, centerY)
                        
                        var hasMovedSignificantly = false
                        var accumulatedDrag = 0f
                        var currentProgress = currentProgressState.value 

                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) {
                                // Release!
                                isDraggingVinyl = false
                                onEndScrubbing(currentProgress)
                                
                                // If it was just a quick tap without significant dragging, trigger play/pause!
                                if (!hasMovedSignificantly) {
                                    onTogglePlayback()
                                }
                                break
                            }

                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val currentAngle = getAngle(change.position.x, change.position.y, centerX, centerY)
                                var delta = currentAngle - lastAngle
                                if (delta > 180) delta -= 360
                                if (delta < -180) delta += 360

                                accumulatedDrag += Math.abs(delta.toFloat())
                                if (accumulatedDrag > 8f) { // threshold for slop to distinguish tap from drag
                                    hasMovedSignificantly = true
                                }

                                if (hasMovedSignificantly) {
                                    // 1. Physically rotate vinyl
                                    manualRotationOffset += delta.toFloat()

                                    // 2. Seek music (360 degrees = 5% of the track length for nice resolution)
                                    val sensitivity = 0.05f
                                    currentProgress = (currentProgress + (delta.toFloat() / 360f) * sensitivity).coerceIn(0f, 1f)
                                    onScrubToFraction(currentProgress)

                                    // 3. Haptic clicks while spinning
                                    if (Math.abs(delta) > 2.5) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                
                                lastAngle = currentAngle
                                change.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Audio-reactive pulsating gradient (Halo effect)
            val infiniteTransitionPulse = rememberInfiniteTransition(label = "BassPulse")
            val rawPulse by infiniteTransitionPulse.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rawPulse"
            )
            
            // Smoothly ramp up and down the pulse based on play state
            val playIntensity by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPlaying) 1f else 0f,
                animationSpec = tween(800),
                label = "playIntensity"
            )
            
            val currentPulse = rawPulse * playIntensity
            val pulseScale = 0.9f + (0.15f * currentPulse) // 90% to 105% width
            val pulseAlpha = 0.05f + (0.20f * currentPulse) // 5% to 25% opacity
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
            ) {
                val radius = size.minDimension / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFCCCCFF).copy(alpha = pulseAlpha), // Pale lavender center
                            Color(0xFF301A4F).copy(alpha = pulseAlpha * 0.8f), // Deep eggplant
                            Color(0xFF202124).copy(alpha = 0f) // Fade to transparent background
                        ),
                        center = centerOffset,
                        radius = radius
                    ),
                    center = centerOffset,
                    radius = radius
                )
            }

            // Concentric metal ring of the turntable plate
            Canvas(modifier = Modifier.fillMaxSize(0.88f)) {
                drawCircle(
                    color = Color(0xFF282828),
                    style = Stroke(width = 8f)
                )
            }

            // The Vinyl record drawing
            Canvas(
                modifier = Modifier.fillMaxSize(0.82f)
            ) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                val vinylRadius = size.minDimension / 2

                withTransform({
                    rotate(displayRotation, centerOffset)
                }) {
                    // Main black vinyl circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF222222), Color(0xFF0A0A0A)),
                            center = centerOffset,
                            radius = vinylRadius
                        ),
                        radius = vinylRadius
                    )

                    // Concentric sound grooves (physical ridges)
                    for (i in 4..16 step 2) {
                        drawCircle(
                            color = Color(0xFF252525),
                            radius = vinylRadius * (i / 18f),
                            style = Stroke(width = 1f)
                        )
                    }

                    // Dynamic Center Label (uses primary theme color)
                    drawCircle(
                        color = primaryColor,
                        radius = vinylRadius * 0.32f
                    )
                    drawCircle(
                        color = secondaryColor,
                        radius = vinylRadius * 0.28f,
                        style = Stroke(width = 1f)
                    )

                    // Inner Spindle core ring
                    drawCircle(
                        color = Color(0xFF1E1E1E),
                        radius = vinylRadius * 0.08f
                    )
                }

                // Tiny silver spindle pin in absolute center
                drawCircle(
                    color = Color(0xFFCCCCCC),
                    radius = vinylRadius * 0.025f
                )
                drawCircle(
                    color = Color.White,
                    radius = vinylRadius * 0.015f
                )
            }

            // Physical Tonearm stylus drawn overlaid in upper-right corner
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val pivotOffset = Offset(size.width * 0.85f, size.height * 0.15f)

                // Draw pivot base
                drawCircle(
                    color = Color(0xFF444444),
                    radius = 24f,
                    center = pivotOffset
                )
                drawCircle(
                    color = Color(0xFF111111),
                    radius = 12f,
                    center = pivotOffset
                )

                // Rotate the tonearm stick based on simulated rotation state
                withTransform({
                    rotate(tonearmAngle, pivotOffset)
                }) {
                    // Draw metal tonearm stick
                    drawLine(
                        color = Color(0xFF888888),
                        start = pivotOffset,
                        end = Offset(size.width * 0.45f, size.height * 0.72f),
                        strokeWidth = 6f
                    )

                    // Draw cartridge/needle weight head at end of stick
                    val headOffset = Offset(size.width * 0.45f, size.height * 0.72f)
                    drawCircle(
                        color = Color(0xFF222222),
                        radius = 10f,
                        center = headOffset
                    )
                    // Draw little active indicator on stylus cartridge using theme colors
                    drawCircle(
                        color = if (isPlaying || isDraggingVinyl) secondaryColor else Color.Gray,
                        radius = 4f,
                        center = headOffset
                    )
                }
            }

            // Scratch mode toggle removed
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Header Details
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title banner
                Text(
                    text = track?.title ?: standbyPhrase,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Normal Slider Seek Bar
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Slider(
                    value = playbackProgress,
                    onValueChange = { newProgress ->
                        onStartScrubbing()
                        onScrubToFraction(newProgress)
                    },
                    onValueChangeFinished = {
                        onEndScrubbing(playbackProgress)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatTime(durationMs),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Play Controls Grid with swapped buttons & Dynamic Material You styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Add to Favorites / Bookmark toggle (replaces search button)
            IconButton(
                onClick = {
                    if (track != null) {
                        onToggleFavorite(track)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        Toast.makeText(context, "Сначала найдите и запустите трек!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (track?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Добавить в избранное",
                    tint = if (track?.isFavorite == true) MaterialTheme.colorScheme.primary else Color.White
                )
            }

            // Button 2: Save to folder toggle (swapped with filters)
            IconButton(
                onClick = {
                    onSaveToFolderClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.CreateNewFolder,
                    contentDescription = "Сохранить в папку",
                    tint = Color.White
                )
            }

            // Button 3: Main Play/Pause Button in absolute center (Material You primary style)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape) // Shadow FIRST to display beautiful Material depth!
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .clickable {
                        onTogglePlayback()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Воспроизведение / Пауза",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Button 4: Filters popup toggle (swapped with folder)
            IconButton(
                onClick = {
                    onFilterClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Настройка фильтров",
                    tint = Color.White
                )
            }

            // Button 5: Combined actions (Share & Download)
            Box {
                IconButton(
                    onClick = {
                        if (track != null) {
                            showActionMenu = true
                        } else {
                            Toast.makeText(context, "Сначала найдите и запустите трек!", Toast.LENGTH_SHORT).show()
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Скачать",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showActionMenu,
                    onDismissRequest = { showActionMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Поделиться ссылкой", color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            showActionMenu = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (track?.audioUrl != null) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Archive Track Link", track.audioUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Прямая ссылка скопирована в буфер обмена!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Скачать трек (.mp3)", color = Color.White, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            showActionMenu = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (track != null) {
                                downloadTrack(context, track)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "ShuffleGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
        // Random text change logic
        var shuffleText by remember { mutableStateOf("ИСКАТЬ СЭМПЛ") }
        LaunchedEffect(Unit) {
            val texts = listOf("ИСКАТЬ СЭМПЛ", "МНЕ ПОВЕЗЕТ", "ВПЕРЕД", "ПОЕХАЛИ")
            while (true) {
                kotlinx.coroutines.delay((5000L..15000L).random())
                shuffleText = texts.random()
            }
        }

        // Giant Shuffle/Dig Button
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.5f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                .clickable {
                    onShuffleClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            contentAlignment = Alignment.Center
        ) {
            // Pulse overlay inside the button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(primaryColor.copy(alpha = glowAlpha * 0.15f), RoundedCornerShape(24.dp))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Casino,
                    contentDescription = "Найти трек",
                    tint = Color.White.copy(alpha = 0.8f + (glowAlpha * 0.2f)),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.animation.AnimatedContent(
                    targetState = shuffleText,
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(animationSpec = tween(500)) togetherWith 
                        androidx.compose.animation.fadeOut(animationSpec = tween(500))
                    },
                    label = "ShuffleText"
                ) { targetText ->
                    Text(
                        text = targetText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

private fun getAngle(x: Float, y: Float, centerX: Float, centerY: Float): Double {
    val dx = x - centerX
    val dy = y - centerY
    return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
}

private fun downloadTrack(context: Context, track: TrackEntity) {
    val audioUrl = track.audioUrl
    if (audioUrl.isNullOrBlank()) {
        Toast.makeText(context, "Ссылка на трек отсутствует", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(audioUrl)
        
        val safeTitle = (track.title ?: "Track_${track.id}")
            .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .trim()
        val fileName = "$safeTitle.mp3"
        
        val request = DownloadManager.Request(uri)
            .setTitle(track.title ?: "WayBack Track")
            .setDescription("Скачивание в Music/WayBackDigging")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "WayBackDigging/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            
        downloadManager.enqueue(request)
        Toast.makeText(context, "Скачивание началось. Файл сохранится в Music/WayBackDigging", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка скачивания: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
