package com.example.data

import kotlinx.coroutines.flow.Flow

class RosterRepository(private val rosterDao: RosterDao) {
    val allRosters: Flow<List<RosterEntity>> = rosterDao.getAllRosters()

    suspend fun insertAll(rosters: List<RosterEntity>) {
        rosterDao.insertAll(rosters)
    }

    suspend fun insert(roster: RosterEntity) {
        rosterDao.insert(roster)
    }

    suspend fun update(roster: RosterEntity) {
        rosterDao.update(roster)
    }

    suspend fun delete(roster: RosterEntity) {
        rosterDao.delete(roster)
    }

    suspend fun deleteAll() {
        rosterDao.deleteAll()
    }

    suspend fun getRosterCount(): Int {
        return rosterDao.getRosterCount()
    }
}
