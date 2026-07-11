package com.jaylizapp.demonidraw

import androidx.activity.ComponentActivity
import android.gesture.Gesture
import android.gesture.GestureOverlayView
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.jaylizapp.demonidraw.data.AppDatabase
import com.jaylizapp.demonidraw.util.GestureManager
import com.jaylizapp.demonidraw.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent

class DrawingActivity : ComponentActivity(), GestureOverlayView.OnGesturePerformedListener {

    private lateinit var gestureManager: GestureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Creamos la vista de la pizarra
        val overlay = GestureOverlayView(this).apply {
            gestureColor = Color.YELLOW
            uncertainGestureColor = Color.argb(100, 255, 255, 0)
            gestureStrokeWidth = 15f
            isEventsInterceptionEnabled = true
            orientation = GestureOverlayView.ORIENTATION_HORIZONTAL
            addOnGesturePerformedListener(this@DrawingActivity)
            // Color de fondo muy ligero para que se note la pizarra pero se vea lo de atrás
            setBackgroundColor(Color.argb(40, 0, 0, 0)) 
        }
        
        // Detector para cerrar con doble clic
        val doubleTapDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                finish()
                return true
            }
        })

        overlay.setOnTouchListener { _, event ->
            doubleTapDetector.onTouchEvent(event)
            false // Importante: false para que el dibujo siga funcionando
        }

        setContentView(overlay)
        gestureManager = GestureManager(this)
    }

    override fun onGesturePerformed(overlay: GestureOverlayView?, gesture: Gesture?) {
        gesture?.let {
            val gestureName = gestureManager.recognize(it)
            if (gestureName != null) {
                executeGestureAction(gestureName)
            } else {
                Toast.makeText(this, "Gesto no reconocido", Toast.LENGTH_SHORT).show()
                // No cerramos inmediatamente aquí por si el usuario quiere intentar de nuevo
                // Pero en este caso, cerraremos para no dejar la pantalla bloqueada
                finish()
            }
        }
    }

    private fun executeGestureAction(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val entry = db.gestureDao().getGestureByName(name)
            
            withContext(Dispatchers.Main) {
                if (entry != null) {
                    if (entry.isShellCommand) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            ShellUtils.executeCommand(entry.action)
                        }
                        Toast.makeText(this@DrawingActivity, "Ejecutando: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        val launchIntent = packageManager.getLaunchIntentForPackage(entry.action)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    }
                }
                finish()
            }
        }
    }
}
