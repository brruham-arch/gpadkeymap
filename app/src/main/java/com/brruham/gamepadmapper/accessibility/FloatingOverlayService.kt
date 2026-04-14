package com.brruham.gamepadmapper.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.brruham.gamepadmapper.R

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: FrameLayout
    private var isActive = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            isActive = intent.getBooleanExtra("enabled", false)
            updateIndicator()
        }
    }

    companion object {
        private const val CHANNEL_ID = "gm_overlay"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatView()

        registerReceiver(
            statusReceiver,
            IntentFilter("com.brruham.gamepadmapper.STATUS"),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatView.isInitialized) windowManager.removeView(floatView)
        unregisterReceiver(statusReceiver)
    }

    // ─── Floating button setup ────────────────────────────────────────────────

    private fun setupFloatView() {
        floatView = FrameLayout(this).apply {
            val size = (48 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        val indicator = ImageView(this).apply {
            setBackgroundColor(Color.parseColor("#CC1976D2"))
            // Simple circle indicator; replace with a proper drawable if desired
        }
        floatView.addView(indicator)

        val params = WindowManager.LayoutParams(
            (48 * resources.displayMetrics.density).toInt(),
            (48 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16; y = 200
        }

        // Drag to reposition
        var dX = 0f; var dY = 0f
        floatView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = params.x - event.rawX
                    dY = params.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX + dX).toInt()
                    params.y = (event.rawY + dY).toInt()
                    windowManager.updateViewLayout(floatView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Short tap = toggle
                    if (Math.abs(event.rawX - (params.x - dX)) < 10) {
                        sendBroadcast(Intent(GamepadAccessibilityService.ACTION_TOGGLE))
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatView, params)
    }

    private fun updateIndicator() {
        val color = if (isActive) Color.parseColor("#CC4CAF50") else Color.parseColor("#CC1976D2")
        (floatView.getChildAt(0) as? ImageView)?.setBackgroundColor(color)
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Gamepad Mapper Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gamepad Mapper")
            .setContentText("Overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
}
