package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoogleSheetsSyncManager
import com.example.data.SyncState
import com.example.ui.RosterViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleSheetsSyncSection(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isOnline by viewModel.isOnline.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val sheetUrl by viewModel.sheetUrl.collectAsState()
    val webhookUrl by viewModel.webhookUrl.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    val conflictPolicy by viewModel.conflictPolicy.collectAsState()
    val allRosters by viewModel.allRosters.collectAsState()

    var editSheetUrl by remember(sheetUrl) { mutableStateOf(sheetUrl) }
    var editWebhookUrl by remember(webhookUrl) { mutableStateOf(webhookUrl) }
    var editAutoSync by remember(isAutoSyncEnabled) { mutableStateOf(isAutoSyncEnabled) }
    var editConflictPolicy by remember(conflictPolicy) { mutableStateOf(conflictPolicy) }

    var showConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Status & Primary Action Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("google_sheets_status_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOnline) "ONLINE - SINKRON GOOGLE SHEETS" else "OFFLINE FIRST (ROOM DB)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = if (isOnline) "Tersambung ke Google Sheets API" else "Aplikasi aktif penuh dengan SQLite/Room Lokal",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshNetworkStatus() },
                        modifier = Modifier.testTag("refresh_network_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cek Koneksi",
                            tint = Color(0xFF6750A4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Info Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Driver Lokal: ${allRosters.size}", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFF3EDF7),
                            labelColor = Color(0xFF1D192B)
                        ),
                        border = null
                    )
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Terakhir Sync: $lastSyncTime", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFFF3EDF7),
                            labelColor = Color(0xFF1D192B)
                        ),
                        border = null
                    )
                    if (pendingSyncCount > 0) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Pending Sync: $pendingSyncCount perubahan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFFFEBEE),
                                labelColor = Color(0xFFC62828)
                            ),
                            border = null
                        )
                    }
                }

                // Sync State Message Display
                when (val state = syncState) {
                    is SyncState.Syncing -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF6750A4),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Menyinkronkan data dengan Google Sheets...",
                                fontSize = 12.sp,
                                color = Color(0xFF1D192B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is SyncState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is SyncState.ErrorOffline -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Mode Offline-First Aktif",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                fontSize = 11.sp,
                                color = Color(0xFFBF360C)
                            )
                        }
                    }
                    is SyncState.ErrorNetwork -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = Color(0xFFB71C1C),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pull Sync Button
                    Button(
                        onClick = { viewModel.fetchAndSyncFromGoogleSheets() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pull_sync_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TARIK SHEETS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Push Sync Button
                    OutlinedButton(
                        onClick = { viewModel.pushLocalDataToGoogleSheets() },
                        border = BorderStroke(1.dp, Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("push_sync_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KIRIM SHEETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auxiliary Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("open_sync_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pengaturan URL & Policy", fontSize = 11.sp, color = Color(0xFF6750A4))
                    }

                    TextButton(
                        onClick = {
                            val csvData = viewModel.exportLocalToCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Roster CSV", csvData)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Format spreadsheet roster disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_csv_format_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salin Format CSV", fontSize = 11.sp, color = Color(0xFF6750A4))
                    }
                }
            }
        }
    }

    // Settings Modal Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengaturan Google Sheets Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("URL Google Sheets (Publik / Spreadsheet ID):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editSheetUrl,
                        onValueChange = { editSheetUrl = it },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit", fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sheet_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { editSheetUrl = GoogleSheetsSyncManager.DEFAULT_SHEET_URL },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Gunakan Sheet Contoh Hanvan", fontSize = 11.sp, color = Color(0xFF6750A4))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("URL Webhook Apps Script (Opsional untuk Push):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editWebhookUrl,
                        onValueChange = { editWebhookUrl = it },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("webhook_url_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = editAutoSync,
                            onCheckedChange = { editAutoSync = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Auto-Sync saat Online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                            Text("Otomatis cek & perbarui roster dari Google Sheets", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Kebijakan Penanganan Konflik:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = editConflictPolicy == "OFFLINE_FIRST",
                            onClick = { editConflictPolicy = "OFFLINE_FIRST" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6750A4))
                        )
                        Text("Offline-First (Utamakan Local Room DB)", fontSize = 11.sp, color = Color(0xFF1C1B1F))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = editConflictPolicy == "SHEETS_FIRST",
                            onClick = { editConflictPolicy = "SHEETS_FIRST" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6750A4))
                        )
                        Text("Google Sheets First (Timpa lokal saat sync)", fontSize = 11.sp, color = Color(0xFF1C1B1F))
                    }
                }
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSyncConfig(editSheetUrl, editWebhookUrl, editAutoSync, editConflictPolicy)
                        showConfigDialog = false
                        Toast.makeText(context, "Pengaturan Google Sheets disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("SIMPAN PENGATURAN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("BATAL", color = Color(0xFF6750A4))
                }
            }
        )
    }
}
