package com.brruham.gamepadmapper.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.brruham.gamepadmapper.model.ActionType
import com.brruham.gamepadmapper.model.ButtonMapping
import com.brruham.gamepadmapper.model.MappingProfile
import com.brruham.gamepadmapper.utils.GestureDispatcher
import com.brruham.gamepadmapper.utils.ProfileRepository

class GamepadAccessibilityService : AccessibilityService() {

    private var activeProfile: MappingProfile? = null
    private var screenW = 1080
    private var screenH = 2400
    private var isEnabled = false

    companion object {
        const val ACTION_RELOAD_PROFILE = "com.brruham.gamepadmapper.RELOAD_PROFILE"
        const val ACTION_TOGGLE         = "com.brruham.gamepadmapper.TOGGLE"
        var instance: GamepadAccessibilityService? = null
    }

    // ─── BroadcastReceiver for hot-reload ────────────────────────────────────

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_RELOAD_PROFILE -> loadActiveProfile()
                ACTION_TOGGLE -> {
                    isEnabled = !isEnabled
                    sendBroadcast(Intent("com.brruham.gamepadmapper.STATUS")
                        .putExtra("enabled", isEnabled))
                }
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Get screen dimensions
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(dm)
        screenW = dm.widthPixels
        screenH = dm.heightPixels

        loadActiveProfile()

        val filter = IntentFilter().apply {
            addAction(ACTION_RELOAD_PROFILE)
            addAction(ACTION_TOGGLE)
        }
        registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        unregisterReceiver(receiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // ─── Key events (buttons) ────────────────────────────────────────────────

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isEnabled) return false
        val profile = activeProfile ?: return false

        val mapping = profile.mappings.firstOrNull { m ->
            m.actionType != ActionType.JOYSTICK_SWIPE && m.buttonCode == event.keyCode
        } ?: return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                GestureDispatcher.onButtonDown(
                    this, mapping,
                    profile.screenshotWidth, profile.screenshotHeight,
                    screenW, screenH
                )
                return true
            }
            KeyEvent.ACTION_UP -> {
                GestureDispatcher.onButtonUp(mapping.buttonCode)
                return true
            }
        }
        return false
    }

    // ─── Motion events (joystick axes) ───────────────────────────────────────

    // Note: Motion events from gamepad come through a different path.
    // We expose a public method for FloatingOverlayService to forward them.
    fun handleMotionEvent(event: MotionEvent) {
        if (!isEnabled) return
        val profile = activeProfile ?: return

        profile.mappings
            .filter { it.actionType == ActionType.JOYSTICK_SWIPE }
            .forEach { mapping ->
                val axisX = event.getAxisValue(mapping.joystickAxisX)
                val axisY = event.getAxisValue(mapping.joystickAxisY)
                val deadzone = 0.15f
                val magnitude = Math.sqrt((axisX * axisX + axisY * axisY).toDouble()).toFloat()
                if (magnitude < deadzone) {
                    GestureDispatcher.onJoystickRelease(mapping.buttonCode)
                } else {
                    GestureDispatcher.onJoystickMove(
                        this, mapping, axisX, axisY,
                        profile.screenshotWidth, profile.screenshotHeight,
                        screenW, screenH
                    )
                }
            }
    }

    // ─── Profile loading ─────────────────────────────────────────────────────

    private fun loadActiveProfile() {
        activeProfile = ProfileRepository.getActiveProfile(this)
    }
}
