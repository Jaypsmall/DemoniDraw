package com.jaylizapp.demonidraw.util

import android.content.Context
import com.jaylizapp.demonidraw.service.DemoniAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HybridEngine {

    private var rootAvailable: Boolean? = null

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (rootAvailable == null) {
            rootAvailable = ShellUtils.executeCommand("id")
        }
        rootAvailable!!
    }

    suspend fun executeAction(context: Context, action: String, isShellCommand: Boolean) {
        if (isRootAvailable()) {
            withContext(Dispatchers.IO) {
                ShellUtils.executeCommand(action)
            }
        } else {
            // Motor híbrido: Si no hay root, intentamos vía Accessibility
            val accessibilityService = DemoniAccessibilityService.getInstance()
            if (accessibilityService != null) {
                when {
                    action.contains("input keyevent 4") || action.equals("back", ignoreCase = true) -> {
                        accessibilityService.performBack()
                    }
                    action.contains("input keyevent 3") || action.equals("home", ignoreCase = true) -> {
                        accessibilityService.performHome()
                    }
                    action.contains("input keyevent 187") || action.equals("recents", ignoreCase = true) -> {
                        accessibilityService.performRecents()
                    }
                    action.startsWith("input tap") -> {
                        val parts = action.split(" ")
                        if (parts.size >= 4) {
                            val x = parts[2].toFloatOrNull() ?: 0f
                            val y = parts[3].toFloatOrNull() ?: 0f
                            accessibilityService.performClick(x, y)
                        }
                    }
                    else -> {
                        // Si es un nombre de paquete, intentamos lanzarlo normalmente
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.trim())
                        launchIntent?.let {
                            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(it)
                        }
                    }
                }
            } else {
                // Si no hay accesibilidad activa, solo intentamos lanzar la app si no es shell
                if (!isShellCommand) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(action.trim())
                    launchIntent?.let {
                        it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
            }
        }
    }
}
