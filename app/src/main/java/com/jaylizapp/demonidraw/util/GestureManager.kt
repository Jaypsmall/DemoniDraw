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
            val bestMatch = predictions[0]
            if (bestMatch.score > 1.0) { // Threshold for recognition
                return bestMatch.name
            }
        }
        return null
    }
    
    fun getGestures(name: String): List<Gesture>? {
        return gestureLibrary.getGestures(name)
    }
}
