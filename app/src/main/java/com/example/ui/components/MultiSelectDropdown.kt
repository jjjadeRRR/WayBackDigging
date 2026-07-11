package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardCharcoal
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    title: String,
    itemsList: List<String>,
    selectedValues: String, // Pipe-separated string: "Jazz|Funk"
    onValuesSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val selectedSet = remember(selectedValues) {
        selectedValues.split("|").filter { it.isNotBlank() && it != "All" }.toSet()
    }
    
    val displayString = if (selectedSet.isEmpty()) "Все" else selectedSet.joinToString(", ")
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = TextGray, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
            OutlinedTextField(
                value = displayString,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color.White.copy(alpha = 0.2f),
                    disabledTextColor = Color.White,
                    disabledTrailingIconColor = Color.White
                )
            )
        }
        
        if (showDialog) {
            Dialog(
                onDismissRequest = { showDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                var searchQuery by remember { mutableStateOf("") }
                
                val filteredItems = remember(searchQuery, itemsList) {
                    if (searchQuery.isBlank()) {
                        itemsList.filter { it != "All" }
                    } else {
                        itemsList.filter { it != "All" && it.contains(searchQuery, ignoreCase = true) }
                    }
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск...", color = TextGray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onValuesSelected("All")
                                            showDialog = false
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedSet.isEmpty(),
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Все", color = Color.White, fontSize = 16.sp)
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                            
                            items(filteredItems) { item ->
                                val isChecked = selectedSet.contains(item)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = if (isChecked) selectedSet - item else selectedSet + item
                                            if (newSet.isEmpty()) {
                                                onValuesSelected("All")
                                            } else {
                                                onValuesSelected(newSet.joinToString("|"))
                                            }
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(item, color = Color.White, fontSize = 16.sp)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Готово", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
