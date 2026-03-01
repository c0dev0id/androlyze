package com.c0dev0id.androlyze.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usb_events")
data class UsbEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val action: String,       // "ATTACHED" or "DETACHED"
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val description: String
)
