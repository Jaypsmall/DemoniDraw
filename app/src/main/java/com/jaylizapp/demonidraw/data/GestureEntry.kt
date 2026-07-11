package com.jaylizapp.demonidraw.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gestures")
data class GestureEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val action: String, // Command or package name
    val isShellCommand: Boolean = true
)
