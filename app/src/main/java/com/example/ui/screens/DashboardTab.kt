package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RosterEntity
import com.example.ui.RosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val filteredRosters by viewModel.filteredRosters.collectAsState()
    val allRosters by viewModel.allRosters.collectAsState()
    val uniqueVehicles by viewModel.uniqueVehicles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedVehicleFilter by viewModel.selectedVehicleFilter.collectAsState()
    val selectedRosterForDetail by viewModel.selectedRosterForDetail.collectAsState()

    var isTruckSharingView by remember { mutableStateOf(false) }
    var showVehicleDropdown by remember { mutableStateOf(false) }
    var selectedShiftTab by remember { mutableStateOf("SEMUA") }

    val currentShiftDrivers = remember(filteredRosters, selectedShiftTab, viewModel.todayDayNumber, allRosters) {
        val baseList = if (selectedShiftTab == "SEMUA") {
            filteredRosters
        } else {
            filteredRosters.filter { roster ->
                val truckDrivers = allRosters.filter { it.vehicleId == roster.vehicleId }
                val assignment = getTruckSlotAssignment(truckDrivers, viewModel.todayDayNumber)
                if (selectedShiftTab == "SIANG") {
                    assignment.dayDriver?.driverName == roster.driverName
                } else {
                    assignment.nightDriver?.driverName == roster.driverName
                }
            }
        }
        
        // Sort so OFF, CR, I (and any leave status) are at the very top of the list
        baseList.sortedWith(compareBy { roster ->
            val shifts = roster.getShiftsList()
            val todayShift = if (viewModel.todayDayNumber <= shifts.size) {
                shifts[viewModel.todayDayNumber - 1].uppercase()
            } else "OFF"
            
            val isOffOrLeave = todayShift == "OFF" || todayShift == "CR" || todayShift == "I" || 
                               todayShift == "CT" || todayShift == "CTD" || todayShift == "CTN" || 
                               todayShift.isEmpty()
            if (isOffOrLeave) 0 else 1
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FA))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search & Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari driver/truk...", color = Color(0xFF49454F), fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6750A4),
                    unfocusedBorderColor = Color(0xFFCAC4D0),
                    focusedTextColor = Color(0xFF1C1B1F),
                    unfocusedTextColor = Color(0xFF1C1B1F),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .testTag("dashboard_search"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF49454F)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF49454F))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter Button
            Box {
                Button(
                    onClick = { showVehicleDropdown = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(55.dp).testTag("vehicle_filter_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (selectedVehicleFilter == "All") "Armada" else selectedVehicleFilter,
                            color = if (selectedVehicleFilter == "All") Color(0xFF1C1B1F) else Color(0xFF6750A4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF49454F),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showVehicleDropdown,
                    onDismissRequest = { showVehicleDropdown = false },
                    modifier = Modifier
                        .background(Color.White)
                        .width(180.dp)
                ) {
                    uniqueVehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (vehicle == "All") "Semua Armada" else vehicle,
                                    color = if (vehicle == selectedVehicleFilter) Color(0xFF6750A4) else Color(0xFF1C1B1F),
                                    fontWeight = if (vehicle == selectedVehicleFilter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setSelectedVehicleFilter(vehicle)
                                showVehicleDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Mode Selector (Drivers List vs Truck Sharing Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFF3EDF7))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val buttonModifier = Modifier
                .weight(1f)
                .height(36.dp)

            Button(
                onClick = { isTruckSharingView = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isTruckSharingView) Color(0xFF6750A4) else Color.Transparent
                ),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = buttonModifier.testTag("driver_list_view_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = if (!isTruckSharingView) Color.White else Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Daftar Driver", color = if (!isTruckSharingView) Color.White else Color(0xFF49454F), fontSize = 12.sp)
            }

            Button(
                onClick = { isTruckSharingView = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTruckSharingView) Color(0xFF6750A4) else Color.Transparent
                ),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = buttonModifier.testTag("truck_sharing_view_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = if (isTruckSharingView) Color.White else Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Berbagi Truk", color = if (isTruckSharingView) Color.White else Color(0xFF49454F), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shift Filter Tabs (SEMUA, SIANG, MALAM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFF3EDF7))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("SEMUA", "SIANG", "MALAM")
            tabs.forEach { tab ->
                val isSelected = selectedShiftTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) Color(0xFF6750A4) else Color.Transparent)
                        .clickable { selectedShiftTab = tab }
                        .testTag("shift_tab_$tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF49454F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Heading showing Active Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTruckSharingView) "MATRIKS BERBAGI TRUK" else "DIREKTORI DRIVER",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF6750A4)
            )
            Text(
                text = "Shift Hari Ini • Hari ${viewModel.todayDayNumber}",
                fontSize = 11.sp,
                color = Color(0xFF49454F)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            if (currentShiftDrivers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Data driver/truk tidak ditemukan.", color = Color(0xFF49454F))
                    }
                }
            } else if (isTruckSharingView) {
                // Group drivers by Truck/Vehicle ID using the active filtered set
                val groupedByTruck = currentShiftDrivers.groupBy { it.vehicleId }
                val sortedTrucks = groupedByTruck.keys.toList().sorted()

                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedTrucks) { truckId ->
                        // Pass the actual full set of drivers for this truck from allRosters so pairing logic works perfectly!
                        val drivers = allRosters.filter { it.vehicleId == truckId }
                        TruckSharingCard(
                            truckId = truckId,
                            drivers = drivers,
                            todayDay = viewModel.todayDayNumber,
                            onDriverClick = { viewModel.selectRosterForDetail(it) }
                        )
                    }
                }
            } else {
                // Standard Drivers List
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentShiftDrivers) { roster ->
                        DriverListItem(
                            roster = roster,
                            todayDay = viewModel.todayDayNumber,
                            onClick = { viewModel.selectRosterForDetail(roster) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Driver Detailed Roster Calendar Dialog
    if (selectedRosterForDetail != null) {
        val roster = selectedRosterForDetail!!
        Dialog(onDismissRequest = { viewModel.selectRosterForDetail(null) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF7F2FA)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = roster.driverName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = roster.vehicleId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6750A4)
                            )
                        }
                        IconButton(onClick = { viewModel.selectRosterForDetail(null) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF1C1B1F))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats summary
                    val stats = viewModel.getRosterStats(roster)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hari Kerja", fontSize = 10.sp, color = Color(0xFF49454F))
                                Text("${stats.totalWorkDays}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Siang (D)", fontSize = 10.sp, color = Color(0xFF49454F))
                                Text("${stats.dayShifts}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Malam (N)", fontSize = 10.sp, color = Color(0xFF49454F))
                                Text("${stats.nightShifts}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9575CD))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("OFF", fontSize = 10.sp, color = Color(0xFF49454F))
                                Text("${stats.offDays}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Kalender Shift ${viewModel.calendarMonth}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Calendar
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarGrid(
                            roster = roster,
                            todayDay = viewModel.todayDayNumber,
                            onDayClick = {}
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Edit / Set as My Profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setMyDriver(roster.driverName)
                                viewModel.selectRosterForDetail(null)
                                viewModel.setActiveTab("My Shift") // Switch tab to My Shift
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jadikan Profil Saya", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverListItem(
    roster: RosterEntity,
    todayDay: Int,
    onClick: () -> Unit
) {
    val shifts = roster.getShiftsList()
    val todayShift = if (todayDay <= shifts.size) shifts[todayDay - 1] else "OFF"
    val (bgColor, textColor, label) = getShiftStyle(todayShift)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Driver metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roster.driverName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = roster.vehicleId,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6750A4)
                    )
                }
            }

            // Shift status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$label ($todayShift)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
            }
        }
    }
}

data class TruckSlotAssignment(
    val dayDriver: RosterEntity?,
    val dayShiftCode: String,
    val nightDriver: RosterEntity?,
    val nightShiftCode: String
)

fun getTruckSlotAssignment(drivers: List<RosterEntity>, todayDay: Int): TruckSlotAssignment {
    if (drivers.isEmpty()) {
        return TruckSlotAssignment(null, "", null, "")
    }
    
    if (drivers.size == 1) {
        val driver = drivers[0]
        val shifts = driver.getShiftsList()
        val shift = if (todayDay <= shifts.size) shifts[todayDay - 1].uppercase() else "OFF"
        
        return if (shift == "N" || shift == "CTN") {
            // It's Night shift, so Day is unassigned
            TruckSlotAssignment(null, "", driver, shift)
        } else {
            // Otherwise, we can treat it as Day (or if it's OFF/leave, default to Day)
            TruckSlotAssignment(driver, shift, null, "")
        }
    }
    
    // There are 2 drivers
    val d1 = drivers[0]
    val d2 = drivers[1]
    
    val shifts1 = d1.getShiftsList()
    val s1 = if (todayDay <= shifts1.size) shifts1[todayDay - 1].uppercase() else "OFF"
    
    val shifts2 = d2.getShiftsList()
    val s2 = if (todayDay <= shifts2.size) shifts2[todayDay - 1].uppercase() else "OFF"
    
    val isD1Day = s1 == "D" || s1 == "CTD"
    val isD2Day = s2 == "D" || s2 == "CTD"
    val isD1Night = s1 == "N" || s1 == "CTN"
    val isD2Night = s2 == "N" || s2 == "CTN"
    
    // Pairing logic:
    // 1. One is Day, one is Night
    if (isD1Day && isD2Night) {
        return TruckSlotAssignment(d1, s1, d2, s2)
    }
    if (isD2Day && isD1Night) {
        return TruckSlotAssignment(d2, s2, d1, s1)
    }
    
    // 2. One is Day, the other is CR/OFF/Leave (goes to Night)
    if (isD1Day && !isD2Day && !isD2Night) {
        return TruckSlotAssignment(d1, s1, d2, s2)
    }
    if (isD2Day && !isD1Day && !isD1Night) {
        return TruckSlotAssignment(d2, s2, d1, s1)
    }
    
    // 3. One is Night, the other is CR/OFF/Leave (goes to Day)
    if (isD1Night && !isD2Day && !isD2Night) {
        return TruckSlotAssignment(d2, s2, d1, s1)
    }
    if (isD2Night && !isD1Day && !isD1Night) {
        return TruckSlotAssignment(d1, s1, d2, s2)
    }
    
    // 4. Default fallback: both OFF/Leave, both Day, or both Night
    return TruckSlotAssignment(d1, s1, d2, s2)
}

@Composable
fun TruckSharingCard(
    truckId: String,
    drivers: List<RosterEntity>,
    todayDay: Int,
    onDriverClick: (RosterEntity) -> Unit
) {
    val assignment = getTruckSlotAssignment(drivers, todayDay)
    
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Truck ID header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = truckId,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF1C1B1F)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show Day/Night slots side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day Slot
                val hasDayDriver = assignment.dayDriver != null
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF3E0).copy(alpha = 0.4f)) // Light warm orange hue for Day
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .then(
                            if (hasDayDriver) {
                                Modifier.clickable { onDriverClick(assignment.dayDriver!!) }
                            } else Modifier
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SIANG (DAY)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    if (hasDayDriver) {
                        Text(
                            text = assignment.dayDriver!!.driverName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val (bgColor, textColor, label) = getShiftStyle(assignment.dayShiftCode)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                                .background(bgColor)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$label (${assignment.dayShiftCode.ifEmpty { "OFF" }})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Tidak Ada Driver",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KOSONG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Night Slot
                val hasNightDriver = assignment.nightDriver != null
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3E5F5).copy(alpha = 0.4f)) // Light purple hue for Night
                        .border(1.dp, Color(0xFF673AB7).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .then(
                            if (hasNightDriver) {
                                Modifier.clickable { onDriverClick(assignment.nightDriver!!) }
                            } else Modifier
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "MALAM (NIGHT)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF311B92),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (hasNightDriver) {
                        Text(
                            text = assignment.nightDriver!!.driverName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val (bgColor, textColor, label) = getShiftStyle(assignment.nightShiftCode)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                                .background(bgColor)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$label (${assignment.nightShiftCode.ifEmpty { "OFF" }})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Tidak Ada Driver",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KOSONG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
