package com.example.test.flutter_test_code

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class CustomLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var isDragging: Boolean = false // Custom tracking property, if necessary

    init {
        isClickable = true // Mark this view as clickable for proper interactions
    }

    override fun performClick(): Boolean {
        // Call the super method to handle accessibility and click events
        super.performClick()

        // Add any custom behavior here if needed (optional)
        return true
    }
}