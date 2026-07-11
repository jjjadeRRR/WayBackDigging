package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrackEntity
import com.example.ui.theme.CardCharcoal
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsView(
    track: TrackEntity?,
    onSaveNotes: (TrackEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember(track?.id) { mutableStateOf(track?.userNotes ?: "") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Source Header
        val primaryColor = MaterialTheme.colorScheme.primary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(primaryColor.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudQueue,
                contentDescription = "Archive.org Database",
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ДАННЫЕ ИЗ ARCHIVE.ORG",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (track == null) {
            // Empty placeholder state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "No Track",
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Трек еще не загружен.",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Нажмите ИСКАТЬ чтобы найти трек!",
                        color = primaryColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Track Metadata Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardCharcoal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = track.title ?: "Неизвестное название",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "исполнитель: ${track.artist ?: "Неизвестный исполнитель"}",
                        color = primaryColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Detail items
                    MetadataItem("Альбом", track.album ?: "Архив Интернета")
                    MetadataItem("Год", track.year?.toString() ?: "Без даты")
                    MetadataItem("Прослушивания", String.format("%,d", track.views))
                    MetadataItem("Коллекция", track.collection?.replace(",", ", ") ?: "Н/Д")
                    MetadataItem("BPM (Темп)", track.bpm?.let { "$it BPM" } ?: "Не анализировался")
                    MetadataItem("Тональность", track.musicKey ?: "Не анализировался")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Classification Tags Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardCharcoal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "КЛАССИФИКАЦИОННЫЕ ТЕГИ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ClassificationTagsSection("Жанры", track.genres ?: "Н/Д")
                    Spacer(modifier = Modifier.height(8.dp))
                    ClassificationTagsSection("Стили", track.styles ?: "Н/Д")
                    Spacer(modifier = Modifier.height(8.dp))
                    ClassificationTagsSection("Регионы", track.regions ?: "США / Весь мир")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Digging Notes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardCharcoal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = "User Notes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ЗАМЕТКИ",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = {
                            Text(
                                "Добавьте лог сэмпла (например, 'Отличный луп во вступлении с 0:12')",
                                color = TextGray.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = TextGray,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onSaveNotes(track, noteText)
                            focusManager.clearFocus()
                        })
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onSaveNotes(track, noteText)
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassificationTagsSection(label: String, value: String) {
    val cleanTags = value.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("[") && !it.contains("-") && it.length < 25 }
        .distinct()
        .take(12)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (cleanTags.isEmpty() || value == "Н/Д") {
            Text("Н/Д", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                cleanTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
