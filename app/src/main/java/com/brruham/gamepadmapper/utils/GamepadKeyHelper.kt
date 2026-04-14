package com.brruham.gamepadmapper.utils

import android.view.KeyEvent
import android.view.MotionEvent

object GamepadKeyHelper {

    val BUTTON_MAP: List<Pair<Int, String>> = listOf(
        KeyEvent.KEYCODE_BUTTON_A        to "A",
        KeyEvent.KEYCODE_BUTTON_B        to "B",
        KeyEvent.KEYCODE_BUTTON_X        to "X",
        KeyEvent.KEYCODE_BUTTON_Y        to "Y",
        KeyEvent.KEYCODE_BUTTON_L1       to "L1",
        KeyEvent.KEYCODE_BUTTON_R1       to "R1",
        KeyEvent.KEYCODE_BUTTON_L2       to "L2",
        KeyEvent.KEYCODE_BUTTON_R2       to "R2",
        KeyEvent.KEYCODE_BUTTON_THUMBL   to "L3",
        KeyEvent.KEYCODE_BUTTON_THUMBR   to "R3",
        KeyEvent.KEYCODE_BUTTON_START    to "Start",
        KeyEvent.KEYCODE_BUTTON_SELECT   to "Select",
        KeyEvent.KEYCODE_DPAD_UP         to "D-Up",
        KeyEvent.KEYCODE_DPAD_DOWN       to "D-Down",
        KeyEvent.KEYCODE_DPAD_LEFT       to "D-Left",
        KeyEvent.KEYCODE_DPAD_RIGHT      to "D-Right"
    )

    val AXIS_MAP: List<Triple<Int, Int, String>> = listOf(
        Triple(MotionEvent.AXIS_X,  MotionEvent.AXIS_Y,  "Left Stick"),
        Triple(MotionEvent.AXIS_Z,  MotionEvent.AXIS_RZ, "Right Stick")
    )

    fun labelForKeycode(code: Int): String =
        BUTTON_MAP.firstOrNull { it.first == code }?.second ?: "Key($code)"

    fun isGamepadKey(event: KeyEvent): Boolean =
        BUTTON_MAP.any { it.first == event.keyCode }
}
