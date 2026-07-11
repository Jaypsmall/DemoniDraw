package com.jaylizapp.demonidraw.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Query("SELECT * FROM gestures")
    fun getAllGestures(): Flow<List<GestureEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGesture(gesture: GestureEntry): Long

    @Update
    suspend fun updateGesture(gesture: GestureEntry)

    @Delete
    suspend fun deleteGesture(gesture: GestureEntry)

    @Query("SELECT * FROM gestures WHERE name = :name LIMIT 1")
    suspend fun getGestureByName(name: String): GestureEntry?
}
