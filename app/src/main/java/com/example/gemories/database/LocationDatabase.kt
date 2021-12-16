package com.example.gemories.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase

@Database(entities = [LocationsTable::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDAO():DAO
    companion object{
        private const val databaseName = "Locations-Database"
        private var myDatabase : LocationDatabase?=null
        fun getInstance(context:Context):LocationDatabase{
            if (myDatabase==null){
                myDatabase = databaseBuilder(
                    context,
                    LocationDatabase::class.java,
                    databaseName
                ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
            }
            return myDatabase!!
        }
    }
}