package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardCharcoal
import com.example.ui.theme.PinkNeon
import com.example.ui.theme.TextGray

import com.example.ui.viewmodel.DiggerViewModel.FilterPreset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    genre: String,
    onGenreChange: (String) -> Unit,
    style: String,
    onStyleChange: (String) -> Unit,
    region: String,
    onRegionChange: (String) -> Unit,
    musicKey: String,
    onMusicKeyChange: (String) -> Unit,
    minBpm: Float,
    maxBpm: Float,
    onBpmRangeChange: (Float, Float) -> Unit,
    minYear: Float,
    maxYear: Float,
    onYearRangeChange: (Float, Float) -> Unit,
    minViews: Float,
    onMinViewsChange: (Float) -> Unit,
    onlyTopicChannels: Boolean,
    onOnlyTopicChannelsChange: (Boolean) -> Unit,
    tagsText: String,
    onTagsTextChange: (String) -> Unit,
    savedPresets: List<FilterPreset>,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (FilterPreset) -> Unit,
    onResetFilters: () -> Unit
) {
    if (!showDialog) return

    val genres = FilterConstants.GENRES
    val styles = FilterConstants.STYLES
    val regions = FilterConstants.REGIONS
    val keysList = listOf(
        "All", "C Major", "C Minor", "C♯ Major", "C♯ Minor", "D Major", "D Minor", "D♯ Major", "D♯ Minor",
        "E Major", "E Minor", "F Major", "F Minor", "F♯ Major", "F♯ Minor", "G Major", "G Minor",
        "G♯ Major", "G♯ Minor", "A Major", "A Minor", "A♯ Major", "A♯ Minor", "B Major", "B Minor"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ФИЛЬТРЫ",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть фильтры",
                            tint = Color.White
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                
                var localGenre by remember(genre) { mutableStateOf(genre) }
                var localStyle by remember(style) { mutableStateOf(style) }
                var localRegion by remember(region) { mutableStateOf(region) }
                var localKey by remember(musicKey) { mutableStateOf(musicKey) }
                var localOnlyTopicChannels by remember(onlyTopicChannels) { mutableStateOf(onlyTopicChannels) }
                var localTagsText by remember(tagsText) { mutableStateOf(tagsText) }

                var localMinBpmInput by remember(minBpm) { mutableStateOf(if (minBpm <= 0f) "" else minBpm.toInt().toString()) }
                var localMaxBpmInput by remember(maxBpm) { mutableStateOf(if (maxBpm <= 0f) "" else maxBpm.toInt().toString()) }
                var localMinYearInput by remember(minYear) { mutableStateOf(if (minYear <= 0f) "" else minYear.toInt().toString()) }
                var localMaxYearInput by remember(maxYear) { mutableStateOf(if (maxYear <= 0f) "" else maxYear.toInt().toString()) }
                var localMinViews by remember(minViews) { mutableStateOf(minViews.toInt().toString()) }

                var isSavingPreset by remember { mutableStateOf(false) }
                var newPresetName by remember { mutableStateOf("") }
                var selectedTab by remember { mutableStateOf(0) } // 0 = Filters, 1 = Presets
                
                // Custom Tab Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabHeader("ФИЛЬТРЫ", selectedTab == 0, count = 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                    TabHeader("ПРЕСЕТЫ", selectedTab == 1, count = savedPresets.size, modifier = Modifier.weight(1f)) { selectedTab = 1 }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 1) {
                    // PRESETS TAB
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (savedPresets.isEmpty()) {
                            Text("У вас пока нет сохраненных пресетов", color = TextGray, fontSize = 12.sp)
                        } else {
                            savedPresets.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .clickable { 
                                            onApplyPreset(preset)
                                            onDismissRequest()
                                        }
                                        .padding(16.dp)
                                ) {
                                    Text(preset.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    // FILTERS TAB
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                    // Tag search
                    Text("ТЕГИ / КЛЮЧЕВЫЕ СЛОВА (через пробел)", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = localTagsText,
                        onValueChange = { localTagsText = it },
                        placeholder = { Text("Введите теги (например, '1970 труба басс')", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Topic Channel toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardCharcoal)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Только тематические каналы",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Ограничивает поиск качественными архивными винтажными записями",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = localOnlyTopicChannels,
                            onCheckedChange = { localOnlyTopicChannels = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Selectors
                    MultiSelectDropdown("ЖАНР", genres, localGenre) { localGenre = it }
                    Spacer(modifier = Modifier.height(14.dp))

                    MultiSelectDropdown("СТИЛЬ", styles, localStyle) { localStyle = it }
                    Spacer(modifier = Modifier.height(14.dp))

                    MultiSelectDropdown("РЕГИОН", regions, localRegion) { localRegion = it }
                    Spacer(modifier = Modifier.height(14.dp))

                    MultiSelectDropdown("ТОНАЛЬНОСТЬ", keysList, localKey) { localKey = it }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Ranges
                    Text(
                        text = "ТЕМП (BPM)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = localMinBpmInput,
                            onValueChange = { newValue ->
                                localMinBpmInput = newValue.filter { it.isDigit() }
                            },
                            label = { Text("От", color = TextGray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                        Text("-", color = Color.White, fontSize = 16.sp)
                        OutlinedTextField(
                            value = localMaxBpmInput,
                            onValueChange = { newValue ->
                                localMaxBpmInput = newValue.filter { it.isDigit() }
                            },
                            label = { Text("До", color = TextGray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ФИЛЬТР ГОДА",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = localMinYearInput,
                            onValueChange = { newValue ->
                                localMinYearInput = newValue.filter { it.isDigit() }
                            },
                            label = { Text("От", color = TextGray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                        Text("-", color = Color.White, fontSize = 16.sp)
                        OutlinedTextField(
                            value = localMaxYearInput,
                            onValueChange = { newValue ->
                                localMaxYearInput = newValue.filter { it.isDigit() }
                            },
                            label = { Text("До", color = TextGray, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "МИНИМУМ ПРОСЛУШИВАНИЙ (Archive.org Views)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = localMinViews,
                        onValueChange = { newValue ->
                            localMinViews = newValue.filter { it.isDigit() }
                        },
                        placeholder = { Text("Например, 100", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
            }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                // Bottom toolbar: redesigned for perfect, clean alignment
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSavingPreset) {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            placeholder = { Text("Имя пресета", color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (newPresetName.isNotBlank()) {
                                        onSavePreset(newPresetName.trim())
                                        isSavingPreset = false
                                        newPresetName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Сохранить пресет")
                            }
                            OutlinedButton(
                                onClick = { isSavingPreset = false },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Отмена")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                onGenreChange(localGenre)
                                onStyleChange(localStyle)
                                onRegionChange(localRegion)
                                onMusicKeyChange(localKey)
                                onOnlyTopicChannelsChange(localOnlyTopicChannels)
                                onTagsTextChange(localTagsText)

                                val minBpmVal = localMinBpmInput.toFloatOrNull() ?: 0f
                                val maxBpmVal = localMaxBpmInput.toFloatOrNull() ?: 0f
                                onBpmRangeChange(minBpmVal, maxBpmVal)

                                val minYearVal = localMinYearInput.toFloatOrNull() ?: 0f
                                val maxYearVal = localMaxYearInput.toFloatOrNull() ?: 0f
                                onYearRangeChange(minYearVal, maxYearVal)

                                val minViewsVal = localMinViews.toFloatOrNull() ?: 0f
                                onMinViewsChange(minViewsVal)

                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Применить фильтры", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isSavingPreset = true },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Save, contentDescription = "Save Preset", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сохранить", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onResetFilters,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сбросить", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
