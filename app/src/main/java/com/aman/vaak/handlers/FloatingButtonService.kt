package com.aman.vaak.handlers

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import com.aman.vaak.R
import com.aman.vaak.managers.KeyboardManager
import com.aman.vaak.managers.NotifyManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FloatingButtonService : Service() {
    @Inject
    lateinit var windowManager: WindowManager

    @Inject
    lateinit var keyboardManager: KeyboardManager

    private var floatingButton: View? = null
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (floatingButton == null) {
            createFloatingButton()
        }
        return START_STICKY
    }

    companion object {
        private const val INITIAL_BUTTON_Y_POSITION = 100
        private const val DRAG_THRESHOLD_PX = 5
    }

    private fun createFloatingButton() {
        val layoutParams =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = INITIAL_BUTTON_Y_POSITION
            }

        floatingButton = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        floatingButton?.findViewById<ImageButton>(R.id.floating_keyboard_button)?.apply {
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        false // Don't consume DOWN event
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Only consume MOVE if we're actually dragging
                        val isDragging =
                            Math.abs(event.rawX - initialTouchX) > DRAG_THRESHOLD_PX ||
                                Math.abs(event.rawY - initialTouchY) > DRAG_THRESHOLD_PX
                        if (isDragging) {
                            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(floatingButton, layoutParams)
                            true // Consume only if dragging
                        } else {
                            false
                        }
                    }
                    else -> false // Don't consume other events
                }
            }

            setOnClickListener {
                toggleKeyboard()
            }
        }

        windowManager.addView(floatingButton, layoutParams)
    }

    @Inject
    lateinit var notifyManager: NotifyManager

    private fun toggleKeyboard() {
        try {
            keyboardManager.showKeyboardSelector()
        } catch (e: Exception) {
            notifyManager.showError(
                title = getString(R.string.error_keyboard_selector),
                message = e.message ?: getString(R.string.error_generic_details),
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingButton?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
