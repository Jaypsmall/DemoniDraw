package com.jaylizapp.demonidraw

import android.gesture.Gesture
import android.gesture.GestureOverlayView
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.jaylizapp.demonidraw.util.GestureManager

class AddGestureActivity : ComponentActivity(), GestureOverlayView.OnGesturePerformedListener {

    private lateinit var gestureManager: GestureManager
    private var gestureName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        gestureName = intent.getStringExtra("GESTURE_NAME")
        if (gestureName == null) {
            finish()
            return
        }

        val overlay = GestureOverlayView(this).apply {
            gestureColor = android.graphics.Color.YELLOW
            uncertainGestureColor = android.graphics.Color.GRAY
            gestureStrokeWidth = 12f
            // Permitir múltiples trazos para gestos más complejos
            gestureStrokeType = GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE
            addOnGesturePerformedListener(this@AddGestureActivity)
        }
        
        setContentView(overlay)
        Toast.makeText(this, "Draw gesture for $gestureName", Toast.LENGTH_LONG).show()
        
        gestureManager = GestureManager(this)
    }

    override fun onGesturePerformed(overlay: GestureOverlayView?, gesture: Gesture?) {
        gesture?.let {
            gestureManager.addGesture(gestureName!!, it)
            Toast.makeText(this, "Gesture saved", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
