package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GoogleSheetsSyncManager
import com.example.data.InitialData
import com.example.data.RosterEntity
import com.example.data.RosterRepository
import com.example.data.SyncLogEntry
import com.example.data.SyncState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RosterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RosterRepository
    val syncManager = GoogleSheetsSyncManager(application)
    private val sharedPrefs = application.getSharedPreferences("hanvan_roster_prefs", Context.MODE_PRIVATE)

    // Sync State Flows
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isOnline = MutableStateFlow(syncManager.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _sheetUrl = MutableStateFlow(syncManager.getSavedSheetUrl())
    val sheetUrl: StateFlow<String> = _sheetUrl.asStateFlow()

    private val _webhookUrl = MutableStateFlow(syncManager.getWebhookUrl())
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(syncManager.getLastSyncTime())
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(syncManager.getPendingSyncCount())
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(syncManager.isAutoSyncEnabled())
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _conflictPolicy = MutableStateFlow(syncManager.getConflictPolicy())
    val conflictPolicy: StateFlow<String> = _conflictPolicy.asStateFlow()

    private val _syncLogs = MutableStateFlow(syncManager.getSyncLogs())
    val syncLogs: StateFlow<List<SyncLogEntry>> = _syncLogs.asStateFlow()

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
        syncManager.incrementPendingSync()
        _pendingSyncCount.value = syncManager.getPendingSyncCount()
    }

    fun updateRoster(roster: RosterEntity) = viewModelScope.launch {
        repository.update(roster)
        syncManager.incrementPendingSync()
        _pendingSyncCount.value = syncManager.getPendingSyncCount()
        if (_selectedRosterForDetail.value?.id == roster.id) {
            _selectedRosterForDetail.value = roster
        }
    }

    fun deleteRoster(roster: RosterEntity) = viewModelScope.launch {
        repository.delete(roster)
        syncManager.incrementPendingSync()
        _pendingSyncCount.value = syncManager.getPendingSyncCount()
        if (_selectedRosterForDetail.value?.id == roster.id) {
            _selectedRosterForDetail.value = null
        }
    }

    // Google Sheets Offline-First Sync Methods
    fun refreshNetworkStatus() {
        val online = syncManager.isOnline()
        _isOnline.value = online
    }

    fun saveSyncConfig(sheetUrl: String, webhookUrl: String, autoSync: Boolean, policy: String) {
        syncManager.saveSheetUrl(sheetUrl)
        syncManager.saveWebhookUrl(webhookUrl)
        syncManager.setAutoSyncEnabled(autoSync)
        syncManager.setConflictPolicy(policy)

        _sheetUrl.value = syncManager.getSavedSheetUrl()
        _webhookUrl.value = syncManager.getWebhookUrl()
        _isAutoSyncEnabled.value = autoSync
        _conflictPolicy.value = policy
    }

    fun fetchAndSyncFromGoogleSheets(targetUrl: String? = null, isAuto: Boolean = false) {
        viewModelScope.launch {
            refreshNetworkStatus()
            val urlToFetch = targetUrl ?: _sheetUrl.value
            val currentRosterCount = repository.getRosterCount()

            _syncState.value = SyncState.Syncing

            if (!syncManager.isOnline()) {
                val msg = "Perangkat sedang Offline. Aplikasi menggunakan database lokal Room (${currentRosterCount} driver aktif)."
                _syncState.value = SyncState.ErrorOffline(msg, currentRosterCount)
                syncManager.addSyncLog(
                    action = if (isAuto) "Auto-Sync (Tarik)" else "Tarik Google Sheets",
                    status = "OFFLINE_CACHED",
                    details = "Offline - Menggunakan Room DB lokal ($currentRosterCount data)"
                )
                _syncLogs.value = syncManager.getSyncLogs()
                return@launch
            }

            val result = syncManager.fetchRostersFromGoogleSheets(urlToFetch)
            result.onSuccess { fetchedList ->
                if (fetchedList.isNotEmpty()) {
                    repository.deleteAll()
                    repository.insertAll(fetchedList)
                    syncManager.setPendingSyncCount(0)

                    val now = syncManager.getLastSyncTime()
                    _lastSyncTime.value = now
                    _pendingSyncCount.value = 0
                    _syncState.value = SyncState.Success(
                        message = "Berhasil mengimpor ${fetchedList.size} roster driver dari Google Sheets!",
                        timestamp = now,
                        rosterCount = fetchedList.size
                    )
                    syncManager.addSyncLog(
                        action = if (isAuto) "Auto-Sync (Tarik)" else "Tarik Google Sheets",
                        status = "ONLINE_SUCCESS",
                        details = "Berhasil memperbarui ${fetchedList.size} driver dari Google Sheets"
                    )
                } else {
                    _syncState.value = SyncState.ErrorNetwork("Data Google Sheets kosong atau tidak memiliki format yang sesuai.")
                    syncManager.addSyncLog("Tarik Google Sheets", "ERROR", "Format data Google Sheets tidak sesuai")
                }
            }.onFailure { exception ->
                if (exception.message == "PERANGKAT_OFFLINE") {
                    _syncState.value = SyncState.ErrorOffline(
                        "Perangkat sedang Offline. Data lokal Room tetap aman dan aktif.",
                        currentRosterCount
                    )
                    syncManager.addSyncLog("Tarik Google Sheets", "OFFLINE_CACHED", "Offline Mode - $currentRosterCount driver lokal")
                } else {
                    val errMsg = exception.localizedMessage ?: "Gagal terhubung ke Google Sheets"
                    _syncState.value = SyncState.ErrorNetwork(errMsg)
                    syncManager.addSyncLog("Tarik Google Sheets", "ERROR", errMsg)
                }
            }
            _syncLogs.value = syncManager.getSyncLogs()
        }
    }

    fun pushLocalDataToGoogleSheets() {
        viewModelScope.launch {
            refreshNetworkStatus()
            val currentRosters = repository.allRosters.first()
            _syncState.value = SyncState.Syncing

            if (!syncManager.isOnline()) {
                syncManager.incrementPendingSync()
                _pendingSyncCount.value = syncManager.getPendingSyncCount()
                val msg = "Perangkat Offline. Perubahan disimpan di antrean lokal (Pending Sync: ${_pendingSyncCount.value})."
                _syncState.value = SyncState.ErrorOffline(msg, currentRosters.size)
                syncManager.addSyncLog("Kirim ke Google Sheets", "OFFLINE_CACHED", "Disimpan di antrean lokal (${_pendingSyncCount.value} pending)")
                _syncLogs.value = syncManager.getSyncLogs()
                return@launch
            }

            val webhook = _webhookUrl.value
            if (webhook.isBlank()) {
                _syncState.value = SyncState.ErrorNetwork("URL Webhook Apps Script belum diatur di menu Sinkronisasi.")
                return@launch
            }

            val result = syncManager.pushRostersToWebhook(webhook, currentRosters)
            result.onSuccess { successMsg ->
                val now = syncManager.getLastSyncTime()
                _lastSyncTime.value = now
                _pendingSyncCount.value = 0
                _syncState.value = SyncState.Success(successMsg, now, currentRosters.size)
                syncManager.addSyncLog("Kirim ke Google Sheets", "ONLINE_SUCCESS", "Kirim ${currentRosters.size} driver ke Webhook Google Sheets")
            }.onFailure { ex ->
                val errMsg = ex.localizedMessage ?: "Gagal push ke Webhook"
                _syncState.value = SyncState.ErrorNetwork(errMsg)
                syncManager.addSyncLog("Kirim ke Google Sheets", "ERROR", errMsg)
            }
            _syncLogs.value = syncManager.getSyncLogs()
        }
    }

    fun exportLocalToCsv(): String {
        val currentList = allRosters.value
        return syncManager.exportLocalToCsv(currentList)
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
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
