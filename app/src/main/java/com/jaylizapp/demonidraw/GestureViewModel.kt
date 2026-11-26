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
import org.json.JSONArray
import org.json.JSONObject

class GestureViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.gestureDao()
    private val gestureManager = GestureManager(application)

    val gestures: StateFlow<List<GestureEntry>> = dao.getAllGestures()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            if (dao.getCount() == 0) {
                dao.insertGesture(GestureEntry(name = "CÁMARA", action = "com.android.camera", isShellCommand = false))
                dao.insertGesture(GestureEntry(name = "REBOOT", action = "reboot", isShellCommand = true))
                dao.insertGesture(GestureEntry(name = "AJUSTES", action = "com.android.settings", isShellCommand = false))
                dao.insertGesture(GestureEntry(name = "WHATSAPP", action = "com.whatsapp", isShellCommand = false))
                dao.insertGesture(GestureEntry(name = "ROOT SHELL", action = "su", isShellCommand = true))
            }
        }
    }

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

    fun exportGesturesToJson(): String {
        val array = JSONArray()
        gestures.value.forEach { gesture ->
            val obj = JSONObject().apply {
                put("name", gesture.name)
                put("action", gesture.action)
                put("isShell", gesture.isShellCommand)
            }
            array.put(obj)
        }
        return array.toString(4)
    }

    fun importGesturesFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.getString("name")
                    val action = obj.getString("action")
                    val isShell = obj.getBoolean("isShell")
                    
                    // Solo insertamos si no existe ya uno con el mismo nombre para evitar duplicados
                    // O podemos simplemente insertar, Room se encarga si tenemos conflictos definidos
                    dao.insertGesture(GestureEntry(name = name, action = action, isShellCommand = isShell))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
