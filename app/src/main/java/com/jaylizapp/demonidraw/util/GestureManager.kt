package com.jaylizapp.demonidraw.util

import android.content.Context
import android.gesture.Gesture
import android.gesture.GestureLibrary
import android.gesture.GestureLibraries
import android.gesture.GestureOverlayView
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
    }

    fun removeGesture(name: String, gesture: Gesture) {
        gestureLibrary.removeGesture(name, gesture)
        gestureLibrary.save()
    }
    
    fun removeAllGestures(name: String) {
        gestureLibrary.removeEntry(name)
        gestureLibrary.save()
    }

    fun recognize(gesture: Gesture): String? {
        val predictions = gestureLibrary.recognize(gesture)
        if (predictions.isNotEmpty()) {
            // Ordenamos por puntuación de mayor a menor (ya vienen ordenadas, pero aseguramos)
            val bestMatch = predictions[0]
            
            android.util.Log.d("GestureManager", "Analizando gesto...")
            predictions.forEach { 
                android.util.Log.d("GestureManager", "Candidato: ${it.name} - Score: ${it.score}")
            }

            // Un score de 1.0 suele ser una coincidencia pobre si hay muchos gestos.
            // Subimos a un mínimo de 1.2 para evitar que ejecute el primero que pille por defecto.
            if (bestMatch.score > 1.2) {
                return bestMatch.name
            }
        }
        return null
    }
    
    fun getGestures(name: String): List<Gesture>? {
        return gestureLibrary.getGestures(name)
    }
}
