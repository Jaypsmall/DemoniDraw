package com.jaylizapp.demonidraw

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaylizapp.demonidraw.data.AppDatabase
import com.jaylizapp.demonidraw.data.GestureEntry
import com.jaylizapp.demonidraw.util.GestureManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GestureViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.gestureDao()
    private val gestureManager = GestureManager(application)

    val gestures: StateFlow<List<GestureEntry>> = dao.getAllGestures()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addGesture(name: String, action: String, isShell: Boolean) {
        viewModelScope.launch {
            dao.insertGesture(GestureEntry(name = name, action = action, isShellCommand = isShell))
        }
    }

    fun updateGesture(gesture: GestureEntry) {
        viewModelScope.launch {
            dao.updateGesture(gesture)
        }
    }

    fun deleteGesture(gesture: GestureEntry) {
        viewModelScope.launch {
            gestureManager.removeAllGestures(gesture.name)
            dao.deleteGesture(gesture)
        }
    }
}
