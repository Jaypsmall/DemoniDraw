package com.jaylizapp.demonidraw.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.gesture.Gesture
import android.gesture.GestureOverlayView
import android.os.Build
import android.os.IBinder
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.jaylizapp.demonidraw.R
import com.jaylizapp.demonidraw.data.AppDatabase
import com.jaylizapp.demonidraw.util.GestureManager
import com.jaylizapp.demonidraw.util.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingService : Service(), GestureOverlayView.OnGesturePerformedListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingTrigger: View
    private lateinit var gestureOverlay: GestureOverlayView
    private lateinit var gestureManager: GestureManager
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        gestureManager = GestureManager(this)
        
        startForegroundService()
        createFloatingTrigger()
        createGestureOverlay()
    }

    private fun startForegroundService() {
        val channelId = "floating_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Floating Service",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Demonidraw")
            .setContentText("Desliza a la izquierda para dibujar")
            .setSmallIcon(R.drawable.demonidraw_icon)
            .build()

        startForeground(1, notification)
    }

    private fun createFloatingTrigger() {
        floatingTrigger = ImageView(this).apply {
            setImageResource(R.drawable.demonidraw_icon)
            setBackgroundColor(Color.parseColor("#AA000000")) 
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0.7f
        }

        val params = WindowManager.LayoutParams(
            80, // Un poco más ancho para facilitar el toque
            250,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 50
            private val SWIPE_VELOCITY_THRESHOLD = 50

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffX = (e1?.x ?: 0f) - e2.x
                if (diffX > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    showPizarra()
                    return true
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val intent = Intent(this@FloatingService, com.jaylizapp.demonidraw.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                return true
            }
        })

        floatingTrigger.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        windowManager.addView(floatingTrigger, params)
    }

    private fun createGestureOverlay() {
        gestureOverlay = GestureOverlayView(this).apply {
            gestureColor = Color.YELLOW
            uncertainGestureColor = Color.argb(100, 255, 255, 0)
            gestureStrokeWidth = 15f
            // Fundamental: configurar el mismo tipo de trazo que al guardar
            gestureStrokeType = GestureOverlayView.GESTURE_STROKE_TYPE_MULTIPLE
            orientation = GestureOverlayView.ORIENTATION_HORIZONTAL
            isEventsInterceptionEnabled = true
            setBackgroundColor(Color.argb(100, 0, 0, 0))
            addOnGesturePerformedListener(this@FloatingService)
            visibility = View.GONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Quitamos el forzado de orientación aquí para evitar el giro de la interfaz
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        val overlayDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                hidePizarra()
                return true
            }
        })
        
        gestureOverlay.setOnTouchListener { _, event ->
            overlayDetector.onTouchEvent(event)
            false 
        }

        windowManager.addView(gestureOverlay, params)
    }

    private fun showPizarra() {
        gestureOverlay.visibility = View.VISIBLE
        val params = gestureOverlay.layoutParams as WindowManager.LayoutParams
        // Quitamos FLAG_NOT_FOCUSABLE para que pueda capturar gestos, pero mantenemos la transparencia
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        windowManager.updateViewLayout(gestureOverlay, params)
    }

    private fun hidePizarra() {
        gestureOverlay.visibility = View.GONE
        val params = gestureOverlay.layoutParams as WindowManager.LayoutParams
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        windowManager.updateViewLayout(gestureOverlay, params)
    }

    override fun onGesturePerformed(overlay: GestureOverlayView?, gesture: Gesture?) {
        gesture?.let {
            val gestureName = gestureManager.recognize(it)
            if (gestureName != null) {
                executeAction(gestureName)
            } else {
                Toast.makeText(this, "Gesto no reconocido", Toast.LENGTH_SHORT).show()
            }
        }
        hidePizarra()
    }

    private fun executeAction(name: String) {
        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val entry = db.gestureDao().getGestureByName(name)
            
            withContext(Dispatchers.Main) {
                if (entry != null) {
                    if (entry.isShellCommand) {
                        serviceScope.launch(Dispatchers.IO) {
                            val success = ShellUtils.executeCommand(entry.action)
                            withContext(Dispatchers.Main) {
                                if (!success) {
                                    Toast.makeText(this@FloatingService, "Error ejecutando comando root", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        Toast.makeText(this@FloatingService, "Ejecutando: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        // Lanzar aplicación por nombre de paquete
                        val launchIntent = packageManager.getLaunchIntentForPackage(entry.action.trim())
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                            Toast.makeText(this@FloatingService, "Abriendo: $name", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@FloatingService, "No se pudo abrir la app: ${entry.action}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        if (::floatingTrigger.isInitialized) windowManager.removeView(floatingTrigger)
        if (::gestureOverlay.isInitialized) windowManager.removeView(gestureOverlay)
    }
}
