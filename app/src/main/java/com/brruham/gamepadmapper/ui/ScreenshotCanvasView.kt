package com.brruham.gamepadmapper.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.brruham.gamepadmapper.model.TouchPoint

/**
 * A View that displays a screenshot and lets the user tap / draw to pick coordinates.
 * Modes:
 *  - SINGLE_POINT  → one tap picks a point
 *  - TWO_POINTS    → first tap = from, second = to  (swipe)
 *  - PATH          → multiple taps build a path; call finishPath() to complete
 */
class ScreenshotCanvasView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    enum class PickMode { SINGLE_POINT, TWO_POINTS, PATH }

    var mode: PickMode = PickMode.SINGLE_POINT
        set(v) { field = v; points.clear(); invalidate() }

    // Raw bitmap coords (in bitmap space, not view space)
    val points = mutableListOf<TouchPoint>()

    var bitmap: Bitmap? = null
        set(v) { field = v; invalidate() }

    var onPointPicked: ((List<TouchPoint>) -> Unit)? = null

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
    }

    // Matrix to fit bitmap into view
    private val matrix = Matrix()
    private val invertMatrix = Matrix()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrix()
    }

    private fun updateMatrix() {
        val bmp = bitmap ?: return
        val sx = width.toFloat() / bmp.width
        val sy = height.toFloat() / bmp.height
        val scale = minOf(sx, sy)
        val dx = (width - bmp.width * scale) / 2f
        val dy = (height - bmp.height * scale) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        matrix.invert(invertMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, matrix, null) }

        // Draw points
        points.forEachIndexed { i, pt ->
            val vx = bitmapToViewX(pt.x)
            val vy = bitmapToViewY(pt.y)
            canvas.drawCircle(vx, vy, 18f, circlePaint)
            canvas.drawText("${i + 1}", vx - 8f, vy + 10f, textPaint)
        }

        // Draw connecting line for swipe/path
        if (points.size >= 2) {
            for (i in 0 until points.size - 1) {
                canvas.drawLine(
                    bitmapToViewX(points[i].x), bitmapToViewY(points[i].y),
                    bitmapToViewX(points[i + 1].x), bitmapToViewY(points[i + 1].y),
                    linePaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val mapped = FloatArray(2) { 0f }
        mapped[0] = event.x; mapped[1] = event.y
        invertMatrix.mapPoints(mapped)
        val bmpX = mapped[0].coerceIn(0f, (bitmap?.width?.toFloat() ?: 0f))
        val bmpY = mapped[1].coerceIn(0f, (bitmap?.height?.toFloat() ?: 0f))
        val pt = TouchPoint(bmpX, bmpY)

        when (mode) {
            PickMode.SINGLE_POINT -> {
                points.clear()
                points.add(pt)
                onPointPicked?.invoke(points.toList())
            }
            PickMode.TWO_POINTS -> {
                if (points.size < 2) {
                    points.add(pt)
                    if (points.size == 2) onPointPicked?.invoke(points.toList())
                }
            }
            PickMode.PATH -> {
                points.add(pt)
                onPointPicked?.invoke(points.toList())
            }
        }
        invalidate()
        return true
    }

    fun finishPath() {
        if (mode == PickMode.PATH) onPointPicked?.invoke(points.toList())
    }

    fun clearPoints() {
        points.clear()
        invalidate()
    }

    private fun bitmapToViewX(x: Float): Float {
        val pts = floatArrayOf(x, 0f)
        matrix.mapPoints(pts)
        return pts[0]
    }

    private fun bitmapToViewY(y: Float): Float {
        val pts = floatArrayOf(0f, y)
        matrix.mapPoints(pts)
        return pts[1]
    }
}
