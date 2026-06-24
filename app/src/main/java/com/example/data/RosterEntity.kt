package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rosters")
data class RosterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val driverName: String,
    val vehicleId: String,
    val shiftsCommaSeparated: String // e.g. "D,N,OFF,CR,I"
) {
    fun getShiftsList(): List<String> {
        return if (shiftsCommaSeparated.isEmpty()) emptyList() else shiftsCommaSeparated.split(",")
    }
}
