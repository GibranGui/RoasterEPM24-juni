package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RosterViewModel
import com.example.ui.screens.AboutTab
import com.example.ui.screens.AnalyticsTab
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.MyShiftTab
import com.example.ui.screens.ToolsTab
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainShell(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(viewModel: RosterViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "DRIVER EPM",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6750A4), // Elegant Violet
                            letterSpacing = 1.5.sp,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROSTER",
                            fontWeight = FontWeight.Light,
                            color = Color(0xFF1C1B1F), // Dark charcoal
                            letterSpacing = 1.sp,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1B1F)
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF49454F),
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                // My Shift Tab
                NavigationBarItem(
                    selected = activeTab == "My Shift",
                    onClick = { viewModel.setActiveTab("My Shift") },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Shift Saya") },
                    label = { Text("Shift Saya", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6750A4),
                        selectedTextColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    ),
                    modifier = Modifier.testTag("nav_my_shift")
                )

                // Dashboard Tab
                NavigationBarItem(
                    selected = activeTab == "Dashboard",
                    onClick = { viewModel.setActiveTab("Dashboard") },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Dasbor") },
                    label = { Text("Dasbor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6750A4),
                        selectedTextColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                // Tools Tab
                NavigationBarItem(
                    selected = activeTab == "Tools",
                    onClick = { viewModel.setActiveTab("Tools") },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Alat") },
                    label = { Text("Alat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6750A4),
                        selectedTextColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    ),
                    modifier = Modifier.testTag("nav_tools")
                )

                // About Tab
                NavigationBarItem(
                    selected = activeTab == "About",
                    onClick = { viewModel.setActiveTab("About") },
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Tentang") },
                    label = { Text("Tentang", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF6750A4),
                        selectedTextColor = Color(0xFF6750A4),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    ),
                    modifier = Modifier.testTag("nav_about")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F2FA))
        ) {
            when (activeTab) {
                "My Shift" -> MyShiftTab(viewModel = viewModel)
                "Dashboard" -> DashboardTab(viewModel = viewModel)
                "Tools" -> ToolsTab(viewModel = viewModel)
                "About" -> AboutTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

