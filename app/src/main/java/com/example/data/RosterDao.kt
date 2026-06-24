package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RosterDao {
    @Query("SELECT * FROM rosters ORDER BY driverName ASC")
    fun getAllRosters(): Flow<List<RosterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rosters: List<RosterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(roster: RosterEntity)

    @Update
    suspend fun update(roster: RosterEntity)

    @Delete
    suspend fun delete(roster: RosterEntity)

    @Query("DELETE FROM rosters")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM rosters")
    suspend fun getRosterCount(): Int
}
