package com.brruham.gamepadmapper.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import com.brruham.gamepadmapper.model.ActionType
import com.brruham.gamepadmapper.model.ButtonMapping
import com.brruham.gamepadmapper.model.TouchPoint

object GestureDispatcher {

    private val handler = Handler(Looper.getMainLooper())

    // Track active hold gestures: buttonCode → cancelable runnable
    private val activeHolds = mutableMapOf<Int, Runnable>()
    // Track active multi-tap runnables
    private val activeMultiTaps = mutableMapOf<Int, Runnable>()
    // Track active joystick loops
    private val activeJoysticks = mutableMapOf<Int, Runnable>()

    // ─── Scale coords from profile resolution to actual screen ──────────────

    fun scalePoint(pt: TouchPoint, profileW: Int, profileH: Int, screenW: Int, screenH: Int) =
        TouchPoint(
            x = pt.x / profileW * screenW,
            y = pt.y / profileH * screenH
        )

    // ─── Button DOWN ─────────────────────────────────────────────────────────

    fun onButtonDown(
        service: AccessibilityService,
        mapping: ButtonMapping,
        profileW: Int, profileH: Int,
        screenW: Int, screenH: Int
    ) {
        val sw = screenW; val sh = screenH
        val pw = profileW; val ph = profileH

        when (mapping.actionType) {
            ActionType.TAP -> {
                val p = scalePoint(mapping.point, pw, ph, sw, sh)
                dispatchTap(service, p.x, p.y)
            }

            ActionType.HOLD -> {
                val p = scalePoint(mapping.point, pw, ph, sw, sh)
                startHold(service, mapping.buttonCode, p.x, p.y)
            }

            ActionType.SWIPE -> {
                val from = scalePoint(mapping.swipeFrom, pw, ph, sw, sh)
                val to   = scalePoint(mapping.swipeTo,   pw, ph, sw, sh)
                dispatchSwipe(service, from.x, from.y, to.x, to.y, mapping.swipeDurationMs)
            }

            ActionType.MULTI_TAP -> {
                val p = scalePoint(mapping.point, pw, ph, sw, sh)
                startMultiTap(service, mapping.buttonCode, p.x, p.y, mapping.multiTapIntervalMs)
            }

            ActionType.GESTURE_PATH -> {
                val scaled = mapping.gesturePath.map { scalePoint(it, pw, ph, sw, sh) }
                dispatchGesturePath(service, scaled, mapping.gestureStepDurationMs)
            }

            ActionType.JOYSTICK_SWIPE -> { /* handled via onJoystickMove */ }
        }
    }

    // ─── Button UP ───────────────────────────────────────────────────────────

    fun onButtonUp(buttonCode: Int) {
        stopHold(buttonCode)
        stopMultiTap(buttonCode)
    }

    // ─── Joystick move ───────────────────────────────────────────────────────

    fun onJoystickMove(
        service: AccessibilityService,
        mapping: ButtonMapping,
        axisX: Float, axisY: Float,
        profileW: Int, profileH: Int,
        screenW: Int, screenH: Int
    ) {
        val deadzone = 0.15f
        val magnitude = Math.sqrt((axisX * axisX + axisY * axisY).toDouble()).toFloat()

        if (magnitude < deadzone) {
            stopJoystick(mapping.buttonCode)
            return
        }

        // If already running at this axis, skip (loop handles it)
        if (activeJoysticks.containsKey(mapping.buttonCode)) return

        val cx = mapping.joystickCenterX / profileW * screenW
        val cy = mapping.joystickCenterY / profileH * screenH
        val r  = mapping.joystickRadius / profileW * screenW

        val loop = object : Runnable {
            override fun run() {
                val dx = axisX * r
                val dy = axisY * r
                val path = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + dx, cy + dy)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, mapping.joystickIntervalMs)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                service.dispatchGesture(gesture, null, null)
                handler.postDelayed(this, mapping.joystickIntervalMs)
            }
        }
        activeJoysticks[mapping.buttonCode] = loop
        handler.post(loop)
    }

    fun onJoystickRelease(buttonCode: Int) = stopJoystick(buttonCode)

    // ─── Internal helpers ────────────────────────────────────────────────────

    private fun dispatchTap(service: AccessibilityService, x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y); lineTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun startHold(service: AccessibilityService, code: Int, x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y); lineTo(x, y) }
        // Use a 10-second hold stroke; we cancel on button up by dispatching a lift
        val stroke = GestureDescription.StrokeDescription(path, 0, 10_000L)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        // Schedule a forced cancel after 10s anyway
        val cancel = Runnable { onButtonUp(code) }
        activeHolds[code] = cancel
        handler.postDelayed(cancel, 10_000L)
    }

    private fun stopHold(code: Int) {
        activeHolds.remove(code)?.let { handler.removeCallbacks(it) }
    }

    private fun stopMultiTap(code: Int) {
        activeMultiTaps.remove(code)?.let { handler.removeCallbacks(it) }
    }

    private fun stopJoystick(code: Int) {
        activeJoysticks.remove(code)?.let { handler.removeCallbacks(it) }
    }

    private fun startMultiTap(
        service: AccessibilityService, code: Int,
        x: Float, y: Float, intervalMs: Long
    ) {
        val loop = object : Runnable {
            override fun run() {
                dispatchTap(service, x, y)
                handler.postDelayed(this, intervalMs)
            }
        }
        activeMultiTaps[code] = loop
        handler.post(loop)
    }

    private fun dispatchSwipe(
        service: AccessibilityService,
        x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long
    ) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun dispatchGesturePath(
        service: AccessibilityService,
        points: List<TouchPoint>,
        stepMs: Long
    ) {
        if (points.size < 2) return
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        val duration = stepMs * (points.size - 1)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
