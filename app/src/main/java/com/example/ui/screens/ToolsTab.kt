package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import com.example.ui.RosterViewModel

@Composable
fun ToolsTab(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var importText by remember { mutableStateOf("") }
    var newDriverName by remember { mutableStateOf("") }
    var newVehicleId by remember { mutableStateOf("") }
    var newShifts by remember { mutableStateOf("") }

    var showResetConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FA))
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Heading
            Text(
                text = "ALAT & SINKRONISASI GOOGLE SHEETS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF6750A4),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Google Sheets Offline First Sync Section
            GoogleSheetsSyncSection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // Import Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().testTag("import_section_card")
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tempel & Impor Roster Baru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1B1F)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tempel spreadsheet roster (format dipisahkan tab) untuk mengganti seluruh database.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { 
                            Text(
                                text = "Tempel di sini:\nNAMA_DRIVER\tID_TRUK\tD\tN\tOFF\tCR...",
                                color = Color(0xFF49454F),
                                fontSize = 12.sp
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_text_field")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (importText.trim().isEmpty()) {
                                Toast.makeText(context, "Harap tempel data roster terlebih dahulu", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = viewModel.parseAndImportRoster(importText)
                                if (success) {
                                    Toast.makeText(context, "Roster berhasil diimpor!", Toast.LENGTH_LONG).show()
                                    importText = ""
                                    viewModel.setActiveTab("My Shift") // Redirect to My Shift tab to see updates
                                } else {
                                    Toast.makeText(context, "Kesalahan: Format roster tidak valid.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("import_submit_button")
                    ) {
                        Text("IMPOR & TULIS ULANG DATABASE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Driver Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tambah Driver Individu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1B1F)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newDriverName,
                        onValueChange = { newDriverName = it },
                        label = { Text("Nama Driver") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_driver_name")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newVehicleId,
                        onValueChange = { newVehicleId = it },
                        label = { Text("ID Kendaraan / Truk (misal: DT HANVAN 99)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_vehicle_id")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newShifts,
                        onValueChange = { newShifts = it },
                        label = { Text("30 Shift (dipisahkan koma, misal: D,N,OFF...)") },
                        placeholder = { Text("D,N,N,OFF,CR,D,D,N,N,OFF...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_shifts_field")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newDriverName.trim().isEmpty() || newVehicleId.trim().isEmpty() || newShifts.trim().isEmpty()) {
                                Toast.makeText(context, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addRoster(newDriverName, newVehicleId, newShifts)
                                Toast.makeText(context, "Driver berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                newDriverName = ""
                                newVehicleId = ""
                                newShifts = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("add_driver_submit_button")
                    ) {
                        Text("SIMPAN DRIVER KE DATABASE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Factory Reset
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFEF5350))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pulihkan Roster Default",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1B1F)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reset semua roster kembali ke data awal 94 driver bawaan sistem.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showResetConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("factory_reset_button")
                    ) {
                        Text("RESET DATABASE KE DEFAULT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Factory Reset Confirmation Dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Database ke Default?", color = Color(0xFF1C1B1F)) },
            text = { Text("Tindakan ini akan menghapus semua jadwal kustom dan memuat kembali data awal 94 driver bawaan.", color = Color(0xFF49454F)) },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefault()
                        viewModel.setMyDriver(null) // Reset selected profile
                        showResetConfirmation = false
                        Toast.makeText(context, "Database berhasil dipulihkan ke default!", Toast.LENGTH_SHORT).show()
                        viewModel.setActiveTab("My Shift")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("YA, RESET", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("BATAL", color = Color(0xFF6750A4))
                }
            }
        )
    }
}
