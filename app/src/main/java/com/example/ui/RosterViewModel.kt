package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InitialData
import com.example.data.RosterEntity
import com.example.data.RosterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RosterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RosterRepository
    private val sharedPrefs = application.getSharedPreferences("hanvan_roster_prefs", Context.MODE_PRIVATE)

    // Current date values based on Android system time (Indonesian locale)
    val calendarMonth: String
        get() {
            return try {
                val calendar = java.util.Calendar.getInstance()
                val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID"))
                sdf.format(calendar.time).split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("id", "ID")) else it.toString() }
                }
            } catch (e: Exception) {
                "Juni 2026"
            }
        }

    val todayDayNumber: Int
        get() {
            return try {
                java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            } catch (e: Exception) {
                24
            }
        }

    // State flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVehicleFilter = MutableStateFlow("All")
    val selectedVehicleFilter: StateFlow<String> = _selectedVehicleFilter.asStateFlow()

    private val _myDriverName = MutableStateFlow<String?>(null)
    val myDriverName: StateFlow<String?> = _myDriverName.asStateFlow()

    private val _selectedRosterForDetail = MutableStateFlow<RosterEntity?>(null)
    val selectedRosterForDetail: StateFlow<RosterEntity?> = _selectedRosterForDetail.asStateFlow()

    private val _activeTab = MutableStateFlow("My Shift") // Default tab is "My Shift" to welcome the driver
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Database source
    val allRosters: StateFlow<List<RosterEntity>>

    // Calculated unique vehicle list for filter dropdown
    val uniqueVehicles: StateFlow<List<String>>

    // Filtered list for UI display
    val filteredRosters: StateFlow<List<RosterEntity>>

    // Roster for the current logged-in driver
    val myRoster: StateFlow<RosterEntity?>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RosterRepository(database.rosterDao())

        // Load "My Driver Name" from SharedPreferences
        _myDriverName.value = sharedPrefs.getString("my_driver_name", null)

        // Initialize and pre-populate if empty
        viewModelScope.launch {
            if (repository.getRosterCount() == 0) {
                val initialList = InitialData.parseInitialRosters()
                repository.insertAll(initialList)
            }
        }

        // Setup flows
        allRosters = repository.allRosters
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        uniqueVehicles = allRosters
            .map { list ->
                listOf("All") + list.map { it.vehicleId }.distinct().sorted()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf("All")
            )

        filteredRosters = combine(allRosters, _searchQuery, _selectedVehicleFilter) { rosters, query, vehicle ->
            rosters.filter { roster ->
                val matchesQuery = roster.driverName.contains(query, ignoreCase = true) || 
                                   roster.vehicleId.contains(query, ignoreCase = true)
                val matchesVehicle = vehicle == "All" || roster.vehicleId == vehicle
                matchesQuery && matchesVehicle
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        myRoster = combine(allRosters, _myDriverName) { rosters, name ->
            if (name == null) null else rosters.find { it.driverName.equals(name, ignoreCase = true) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedVehicleFilter(vehicle: String) {
        _selectedVehicleFilter.value = vehicle
    }

    fun setMyDriver(name: String?) {
        _myDriverName.value = name
        sharedPrefs.edit().putString("my_driver_name", name).apply()
    }

    fun selectRosterForDetail(roster: RosterEntity?) {
        _selectedRosterForDetail.value = roster
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun updateRoster(roster: RosterEntity) = viewModelScope.launch {
        repository.update(roster)
        // Update detail view if active
        if (_selectedRosterForDetail.value?.id == roster.id) {
            _selectedRosterForDetail.value = roster
        }
    }

    fun deleteRoster(roster: RosterEntity) = viewModelScope.launch {
        repository.delete(roster)
        if (_selectedRosterForDetail.value?.id == roster.id) {
            _selectedRosterForDetail.value = null
        }
    }

    fun addRoster(driverName: String, vehicleId: String, shiftsCommaSeparated: String) = viewModelScope.launch {
        val cleanShifts = shiftsCommaSeparated.split(",")
            .map { it.trim().uppercase() }
            .joinToString(",")
        repository.insert(
            RosterEntity(
                driverName = driverName.trim().uppercase(),
                vehicleId = vehicleId.trim().uppercase(),
                shiftsCommaSeparated = cleanShifts
            )
        )
    }

    fun parseAndImportRoster(rawText: String): Boolean {
        try {
            val list = mutableListOf<RosterEntity>()
            val lines = rawText.split("\n")
            // Match tabs or 2 or more consecutive spaces
            val splitter = Regex("\\t|\\s{2,}")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val parts = trimmed.split(splitter)
                if (parts.size >= 2) {
                    val name = parts[0].trim().uppercase()
                    val vehicle = parts[1].trim().uppercase()
                    val shifts = parts.subList(2, parts.size).map { it.trim().uppercase() }
                    val shiftsStr = shifts.joinToString(",")
                    list.add(RosterEntity(driverName = name, vehicleId = vehicle, shiftsCommaSeparated = shiftsStr))
                }
            }
            if (list.isNotEmpty()) {
                viewModelScope.launch {
                    repository.deleteAll()
                    repository.insertAll(list)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun resetToDefault() = viewModelScope.launch {
        repository.deleteAll()
        val initialList = InitialData.parseInitialRosters()
        repository.insertAll(initialList)
    }

    // Helper functions for stats
    fun getRosterStats(roster: RosterEntity): RosterStats {
        val shifts = roster.getShiftsList()
        var dayShifts = 0
        var nightShifts = 0
        var offDays = 0
        var leaveRoster = 0 // CR
        var leaveAnnual = 0 // CTD, CTN
        var permittedAbsence = 0 // I
        var standby = 0 // CTD/CTN standby or CTN itself? Wait, CTD is Cuti Tahunan Day, CTN is Cuti Tahunan Night.
        var other = 0

        for (shift in shifts) {
            when (shift.uppercase()) {
                "D" -> dayShifts++
                "N" -> nightShifts++
                "OFF" -> offDays++
                "CR" -> leaveRoster++
                "CTD" -> leaveAnnual++
                "CTN" -> leaveAnnual++
                "I" -> permittedAbsence++
                else -> {
                    if (shift.isNotEmpty()) other++
                }
            }
        }

        val totalWorkDays = dayShifts + nightShifts
        return RosterStats(
            dayShifts = dayShifts,
            nightShifts = nightShifts,
            offDays = offDays,
            leaveRoster = leaveRoster,
            leaveAnnual = leaveAnnual,
            permittedAbsence = permittedAbsence,
            other = other,
            totalWorkDays = totalWorkDays
        )
    }
}

data class RosterStats(
    val dayShifts: Int,
    val nightShifts: Int,
    val offDays: Int,
    val leaveRoster: Int,
    val leaveAnnual: Int,
    val permittedAbsence: Int,
    val other: Int,
    val totalWorkDays: Int
)
