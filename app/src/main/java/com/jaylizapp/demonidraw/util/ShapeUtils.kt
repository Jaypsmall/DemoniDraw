package com.jaylizapp.demonidraw.util

import android.gesture.Gesture
import android.gesture.GesturePoint
import android.gesture.GestureStroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ShapeUtils {

    fun createSquare(): Gesture = createGestureFromPoints(listOf(
        100f to 100f, 400f to 100f, 400f to 400f, 100f to 400f, 100f to 100f
    ))

    fun createTriangle(): Gesture = createGestureFromPoints(listOf(
        250f to 100f, 450f to 400f, 50f to 400f, 250f to 100f
    ))

    fun createVShape(): Gesture = createGestureFromPoints(listOf(
        100f to 100f, 250f to 400f, 400f to 100f
    ))

    fun createZShape(): Gesture = createGestureFromPoints(listOf(
        100f to 100f, 400f to 100f, 100f to 400f, 400f to 400f
    ))

    fun createCircle(): Gesture {
        val points = mutableListOf<Pair<Float, Float>>()
        val centerX = 250f
        val centerY = 250f
        val radius = 150f
        val segments = 24
        for (i in 0..segments) {
            val angle = 2 * PI * i / segments
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            points.add(x to y)
        }
        return createGestureFromPoints(points)
    }

    private fun createGestureFromPoints(points: List<Pair<Float, Float>>): Gesture {
        val gesture = Gesture()
        val gesturePoints = points.mapIndexed { index, pair ->
            GesturePoint(pair.first, pair.second, index * 50L)
        }
        gesture.addStroke(GestureStroke(ArrayList(gesturePoints)))
        return gesture
    }
}
