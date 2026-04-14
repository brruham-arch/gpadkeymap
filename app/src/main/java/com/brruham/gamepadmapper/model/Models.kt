package com.brruham.gamepadmapper.model

import android.view.KeyEvent

// ─── Action Types ───────────────────────────────────────────────────────────

enum class ActionType {
    TAP,           // single tap at point
    HOLD,          // hold while button pressed, release when button released
    SWIPE,         // swipe from A to B
    MULTI_TAP,     // repeated tap at interval
    GESTURE_PATH,  // multi-point free path
    JOYSTICK_SWIPE // analog stick → directional swipe
}

// ─── A single touch point ───────────────────────────────────────────────────

data class TouchPoint(
    val x: Float = 0f,
    val y: Float = 0f
)

// ─── One button → action mapping ────────────────────────────────────────────

data class ButtonMapping(
    val id: String = java.util.UUID.randomUUID().toString(),
    val buttonCode: Int = KeyEvent.KEYCODE_BUTTON_A,    // KeyEvent keycode
    val buttonLabel: String = "A",                       // display label
    val actionType: ActionType = ActionType.TAP,

    // TAP / HOLD / MULTI_TAP → single point
    val point: TouchPoint = TouchPoint(),

    // SWIPE → two points + duration
    val swipeFrom: TouchPoint = TouchPoint(),
    val swipeTo: TouchPoint = TouchPoint(),
    val swipeDurationMs: Long = 200,

    // MULTI_TAP → interval
    val multiTapIntervalMs: Long = 100,

    // GESTURE_PATH → ordered list of points
    val gesturePath: List<TouchPoint> = emptyList(),
    val gestureStepDurationMs: Long = 50,

    // JOYSTICK_SWIPE → axis codes + center + radius
    val joystickAxisX: Int = -1,   // MotionEvent.AXIS_X etc
    val joystickAxisY: Int = -1,
    val joystickCenterX: Float = 0f,
    val joystickCenterY: Float = 0f,
    val joystickRadius: Float = 100f,
    val joystickIntervalMs: Long = 50
)

// ─── Profile: collection of mappings ────────────────────────────────────────

data class MappingProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "New Profile",
    val screenshotPath: String = "",       // absolute path to reference screenshot
    val screenshotWidth: Int = 1080,
    val screenshotHeight: Int = 2400,
    val mappings: MutableList<ButtonMapping> = mutableListOf()
)
