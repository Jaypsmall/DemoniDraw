package com.jaylizapp.demonidraw.util

import android.content.Context
import android.gesture.Gesture
import android.gesture.GestureLibrary
import android.gesture.GestureLibraries
import java.io.File

class GestureManager(private val context: Context) {
    private val gestureLibrary: GestureLibrary

    init {
        val file = File(context.filesDir, "gestures")
        gestureLibrary = GestureLibraries.fromFile(file)
        gestureLibrary.load()
    }

    fun addGesture(name: String, gesture: Gesture) {
        // Importante: Limpiamos gestos anteriores con el mismo nombre para evitar duplicados que confundan al motor
        gestureLibrary.removeEntry(name)
        gestureLibrary.addGesture(name, gesture)
        gestureLibrary.save()
        // Forzamos recarga tras guardar
        gestureLibrary.load()
    }

    fun removeGesture(name: String, gesture: Gesture) {
        gestureLibrary.removeGesture(name, gesture)
        gestureLibrary.save()
        gestureLibrary.load()
    }
    
    fun removeAllGestures(name: String) {
        gestureLibrary.removeEntry(name)
        gestureLibrary.save()
        gestureLibrary.load()
    }

    fun recognize(gesture: Gesture): String? {
        // Recargamos antes de reconocer para asegurar que tenemos los últimos cambios
        gestureLibrary.load()
        val predictions = gestureLibrary.recognize(gesture)
        if (predictions.isNotEmpty()) {
            val bestMatch = predictions[0]
            
            android.util.Log.d("GestureManager", "Analizando: ${bestMatch.name} - Score: ${bestMatch.score}")
            
            // Bajamos a 1.0 para que sea más permisivo pero siga siendo preciso
            if (bestMatch.score > 1.0) {
                return bestMatch.name
            }
        }
        return null
    }
    
    fun getGestures(name: String): List<Gesture>? {
        return gestureLibrary.getGestures(name)
    }
}
