package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val timestamp: String, val rosterCount: Int) : SyncState()
    data class ErrorOffline(val message: String, val localCount: Int) : SyncState()
    data class ErrorNetwork(val message: String) : SyncState()
}

data class SyncLogEntry(
    val timestamp: String,
    val action: String,
    val status: String, // "ONLINE_SUCCESS", "OFFLINE_CACHED", "ERROR"
    val details: String
)

class GoogleSheetsSyncManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("google_sheets_sync_prefs", Context.MODE_PRIVATE)

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }

    fun getSavedSheetUrl(): String {
        return prefs.getString("sheet_url", DEFAULT_SHEET_URL) ?: DEFAULT_SHEET_URL
    }

    fun saveSheetUrl(url: String) {
        prefs.edit().putString("sheet_url", url.trim()).apply()
    }

    fun getWebhookUrl(): String {
        return prefs.getString("webhook_url", "") ?: ""
    }

    fun saveWebhookUrl(url: String) {
        prefs.edit().putString("webhook_url", url.trim()).apply()
    }

    fun getLastSyncTime(): String {
        return prefs.getString("last_sync_time", "Belum Pernah") ?: "Belum Pernah"
    }

    fun getPendingSyncCount(): Int {
        return prefs.getInt("pending_sync_count", 0)
    }

    fun setPendingSyncCount(count: Int) {
        prefs.edit().putInt("pending_sync_count", count).apply()
    }

    fun incrementPendingSync() {
        val current = getPendingSyncCount()
        setPendingSyncCount(current + 1)
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("auto_sync_enabled", true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun getConflictPolicy(): String {
        return prefs.getString("conflict_policy", "OFFLINE_FIRST") ?: "OFFLINE_FIRST"
    }

    fun setConflictPolicy(policy: String) {
        prefs.edit().putString("conflict_policy", policy).apply()
    }

    fun addSyncLog(action: String, status: String, details: String) {
        val currentLogs = getSyncLogs().toMutableList()
        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID")).format(Date())
        currentLogs.add(0, SyncLogEntry(timestamp, action, status, details))
        // Keep max 20 logs
        val trimmed = if (currentLogs.size > 20) currentLogs.subList(0, 20) else currentLogs
        val jsonString = trimmed.joinToString("|||") { "${it.timestamp}###${it.action}###${it.status}###${it.details}" }
        prefs.edit().putString("sync_logs_v2", jsonString).apply()
    }

    fun getSyncLogs(): List<SyncLogEntry> {
        val raw = prefs.getString("sync_logs_v2", null) ?: return emptyList()
        return try {
            raw.split("|||").mapNotNull { line ->
                val parts = line.split("###")
                if (parts.size >= 4) {
                    SyncLogEntry(parts[0], parts[1], parts[2], parts[3])
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun extractSpreadsheetId(urlOrId: String): String {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("docs.google.com/spreadsheets/d/")) {
            val pattern = Regex("spreadsheets/d/([a-zA-Z0-9-_]+)")
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return trimmed
    }

    fun buildCsvExportUrl(spreadsheetUrlOrId: String): String {
        val id = extractSpreadsheetId(spreadsheetUrlOrId)
        return if (id.isNotEmpty()) {
            "https://docs.google.com/spreadsheets/d/$id/export?format=csv"
        } else ""
    }

    suspend fun fetchRostersFromGoogleSheets(spreadsheetUrlOrId: String): Result<List<RosterEntity>> = withContext(Dispatchers.IO) {
        val csvUrl = buildCsvExportUrl(spreadsheetUrlOrId)
        if (csvUrl.isEmpty()) {
            return@withContext Result.failure(Exception("URL Google Sheets atau Spreadsheet ID tidak valid."))
        }

        if (!isOnline()) {
            return@withContext Result.failure(Exception("PERANGKAT_OFFLINE"))
        }

        try {
            val request = Request.Builder()
                .url(csvUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}: Gagal mengunduh Google Sheet."))
                }

                val csvContent = response.body?.string() ?: ""
                if (csvContent.isBlank()) {
                    return@withContext Result.failure(Exception("Data Google Sheet kosong."))
                }

                val parsedRosters = parseCsvToRosters(csvContent)
                if (parsedRosters.isEmpty()) {
                    return@withContext Result.failure(Exception("Format Google Sheet tidak cocok dengan struktur Roster Driver."))
                }

                val nowString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
                prefs.edit().putString("last_sync_time", nowString).apply()

                Result.success(parsedRosters)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushRostersToWebhook(webhookUrl: String, rosters: List<RosterEntity>): Result<String> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            incrementPendingSync()
            return@withContext Result.failure(Exception("PERANGKAT_OFFLINE"))
        }

        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isEmpty()) {
            return@withContext Result.failure(Exception("URL Webhook Apps Script belum diisi."))
        }

        try {
            val jsonBody = buildJsonBodyForPush(rosters)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(cleanUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    setPendingSyncCount(0)
                    val nowString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
                    prefs.edit().putString("last_sync_time", nowString).apply()
                    Result.success("Berhasil mengirim data ke Google Sheets Webhook!")
                } else {
                    Result.failure(Exception("HTTP ${response.code}: Gagal push ke Webhook."))
                }
            }
        } catch (e: Exception) {
            incrementPendingSync()
            Result.failure(e)
        }
    }

    fun parseCsvToRosters(csvText: String): List<RosterEntity> {
        val result = mutableListOf<RosterEntity>()
        val lines = csvText.split("\n")
        
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            // Support comma, tab, or semicolon CSV formatting
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
                val firstCol = parts[0].replace("\"", "").trim()
                val secondCol = parts[1].replace("\"", "").trim()

                // Skip header if first column contains "NAMA" or "DRIVER"
                if (firstCol.equals("NAMA", ignoreCase = true) || 
                    firstCol.equals("NAMA DRIVER", ignoreCase = true) ||
                    firstCol.equals("DRIVER", ignoreCase = true)) {
                    continue
                }

                if (firstCol.isNotEmpty() && secondCol.isNotEmpty()) {
                    val shifts = parts.subList(2, parts.size)
                        .map { it.replace("\"", "").trim().uppercase() }
                        .filter { it.isNotEmpty() }
                    val shiftsCommaSeparated = shifts.joinToString(",")

                    result.add(
                        RosterEntity(
                            driverName = firstCol.uppercase(),
                            vehicleId = secondCol.uppercase(),
                            shiftsCommaSeparated = shiftsCommaSeparated
                        )
                    )
                }
            }
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line.toCharArray()) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                (ch == ',' || ch == '\t' || ch == ';') && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun exportLocalToCsv(rosters: List<RosterEntity>): String {
        val sb = StringBuilder()
        sb.append("NAMA DRIVER\tUNIT KENDARAAN\tSHIFT 1\tSHIFT 2\tSHIFT 3\tSHIFT 4\tSHIFT 5\tSHIFT 6\tSHIFT 7\tSHIFT 8\tSHIFT 9\tSHIFT 10\tSHIFT 11\tSHIFT 12\tSHIFT 13\tSHIFT 14\tSHIFT 15\tSHIFT 16\tSHIFT 17\tSHIFT 18\tSHIFT 19\tSHIFT 20\tSHIFT 21\tSHIFT 22\tSHIFT 23\tSHIFT 24\tSHIFT 25\tSHIFT 26\tSHIFT 27\tSHIFT 28\tSHIFT 29\tSHIFT 30\n")
        for (r in rosters) {
            val shifts = r.getShiftsList().joinToString("\t")
            sb.append("${r.driverName}\t${r.vehicleId}\t$shifts\n")
        }
        return sb.toString()
    }

    private fun buildJsonBodyForPush(rosters: List<RosterEntity>): String {
        val itemsJson = rosters.joinToString(",") { r ->
            val shiftsArr = r.getShiftsList().joinToString(",") { "\"$it\"" }
            """{"driverName":"${r.driverName}","vehicleId":"${r.vehicleId}","shifts":[$shiftsArr]}"""
        }
        return """{"action":"sync_rosters","timestamp":${System.currentTimeMillis()},"rosters":[$itemsJson]}"""
    }

    companion object {
        const val DEFAULT_SHEET_URL = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit"
    }
}
