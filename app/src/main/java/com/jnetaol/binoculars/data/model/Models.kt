package com.jnetaol.binoculars.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class CapturedPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val zoomLevel: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
