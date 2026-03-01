package com.c0dev0id.androlyze.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsbEventDao {
    @Insert
    suspend fun insert(event: UsbEvent)

    @Query("SELECT * FROM usb_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<UsbEvent>>

    @Query("DELETE FROM usb_events")
    suspend fun deleteAll()
}
