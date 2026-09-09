package com.jaylizapp.demonidraw.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class DemoniAccessibilityService : AccessibilityService() {

    companion object {
        private var sInstance: DemoniAccessibilityService? = null
        
        fun getInstance(): DemoniAccessibilityService? = sInstance
        
        fun isRunning(): Boolean = sInstance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sInstance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        sInstance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sInstance = null
    }

    fun performClick(x: Float, y: Float) {
        val clickPath = Path()
        clickPath.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(clickPath, 0, 10))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
}
