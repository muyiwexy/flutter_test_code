package com.example.test.flutter_test_code

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.math.PI

class MainActivity : FlutterActivity() {
    object WindowSetup {
        var positionGravity: String = "auto" // Possible values: "auto", "left", "right"
        var gravity = Gravity.CENTER
    }

    private companion object {
        const val CHANNEL_NAME = "com.example/my_channel"
        const val SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 1000
        const val BUTTON_SIZE = 220
        const val ANIMATION_DURATION = 300L
        const val RADIUS = 250f
    }

    private var isButtonOpen = false
    private var progress:Float=0f
    private var mainButton: CustomLinearLayout? = null // Ensure this is correctly typed
    private var camButton: CustomLinearLayout? = null
    private val additionalButtons = mutableListOf<View>()
    private var isOverlayVisible = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME).setMethodCallHandler { call, result ->
            when (call.method) {
                "createNativeButton" -> handleCreateNativeButton(result)
                "setThumbnail" -> showPath(call)
                else -> result.notImplemented()
            }
        }
    }

    private fun showPath(call: MethodCall) {
        val imagePath: String = call.arguments as String
        setButtonBackground(imagePath)
    }
    private fun setButtonBackground(imagePath: String) {
        val imgFile = File(imagePath)

        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            Log.d("BitMap", "$bitmap")
            runOnUiThread {
                val image = camButton?.findViewById<ImageView>(R.id.cam_image)
                image?.let {
                    it.setBackgroundResource(R.drawable.rounded_button_background_green)
                    it.setImageBitmap(bitmap)
                    it.setColorFilter(Color.TRANSPARENT)
                    it.clipToOutline = true

                    val camParams = it.layoutParams
                    camParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    camParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    it.layoutParams = camParams
                } ?: Log.d("MainActivity", "ImageView with ID R.id.add not found.")
            }
//            runOnUiThread {
//                val image = mainButton?.findViewById<ImageView>(R.id.add)
//                image?.let {
//                    it.setBackgroundResource(R.drawable.rounded_button_background_green)
//                    it.setImageResource(R.drawable.vynestudio)
//                    it.setColorFilter(Color.TRANSPARENT)
//                    it.clipToOutline = true
//
//                    val params = it.layoutParams
//                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
//                    it.layoutParams = params
//                } ?: Log.d("MainActivity", "ImageView with ID R.id.add not found.")
//            }
        } else {
            Log.d("MainActivity", "Image file does not exist at path: $imagePath")
        }
    }


    private fun handleCreateNativeButton(result: MethodChannel.Result) {
        if (isOverlayVisible) {
            // If the overlay is already visible, remove it
            removeNativeButton()
            disposeState()
        } else {
            // Otherwise, create a new overlay if permission exists
            if (hasOverlayPermission()) {
                createNativeButton()

            } else {
                requestOverlayPermission()
                result.error("PERMISSION_DENIED", "Overlay permission is not granted", null)
            }
        }
        result.success(null)
    }

    private fun removeNativeButton() {
        // Remove main button from the window
        if (mainButton != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(mainButton)
            mainButton = null
        }
        // Clear additional buttons if any
        for (button in additionalButtons) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(button)
        }
        additionalButtons.clear()
    }

    private fun createNativeButton() {
        isOverlayVisible = true
        createButtons()

        val layoutInflater = LayoutInflater.from(this)
        val inflatedViewGroup = LinearLayout(this)
        // Inflate the XML layout you provided
        val nativeButtonView = layoutInflater.inflate(R.layout.open_button, inflatedViewGroup, false)

        // Find the CustomFrameLayout from your inflated layout
        val ellipseImageView = nativeButtonView.findViewById<CustomLinearLayout>(R.id.primary)
        mainButton = ellipseImageView.apply {
            setOnClickListener {
                if (!isDragging) {
                    toggleButtonAnimation()
                }
            }

            var isDragging = false
            var lastTouchX = 0f
            var lastTouchY = 0f
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        isDragging = false
                        true // Event handled
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isButtonOpen) return@setOnTouchListener false
                        val params = layoutParams as WindowManager.LayoutParams
                        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

                        // Update movement
                        val dx = event.rawX - lastTouchX
                        val dy = event.rawY - lastTouchY
                        val newY = params.y + dy.toInt()

                        val safeAreaTop = 200
                        val safeAreaBottom = Resources.getSystem().displayMetrics.heightPixels - BUTTON_SIZE - 200

                        if (newY in safeAreaTop..safeAreaBottom) {
                            params.x = (params.x + dx.toInt()).coerceIn(0, Resources.getSystem().displayMetrics.widthPixels - BUTTON_SIZE)
                            params.y = (params.y + dy.toInt()).coerceIn(0, Resources.getSystem().displayMetrics.heightPixels - BUTTON_SIZE)
                            windowManager.updateViewLayout(view, params)

                            lastTouchX = event.rawX
                            lastTouchY = event.rawY
                            isDragging = true
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            val params = layoutParams as WindowManager.LayoutParams
                            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                            val destX = calculateSnapPosition(params.x, Resources.getSystem().displayMetrics.widthPixels)
                            snapToPosition(params, destX, params.y, windowManager)
                        } else {
                            performClick()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        val params = getWindowLayoutParams()
        addViewToWindow(mainButton!!, params)
    }

    private fun calculateSnapPosition(x: Int, screenWidth: Int): Int {
        val destX = when (WindowSetup.positionGravity) {
            "left" -> 0
            "right" -> screenWidth - BUTTON_SIZE
            "auto" -> {
                if (x + BUTTON_SIZE / 2 <= screenWidth / 2) 0 else screenWidth - BUTTON_SIZE
            }
            else -> x
        }
        return destX
    }

    private fun snapToPosition(params: WindowManager.LayoutParams, destX: Int, destY: Int, windowManager: WindowManager) {
        val animationHandler = Looper.getMainLooper()

        // Smoothly snap using a timer/handler
        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                Handler(animationHandler).post {
                    // Gradual transition formula
                    params.x = (2 * (params.x - destX)) / 3 + destX
                    params.y = (2 * (params.y - destY)) / 3 + destY

                    windowManager.updateViewLayout(mainButton, params)

                    // Stop the timer when snapping position is reached
                    if (kotlin.math.abs(params.x - destX) < 2 && kotlin.math.abs(params.y - destY) < 2) {
                        timer.cancel()
                    }
                }
            }
        }
        timer.schedule(task, 0, 25) // Schedule the snapping animation
    }

    private fun getWindowLayoutParams(): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 200 // Safe area offset
            }
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }

    private fun addViewToWindow(customShapeView: View, params: WindowManager.LayoutParams) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(customShapeView, params)
    }

    private fun toggleButtonAnimation() {
        isButtonOpen = !isButtonOpen // Toggle the button state

        mainButton?.let { button ->
            // Find the ImageView inside the CustomFrameLayout
            val addImageView = button.findViewById<ImageView>(R.id.add)

            // Create scale and rotation animators for the ImageView
            val scaleX = if (isButtonOpen) 0.8f else 1.0f
            val scaleY = if (isButtonOpen) 0.8f else 1.0f
            val rotationAngle = if (isButtonOpen) 45f else 0f // Rotate 45 degrees when opened

            val scaleAnimatorX = ObjectAnimator.ofFloat(button, "scaleX", scaleX)
            val scaleAnimatorY = ObjectAnimator.ofFloat(button, "scaleY", scaleY)
            val rotationAnimator = ObjectAnimator.ofFloat(addImageView, "rotation", rotationAngle)

            val colorAnimator = if (isButtonOpen) {
                createColorAnimator("#EC3A6A", "#939296", addImageView)
            } else {
                createColorAnimator("#939296", "#EC3A6A", addImageView)
            }

            // Combine all animations into a single AnimatorSet
            val animatorSet = AnimatorSet().apply {
                playTogether(scaleAnimatorX, scaleAnimatorY, rotationAnimator, colorAnimator)
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Start the animations
            animatorSet.start()

            // Utilize the custom animator purely for tracking animation state
            val customAnimator = CustomAnimator(ANIMATION_DURATION)
            if (isButtonOpen) {
                Log.d("Animator value", "forward")
                customAnimator.startForward() // Indicate starting forward (0 to 1)
            } else {
                Log.d("Animator value", "reverse")
                customAnimator.startReverse() // Indicate starting in reverse (1 to 0)
            }
        }
    }

    private fun createColorAnimator(
        startColor: String,
        endColor: String,
        targetView: ImageView,
        duration: Long = 250
    ): ValueAnimator? {
        return ObjectAnimator.ofArgb(Color.parseColor(startColor), Color.parseColor(endColor)).apply {
            this.duration = duration
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                targetView.setColorFilter(animatedColor) // Apply animated color
            }
        }
    }

    data class ButtonConfig(
        val layoutRes: Int,
        val viewId: Int
    )

    private fun createButtons() {
        // Define the button configurations
        val buttonConfigs = listOf(
            ButtonConfig(R.layout.video_button, R.id.video),
            ButtonConfig(R.layout.camera_button, R.id.camera),
            ButtonConfig(R.layout.prep, R.id.prep),
            ButtonConfig(R.layout.begin, R.id.begin)
        )

        // Get the current layout parameters
        val params = mainButton?.layoutParams as? WindowManager.LayoutParams
        val currentX = params?.x ?: 20
        val currentY = params?.y ?: 100

        // Initialize a layout inflater
        val layoutInflater = LayoutInflater.from(this)

        // Iterate over each button configuration and create buttons
        for (config in buttonConfigs) {
            val inflatedViewGroup = LinearLayout(this)

            // Inflate the XML layout provided
            val nativeButtonView = layoutInflater.inflate(config.layoutRes, inflatedViewGroup, false)

            // Find the CustomFrameLayout from your inflated layout
            val ellipseImageView = nativeButtonView.findViewById<CustomLinearLayout>(config.viewId)
            ellipseImageView.alpha = 0f

            // Apply different actions or properties based on the button type
            when (config.viewId) {
                R.id.video -> {
                    ellipseImageView.apply {
                        setOnClickListener {
                            flutterEngine?.dartExecutor?.binaryMessenger?.let { it1 ->
                                MethodChannel(
                                    it1, CHANNEL_NAME).invokeMethod("openGallery", null)
                            }
                            // Add additional actions specific to the video button
                        }
                        // Set properties specific to the video button, if needed
                    }
                }
                R.id.camera -> {
                    camButton = ellipseImageView.apply {
                        setOnClickListener {
                            flutterEngine?.dartExecutor?.binaryMessenger?.let { it1 ->
                                MethodChannel(
                                    it1, CHANNEL_NAME).invokeMethod("openCamera", null)
                            }
                            // Add additional actions specific to the camera button
                        }
                    }
                }
                R.id.prep -> {
                    ellipseImageView.apply {
                        setOnClickListener {
                            Log.d("Button Click", "Prep button clicked")
                            // Add additional actions specific to the prep button
                        }
                        // Set properties specific to the prep button, if needed
                    }
                }
                R.id.begin -> {
                    ellipseImageView.apply {
                        setOnClickListener {
                            Log.d("Button Click", "Begin button clicked")
                            // Add additional actions specific to the begin button
                        }
                        // Set properties specific to the begin button, if needed
                    }
                }
                else -> {
                    Log.w("Warning", "Unhandled button type: ${config.viewId}")
                }
            }

            // Set the layout parameters and add the view to the window
            val buttonParams = getInitialButtonLayoutParams(currentX, currentY)
            addViewToWindow(ellipseImageView, buttonParams)
            additionalButtons.add(ellipseImageView)
        }
    }
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasOverlayPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(this)
    } else {
        true // Automatically granted on versions below Marshmallow
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE) {
            if (hasOverlayPermission()) {
                createNativeButton()
                isOverlayVisible = true
            } else {
                // Consider showing a message to the user about the permission denial
            }
        }
    }

    private fun disposeState() {
        // Reset all variables and state
        mainButton = null
        additionalButtons.clear()
        isOverlayVisible = false
        progress = 0f // Reset progress for animations
        Log.d("Dispose", "All state and resources have been disposed.")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure cleanup happens if the app is destroyed
        removeNativeButton()
        disposeState()
    }

    inner class CustomAnimator(duration: Long) {
        private val animator = ValueAnimator()

        init {
            animator.duration = duration
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener { animation ->
               progress = animation.animatedValue as Float
                updateButtonPositions()
                Log.d("progress", "$progress")
            }
        }
        private fun updateButtonPositions() {
            val params = mainButton?.layoutParams as? WindowManager.LayoutParams
            val currentX = params?.x ?: 20
            val currentY = params?.y ?: 100

            // Determine if the main button is positioned to the left half of the screen
            val isGravityLeft = currentX < Resources.getSystem().displayMetrics.widthPixels / 2

            val startAngle = 90.0
            val endAngle = -90.0
            val stepAngle = (endAngle - startAngle) / additionalButtons.size
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            for (i in additionalButtons.indices) {
                // Calculate current angle
                val angleInDegrees = startAngle + (i * stepAngle)
                val adjustedAngle = (180.0 - angleInDegrees) * (PI / 180)

                // Calculate offsets dynamically
                val offset = Offset.fromDirection(adjustedAngle, progress * RADIUS.toDouble())
                val buttonParams = additionalButtons[i].layoutParams as WindowManager.LayoutParams

                // Dynamically calculate the new positions
                buttonParams.x = if (isGravityLeft) {
                    -(offset.x.toInt() + currentX)
                } else {
                    offset.x.toInt() + currentX
                }
                buttonParams.y = offset.y.toInt() + currentY+20

                // Apply alpha based on progress
                additionalButtons[i].alpha = progress

                // Calculate rotation
                val rotation = (1.0 - progress) * PI / 2 * (180 / PI) // Convert to degrees

                // Apply rotation to each button
                additionalButtons[i].rotation = rotation.toFloat()

                // Update the layout parameters
                windowManager.updateViewLayout(additionalButtons[i], buttonParams)
            }
        }


        fun startForward() {
            animator.setFloatValues(0f, 1f)
            animator.start()
        }

        fun startReverse() {
            animator.setFloatValues(1f, 0f)
            animator.start()
        }
    }

    private fun getInitialButtonLayoutParams(currentX: Int, currentY: Int): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
//                width = BUTTON_SIZE
//                height = BUTTON_SIZE
                gravity = Gravity.TOP or Gravity.START
                x = currentX
                y = currentY
            }
        } else {
            throw UnsupportedOperationException("Overlay permission for SDK < O is not handled.")
        }
    }
}