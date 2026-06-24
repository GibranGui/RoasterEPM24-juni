package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RosterViewModel

@Composable
fun AnalyticsTab(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val allRosters by viewModel.allRosters.collectAsState()
    val todayDay = viewModel.todayDayNumber

    // Calculate today's shift breakdown
    var dayCount = 0
    var nightCount = 0
    var offCount = 0
    var leaveCount = 0
    var otherCount = 0

    val uniqueTrucks = mutableSetOf<String>()
    var activeTrucksCount = 0

    for (roster in allRosters) {
        uniqueTrucks.add(roster.vehicleId)
        val shifts = roster.getShiftsList()
        val todayShift = if (todayDay <= shifts.size) shifts[todayDay - 1] else "OFF"
        
        when (todayShift.uppercase()) {
            "D" -> {
                dayCount++
                activeTrucksCount++
            }
            "N" -> {
                nightCount++
                activeTrucksCount++
            }
            "OFF" -> offCount++
            "CR", "CTD", "CTN", "I" -> leaveCount++
            else -> {
                if (todayShift.isNotEmpty()) otherCount++ else offCount++
            }
        }
    }

    val totalDriversCount = allRosters.size
    val totalTrucksCount = uniqueTrucks.size

    // Occupancy Rate: ratio of trucks that are being driven today
    // Since each truck can be active if at least one driver is D or N today.
    val activeTrucksSet = mutableSetOf<String>()
    for (roster in allRosters) {
        val shifts = roster.getShiftsList()
        val todayShift = if (todayDay <= shifts.size) shifts[todayDay - 1] else "OFF"
        if (todayShift.uppercase() == "D" || todayShift.uppercase() == "N") {
            activeTrucksSet.add(roster.vehicleId)
        }
    }
    val occupancyRate = if (totalTrucksCount > 0) {
        (activeTrucksSet.size.toFloat() / totalTrucksCount.toFloat()) * 100f
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F2FA))
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Headings
            Text(
                text = "OPERATIONS ANALYTICS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF6750A4),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // General KPIs Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    title = "Total Drivers",
                    value = "$totalDriversCount",
                    description = "Active roster count",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Fleet",
                    value = "$totalTrucksCount",
                    description = "Unique dump trucks",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    title = "Active Trucks Today",
                    value = "${activeTrucksSet.size}",
                    description = "Trucks in service today",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Fleet Utilization",
                    value = String.format("%.1f%%", occupancyRate),
                    description = "Active truck sharing ratio",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Distribution Donut Chart
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().testTag("analytics_chart_card")
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Today's Driver Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "Day $todayDay shift rosters breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Custom Donut Canvas Chart
                        Box(
                            modifier = Modifier.size(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChartCanvas(
                                dayVal = dayCount.toFloat(),
                                nightVal = nightCount.toFloat(),
                                offVal = offCount.toFloat(),
                                leaveVal = leaveCount.toFloat()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalDriversCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1C1B1F)
                                )
                                Text(
                                    text = "DRIVERS",
                                    fontSize = 9.sp,
                                    color = Color(0xFF49454F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Chart Legend
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem(label = "Siang (D)", count = dayCount, color = Color(0xFF6750A4))
                            LegendItem(label = "Malam (N)", count = nightCount, color = Color(0xFF9575CD))
                            LegendItem(label = "OFF (OFF)", count = offCount, color = Color(0xFF81C784))
                            LegendItem(label = "Cuti (CR/CT)", count = leaveCount, color = Color(0xFFEF5350))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shift Allocation Status Information Alert
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Truck sharing optimizes assets. Standard roster schedules assign two alternating drivers per truck to maintain 24-hour haulage capability at the mine site.",
                        fontSize = 11.sp,
                        color = Color(0xFF21005D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F)
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1C1B1F)
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color(0xFF6750A4)
            )
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF1C1B1F)
            )
            Text(
                text = "$count drivers",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F)
            )
        }
    }
}

@Composable
fun DonutChartCanvas(
    dayVal: Float,
    nightVal: Float,
    offVal: Float,
    leaveVal: Float
) {
    val total = dayVal + nightVal + offVal + leaveVal
    val daySweep = if (total > 0) (dayVal / total) * 360f else 0f
    val nightSweep = if (total > 0) (nightVal / total) * 360f else 0f
    val offSweep = if (total > 0) (offVal / total) * 360f else 0f
    val leaveSweep = if (total > 0) (leaveVal / total) * 360f else 0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 32f
        val diameter = size.minDimension - strokeWidth
        val size = Size(diameter, diameter)
        val offset = Offset(strokeWidth / 2, strokeWidth / 2)

        var startAngle = -90f

        // Day Shift
        if (daySweep > 0) {
            drawArc(
                color = Color(0xFF6750A4),
                startAngle = startAngle,
                sweepAngle = daySweep,
                useCenter = false,
                topLeft = offset,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += daySweep
        }

        // Night Shift
        if (nightSweep > 0) {
            drawArc(
                color = Color(0xFF9575CD),
                startAngle = startAngle,
                sweepAngle = nightSweep,
                useCenter = false,
                topLeft = offset,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += nightSweep
        }

        // Off Days
        if (offSweep > 0) {
            drawArc(
                color = Color(0xFF81C784),
                startAngle = startAngle,
                sweepAngle = offSweep,
                useCenter = false,
                topLeft = offset,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += offSweep
        }

        // Leave
        if (leaveSweep > 0) {
            drawArc(
                color = Color(0xFFEF5350),
                startAngle = startAngle,
                sweepAngle = leaveSweep,
                useCenter = false,
                topLeft = offset,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
