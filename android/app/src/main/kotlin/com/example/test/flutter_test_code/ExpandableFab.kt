package com.example.test.flutter_test_code


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ExpandableFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var directionDegrees: Float = 0f
    private var maxDistance: Float = 0f
    private var top: Float = 0f
    private var isOnRightSide: Boolean = false
    private var isAbove: Boolean = false
    var progress: Float = 0f
        set(value) {
            field = value
            invalidate() // Redraw the view when progress changes
        }

    private val paint = Paint().apply {
        color = 0xFF6200EE.toInt() // Example color
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radians = Math.toRadians(directionDegrees.toDouble())

        val offsetX = (cos(radians) * maxDistance * progress).toFloat()
        val offsetY = (sin(radians) * maxDistance * progress).toFloat()

        val adjustedTop = if (isAbove) top + offsetY else top - offsetY
        val adjustedLeft = if (isOnRightSide) width - offsetX else 20f + offsetX

        canvas.save()
        canvas.translate(adjustedLeft, adjustedTop)
        canvas.rotate((1.0f - progress) * 90) // Rotate by 90 degrees
        canvas.drawCircle(0f, 0f, 50f, paint) // Example shape to represent FAB
        canvas.restore()
    }

    fun animateFab(duration: Long) {
        val animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                progress = interpolatedTime
            }
        }

        animation.duration = duration
        animation.interpolator = LinearInterpolator()
        this.startAnimation(animation)
    }
}