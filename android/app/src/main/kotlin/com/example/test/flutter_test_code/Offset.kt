package com.example.test.flutter_test_code

import kotlin.math.cos
import kotlin.math.sin

data class Offset(val x: Double, val y: Double) {
    companion object {
        fun fromDirection(directionRadians: Double, distance: Double): Offset {
            val x = distance * cos(directionRadians)
            val y = distance * sin(directionRadians)
            return Offset(x, y)
        }
    }
}
