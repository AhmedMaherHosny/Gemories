package com.example.gemories.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DAO {
    @Insert
    fun addLocation(location: LocationsTable)
    @Query("SELECT * FROM LocationsTable")
    fun getAllLocations():List<LocationsTable>
    @Query("SELECT COUNT() FROM LocationsTable WHERE latitude = :lat AND longitude = :lon")
    fun count(lat: Double, lon: Double): Int
    @Query("SELECT COUNT(id) FROM LocationsTable WHERE id = 1")
    fun checkIfEmpty(): Int
}