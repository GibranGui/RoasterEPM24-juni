package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.RosterEntity
import com.example.ui.RosterStats
import com.example.ui.RosterViewModel

// Custom colors for shift states
val ShiftDayColor = Color(0xFFFF9800) // Safety Orange
val ShiftDayBg = Color(0xFFFFF3E0)
val ShiftNightColor = Color(0xFF673AB7) // Midnight Purple
val ShiftNightBg = Color(0xFFF3E5F5)
val ShiftOffColor = Color(0xFF4CAF50) // Rest Green
val ShiftOffBg = Color(0xFFE8F5E9)
val ShiftLeaveColor = Color(0xFFE53935) // Leave Red
val ShiftLeaveBg = Color(0xFFFFEBEE)
val ShiftOtherBg = Color(0xFFECEFF1)
val ShiftOtherColor = Color(0xFF37474F)

@Composable
fun MyShiftTab(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val myDriverName by viewModel.myDriverName.collectAsState()
    val myRoster by viewModel.myRoster.collectAsState()
    val allRosters by viewModel.allRosters.collectAsState()

    var showSelectorDialog by remember { mutableStateOf(false) }
    var selectorSearchText by remember { mutableStateOf("") }

    if (myDriverName == null) {
        // Welcome Screen / Profile Selector
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF7F2FA)) // Clean Minimalism Light Background
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hanvan_truck),
                        contentDescription = "Hanvan Truck",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "SELAMAT DATANG DI ROSTER ARMADA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4),
                            letterSpacing = 1.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pilih nama driver Anda untuk melihat dan mengelola jadwal shift kabin pribadi Anda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showSelectorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("select_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PILIH NAMA SAYA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Driver Logged-in Screen
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF7F2FA))
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Personalized Banner Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("my_shift_card")
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hanvan_truck),
                                contentDescription = "Hanvan Truck Background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient Overlay for readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                                            startY = 50f
                                        )
                                    )
                             )
                            // Driver name and truck badge
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = myDriverName ?: "",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = myRoster?.vehicleId ?: "UNASSIGNED",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD0BCFF)
                                        )
                                    )
                                }
                            }

                            // Change Profile Button icon
                            IconButton(
                                onClick = { showSelectorDialog = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color(0x80000000), CircleShape)
                                    .testTag("change_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Change Profile",
                                    tint = Color.White
                                )
                            }
                        }

                        // Today's Shift Status
                        if (myRoster != null) {
                            val roster = myRoster!!
                            val shifts = roster.getShiftsList()
                            val todayShift = if (viewModel.todayDayNumber <= shifts.size) shifts[viewModel.todayDayNumber - 1] else "OFF"
                            val tomorrowShift = if (viewModel.todayDayNumber < shifts.size) shifts[viewModel.todayDayNumber] else "OFF"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Today Block
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SHIFT HARI INI (HARI ${viewModel.todayDayNumber})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF49454F)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ShiftBadgeBig(shiftCode = todayShift)
                                }

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(70.dp)
                                        .background(Color(0xFFCAC4D0))
                                        .align(Alignment.CenterVertically)
                                )

                                // Tomorrow Block
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "SHIFT BESOK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF49454F)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ShiftBadgeBig(shiftCode = tomorrowShift)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Dashboard Block
                if (myRoster != null) {
                    val stats = viewModel.getRosterStats(myRoster!!)
                    Text(
                        text = "STATISTIK ANDA • ${viewModel.calendarMonth}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(title = "Hari Kerja", value = "${stats.totalWorkDays}", iconColor = Color(0xFFFF9800), modifier = Modifier.weight(1f))
                        StatCard(title = "Siang (D)", value = "${stats.dayShifts}", iconColor = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                        StatCard(title = "Malam (N)", value = "${stats.nightShifts}", iconColor = Color(0xFF9575CD), modifier = Modifier.weight(1f))
                        StatCard(title = "OFF", value = "${stats.offDays}", iconColor = Color(0xFF81C784), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(title = "Cuti Roster (CR)", value = "${stats.leaveRoster}", iconColor = Color(0xFFEF5350), modifier = Modifier.weight(1f))
                        StatCard(title = "Cuti Tahunan (CT)", value = "${stats.leaveAnnual}", iconColor = Color(0xFFE57373), modifier = Modifier.weight(1f))
                        StatCard(title = "Induksi (I)", value = "${stats.permittedAbsence}", iconColor = Color(0xFF64B5F6), modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Calendar Heading
                Text(
                    text = "KALENDER KABIN SAYA • ${viewModel.calendarMonth}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6750A4),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Calendar Grid
                if (myRoster != null) {
                    CalendarGrid(
                        roster = myRoster!!,
                        todayDay = viewModel.todayDayNumber,
                        onDayClick = { dayIndex ->
                            // Optional: support simple quick shift editing on cell tap!
                        }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Driver Selection Dialog
    if (showSelectorDialog) {
        Dialog(onDismissRequest = { showSelectorDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF7F2FA)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Profil Anda",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1C1B1F)
                        )
                        IconButton(onClick = { showSelectorDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1C1B1F))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search text field
                    OutlinedTextField(
                        value = selectorSearchText,
                        onValueChange = { selectorSearchText = it },
                        placeholder = { Text("Cari nama driver...", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_selector_search"),
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF49454F)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Drivers List
                    val filteredList = allRosters.filter {
                        it.driverName.contains(selectorSearchText, ignoreCase = true) ||
                        it.vehicleId.contains(selectorSearchText, ignoreCase = true)
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (filteredList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Driver tidak ditemukan", color = Color(0xFF49454F))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredList.size) { index ->
                                    val roster = filteredList[index]
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.setMyDriver(roster.driverName)
                                                showSelectorDialog = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = roster.driverName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1C1B1F)
                                                )
                                                Text(
                                                    text = roster.vehicleId,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF6750A4)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF49454F)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftBadgeBig(shiftCode: String) {
    val (bgColor, textColor, label) = getShiftStyle(shiftCode)

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "($shiftCode)",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(75.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF49454F),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarGrid(
    roster: RosterEntity,
    todayDay: Int,
    onDayClick: (Int) -> Unit
) {
    val shifts = roster.getShiftsList()
    val daysOfWeek = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (day in daysOfWeek) {
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of 30 days. June 1 is a Monday, so we place days directly 1 to 30.
        // We can render 5 rows.
        var dayCounter = 1
        for (row in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val currentDay = dayCounter
                    if (currentDay <= 30) {
                        val shiftCode = if (currentDay <= shifts.size) shifts[currentDay - 1] else ""
                        CalendarCell(
                            day = currentDay,
                            shiftCode = shiftCode,
                            isToday = currentDay == todayDay,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .testTag("calendar_day_$currentDay")
                                .clickable { onDayClick(currentDay - 1) }
                        )
                        dayCounter++
                    } else {
                        // Empty cell for the end of the month
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    day: Int,
    shiftCode: String,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, _) = getShiftStyle(shiftCode)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) Color(0xFF6750A4) else bgColor)
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = if (isToday) Color(0xFF21005D) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) Color.White else Color(0xFF49454F)
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (shiftCode.isNotEmpty()) {
                Text(
                    text = shiftCode,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isToday) Color.White else textColor
                )
            }
        }
    }
}

fun getShiftStyle(shiftCode: String): Triple<Color, Color, String> {
    return when (shiftCode.uppercase()) {
        "D" -> Triple(ShiftDayBg, ShiftDayColor, "SIANG")
        "N" -> Triple(ShiftNightBg, ShiftNightColor, "MALAM")
        "OFF" -> Triple(ShiftOffBg, ShiftOffColor, "OFF")
        "CR" -> Triple(ShiftLeaveBg, ShiftLeaveColor, "CUTI ROSTER")
        "CT" -> Triple(ShiftLeaveBg, ShiftLeaveColor, "CUTI TAHUNAN")
        "CTD" -> Triple(ShiftLeaveBg, ShiftLeaveColor, "CUTI TAHUNAN (S)")
        "CTN" -> Triple(ShiftLeaveBg, ShiftLeaveColor, "CUTI TAHUNAN (M)")
        "I" -> Triple(Color(0xFFE3F2FD), Color(0xFF1E88E5), "INDUKSI")
        "" -> Triple(Color(0xFFF1F5F9), Color.Gray, "OFF")
        else -> Triple(ShiftOtherBg, ShiftOtherColor, "$shiftCode")
    }
}
