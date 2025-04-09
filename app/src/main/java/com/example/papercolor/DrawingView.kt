package com.example.papercolor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
    }
    private var path = Path()
    private val paths = mutableListOf<Pair<Path, Paint>>()
    private val undonePaths = mutableListOf<Pair<Path, Paint>>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((p, pt) in paths) {
            canvas.drawPath(p, pt)
        }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> path.lineTo(x, y)
            MotionEvent.ACTION_UP -> {
                paths.add(Pair(Path(path), Paint(paint)))
                undonePaths.clear()
                path.reset()
            }
        }
        invalidate()
        return true
    }

    fun setColor(color: Int) {
        paint.color = color
    }

    fun setBrushStyle(style: String) {
        when (style) {
            "marker" -> paint.strokeWidth = 10f
            "feather" -> paint.strokeWidth = 5f
            "pencil" -> paint.strokeWidth = 2f
            "ink" -> paint.strokeWidth = 4f
        }
    }

    fun erase() {
        paint.color = Color.WHITE
        paint.strokeWidth = 20f
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            invalidate()
        }
    }

    fun redo() {
        if (undonePaths.isNotEmpty()) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            invalidate()
        }
    }

    fun clear() {
        paths.clear()
        undonePaths.clear()
        path.reset()
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
}