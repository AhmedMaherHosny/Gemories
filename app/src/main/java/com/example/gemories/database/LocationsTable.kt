package com.example.gemories.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity
data class LocationsTable(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    @Embedded val location: LatLng
)
