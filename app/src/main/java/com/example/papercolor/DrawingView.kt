package com.example.papercolor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // Vẽ
    private var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
    }
    // Đường
    private var path = Path()
    private val paths = mutableListOf<Pair<Path, Paint>>()
    private val undonePaths = mutableListOf<Pair<Path, Paint>>()

    // Vẽ pixel
    private var isPixelMode = false
    private var pixelSize = 10f // kích thước
    private var canvasWidthInPixels = 50 // ngang
    private var canvasHeightInPixels = 50 // pixel dọc
    private val pixelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    // Biến để chèn ảnh
    private var backgroundBitmap: Bitmap? = null
    private var backgroundRect: RectF? = null

    // List text
    private val texts = mutableListOf<TextItem>()
    private val undoneTexts = mutableListOf<TextItem>()

    // Biến điều khiển chế độ text
    private var currentText: String? = null
    private var isTextMode = false

    private var currentFontFamily: String = "sans-serif"
    private var currentTextSize: Float = 24f
    private var currentTextColor: Int = Color.BLACK

    // Zoom và pan
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f
    private val scaleDetector: ScaleGestureDetector

    // Shape
    enum class GeometryTool {
        NONE, LINE, RECTANGLE, CIRCLE, TRIANGLE
    }

    private var geometryMode = GeometryTool.NONE // Chế độ vẽ hình học
    private var startX = 0f // Tọa độ bắt đầu của hình
    private var startY = 0f
    private var endX = 0f   // Tọa độ kết thúc của hình
    private var endY = 0f
    private var tempPath = Path() // Path tạm thời để hiển thị hình khi kéo
    private var isDrawingGeometry = false // Biến kiểm soát trạng thái vẽ hình học

    private val backgroundStack = mutableListOf<Bitmap?>()
    private var currentBackgroundIndex = -1

    // Hàm mới để cập nhật ảnh nền từ stack
    private fun updateCurrentBackground() {
        backgroundBitmap = backgroundStack.getOrNull(currentBackgroundIndex)?.let {
            it.copy(it.config ?: Bitmap.Config.ARGB_8888, true)
        }
        calculateBackgroundRect()
        invalidate()
    }

    private fun calculateBackgroundRect() {
        backgroundBitmap?.let { bitmap ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
            val scaledWidth = imageWidth * scale
            val scaledHeight = imageHeight * scale

            val left = (viewWidth - scaledWidth) / 2
            val top = (viewHeight - scaledHeight) / 2

            backgroundRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        }
    }

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.1f, 5.0f)
            invalidate()
            return true
        }
    }

    // Hàm để tạo lưới pixel
    private var showPixelGrid = false
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        alpha = 100 // Độ trong suốt
    }

    private fun drawPixelGrid(canvas: Canvas) {
        val viewWidth = width.toFloat() / scaleFactor
        val viewHeight = height.toFloat() / scaleFactor

        var x = 0f
        while (x <= viewWidth) {
            canvas.drawLine(x, 0f, x, viewHeight, gridPaint)
            x += pixelSize
        }

        var y = 0f
        while (y <= viewHeight) {
            canvas.drawLine(0f, y, viewWidth, y, gridPaint)
            y += pixelSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor)

        backgroundBitmap?.let { bitmap ->
            backgroundRect?.let { rect ->
                canvas.drawBitmap(bitmap, null, rect, null)
            }
        }

        for ((p, pt) in paths) {
            canvas.drawPath(p, pt)
        }

        canvas.drawPath(path, paint)

        // Vẽ hình học tạm thời
        if (geometryMode != GeometryTool.NONE && isDrawingGeometry) {
            tempPath.reset()
            when (geometryMode) {
                GeometryTool.LINE -> {
                    tempPath.moveTo(startX, startY)
                    tempPath.lineTo(endX, endY)
                }
                GeometryTool.RECTANGLE -> {
                    tempPath.addRect(
                        min(startX, endX), min(startY, endY),
                        max(startX, endX), max(startY, endY),
                        Path.Direction.CW
                    )
                }
                GeometryTool.CIRCLE -> {
                    val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                    tempPath.addCircle(startX, startY, radius, Path.Direction.CW)
                }
                GeometryTool.TRIANGLE -> {
                    val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                    val centerX = startX
                    val centerY = startY
                    tempPath.moveTo(centerX, centerY - radius) // Đỉnh trên
                    tempPath.lineTo(centerX - radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat()) // Đỉnh trái dưới
                    tempPath.lineTo(centerX + radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat()) // Đỉnh phải dưới
                    tempPath.close()
                }
                GeometryTool.NONE -> {}
            }
            canvas.drawPath(tempPath, paint)
        }

        for (textItem in texts) {
            canvas.drawText(textItem.text, textItem.x, textItem.y, textItem.paint)
        }

        if (isPixelMode && showPixelGrid) {
            drawPixelGrid(canvas)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        // Xử lý riêng cho chế độ pixel
        if (isPixelMode) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (scaleDetector.isInProgress) return true

                    val invertedMatrix = Matrix()
                    val transformMatrix = Matrix().apply {
                        postTranslate(posX, posY)
                        postScale(scaleFactor, scaleFactor)
                        invert(invertedMatrix)
                    }

                    val pts = floatArrayOf(event.x, event.y)
                    invertedMatrix.mapPoints(pts)

                    val pixelX = (pts[0] / pixelSize).toInt() * pixelSize
                    val pixelY = (pts[1] / pixelSize).toInt() * pixelSize

                    val pixelPath = Path().apply {
                        addRect(pixelX, pixelY, pixelX + pixelSize, pixelY + pixelSize, Path.Direction.CW)
                    }

                    paths.add(Pair(pixelPath, Paint(paint).apply {
                        style = Paint.Style.FILL
                        isAntiAlias = false
                    }))

                    invalidate()
                    return true
                }
            }
        }

        // Xử lý chế độ hình học
        if (geometryMode != GeometryTool.NONE) {
            val invertedMatrix = Matrix()
            val transformMatrix = Matrix().apply {
                postTranslate(posX, posY)
                postScale(scaleFactor, scaleFactor)
                invert(invertedMatrix)
            }
            val pts = floatArrayOf(event.x, event.y)
            invertedMatrix.mapPoints(pts)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    startX = pts[0]
                    startY = pts[1]
                    endX = pts[0]
                    endY = pts[1]
                    isDrawingGeometry = true
                    tempPath.reset()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDrawingGeometry) {
                        endX = pts[0]
                        endY = pts[1]
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDrawingGeometry) {
                        endX = pts[0]
                        endY = pts[1]
                        val finalPath = Path()
                        when (geometryMode) {
                            GeometryTool.LINE -> {
                                finalPath.moveTo(startX, startY)
                                finalPath.lineTo(endX, endY)
                            }
                            GeometryTool.RECTANGLE -> {
                                finalPath.addRect(
                                    min(startX, endX), min(startY, endY),
                                    max(startX, endX), max(startY, endY),
                                    Path.Direction.CW
                                )
                            }
                            GeometryTool.CIRCLE -> {
                                val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                                finalPath.addCircle(startX, startY, radius, Path.Direction.CW)
                            }
                            GeometryTool.TRIANGLE -> {
                                val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                                val centerX = startX
                                val centerY = startY
                                finalPath.moveTo(centerX, centerY - radius) // Đỉnh trên
                                finalPath.lineTo(centerX - radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat()) // Đỉnh trái dưới
                                finalPath.lineTo(centerX + radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat()) // Đỉnh phải dưới
                                finalPath.close()
                            }
                            GeometryTool.NONE -> {}
                        }
                        if (!finalPath.isEmpty) {
                            paths.add(Pair(finalPath, Paint(paint)))
                            undonePaths.clear()
                        }
                        tempPath.reset()
                        isDrawingGeometry = false
                        invalidate()
                    }
                    return true
                }
            }
            return true
        }

        // Xử lý chế độ text
        if (isTextMode && currentText != null) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    val invertedMatrix = Matrix()
                    val transformMatrix = Matrix().apply {
                        postTranslate(posX, posY)
                        postScale(scaleFactor, scaleFactor)
                        invert(invertedMatrix)
                    }

                    val pts = floatArrayOf(event.x, event.y)
                    invertedMatrix.mapPoints(pts)

                    val textPaint = Paint(paint).apply {
                        style = Paint.Style.FILL
                        textSize = currentTextSize
                        typeface = Typeface.create(currentFontFamily, Typeface.NORMAL)
                        color = currentTextColor
                    }
                    texts.add(TextItem(currentText!!, pts[0], pts[1], textPaint))
                    undoneTexts.clear()
                    invalidate()
                    return true
                }
            }
            return true
        }

        // Xử lý chế độ vẽ thông thường
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (scaleDetector.isInProgress) return true

                lastTouchX = event.x
                lastTouchY = event.y

                val invertedMatrix = Matrix()
                val transformMatrix = Matrix().apply {
                    postTranslate(posX, posY)
                    postScale(scaleFactor, scaleFactor)
                    invert(invertedMatrix)
                }

                val pts = floatArrayOf(event.x, event.y)
                invertedMatrix.mapPoints(pts)

                path.moveTo(pts[0], pts[1])
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) {
                    val focusX = scaleDetector.focusX
                    val focusY = scaleDetector.focusY

                    val dx = focusX - lastTouchX
                    val dy = focusY - lastTouchY

                    posX += dx
                    posY += dy

                    lastTouchX = focusX
                    lastTouchY = focusY

                    invalidate()
                } else {
                    val invertedMatrix = Matrix()
                    val transformMatrix = Matrix().apply {
                        postTranslate(posX, posY)
                        postScale(scaleFactor, scaleFactor)
                        invert(invertedMatrix)
                    }

                    val pts = floatArrayOf(event.x, event.y)
                    invertedMatrix.mapPoints(pts)

                    path.lineTo(pts[0], pts[1])
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!scaleDetector.isInProgress) {
                    paths.add(Pair(Path(path), Paint(paint)))
                    undonePaths.clear()
                    path.reset()
                    invalidate()
                }
                return true
            }
        }

        return true
    }

    // Reset zoom và pan về mặc định
    fun resetZoom() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        invalidate()
    }

    // Set màu cho brush
    fun setColor(color: Int) {
        paint.color = color
        pixelPaint.color = color
    }

    // Set kiểu bút
    fun setBrushStyle(style: String) {
        when (style) {
            "marker" -> {
                paint.strokeWidth = 10f
                paint.style = Paint.Style.STROKE
            }
            "feather" -> {
                paint.strokeWidth = 5f
                paint.style = Paint.Style.STROKE
            }
            "pencil" -> {
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
            }
            "ink" -> {
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE
            }
        }
    }

    // Bút xóa
    fun erase() {
        paint.color = Color.WHITE
        paint.strokeWidth = 20f
    }

    fun undo(): Boolean {
        return if (paths.isNotEmpty()) {
            undonePaths.add(paths.removeAt(paths.size - 1))
            invalidate()
            true
        } else if (texts.isNotEmpty()) {
            undoneTexts.add(texts.removeAt(texts.size - 1))
            invalidate()
            true
        } else {
            false
        }
    }

    fun redo(): Boolean {
        return if (undonePaths.isNotEmpty()) {
            paths.add(undonePaths.removeAt(undonePaths.size - 1))
            invalidate()
            true
        } else if (undoneTexts.isNotEmpty()) {
            texts.add(undoneTexts.removeAt(undoneTexts.size - 1))
            invalidate()
            true
        } else {
            false
        }
    }

    fun undoBackground(): Boolean {
        if (currentBackgroundIndex > 0) {
            currentBackgroundIndex--
            updateCurrentBackground()
            return true
        }
        return false
    }

    fun redoBackground(): Boolean {
        if (currentBackgroundIndex < backgroundStack.size - 1) {
            currentBackgroundIndex++
            updateCurrentBackground()
            return true
        }
        return false
    }

    fun clear() {
        paths.clear()
        undonePaths.clear()
        path.reset()
        texts.clear()
        undoneTexts.clear()
        backgroundBitmap = null
        backgroundStack.clear()
        currentBackgroundIndex = -1
        isPixelMode = false
        isTextMode = false
        currentText = null
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.BLACK
        }
        resetZoom()
        invalidate()
    }

    private var backgroundColor = Color.WHITE

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(backgroundColor)

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor)

        backgroundBitmap?.let { bitmap ->
            backgroundRect?.let { rect ->
                canvas.drawBitmap(bitmap, null, rect, null)
            }
        }

        for ((p, pt) in paths) {
            canvas.drawPath(p, pt)
        }

        canvas.drawPath(path, paint)

        for (textItem in texts) {
            canvas.drawText(textItem.text, textItem.x, textItem.y, textItem.paint)
        }

        canvas.restore()
        return bitmap
    }

    fun setCanvasBackgroundColor(color: Int) {
        backgroundColor = color
        invalidate()
    }

    fun getCurrentBackgroundColor(): Int {
        return backgroundColor
    }

    fun enableTextMode(
        text: String,
        fontFamily: String = "sans-serif",
        textSize: Float = 24f,
        textColor: Int = Color.BLACK
    ) {
        isTextMode = true
        currentText = text
        currentFontFamily = fontFamily
        currentTextSize = textSize
        currentTextColor = textColor
        isPixelMode = false
        geometryMode = GeometryTool.NONE
        invalidate()
    }

    fun disableTextMode() {
        isTextMode = false
        currentText = null
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        if (currentBackgroundIndex < backgroundStack.size - 1) {
            backgroundStack.subList(currentBackgroundIndex + 1, backgroundStack.size).clear()
        }

        backgroundStack.add(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true))
        currentBackgroundIndex = backgroundStack.size - 1
        updateCurrentBackground()
    }

    data class TextItem(val text: String, val x: Float, val y: Float, val paint: Paint)

    fun enablePixelMode(pixelSize: Int, widthInPixels: Int, heightInPixels: Int, showGrid: Boolean = true) {
        isPixelMode = true
        this.pixelSize = pixelSize.toFloat()
        this.canvasWidthInPixels = widthInPixels
        this.canvasHeightInPixels = heightInPixels
        this.showPixelGrid = showGrid
        isTextMode = false
        geometryMode = GeometryTool.NONE
        paint.apply {
            strokeWidth = 1f
            isAntiAlias = false
            style = Paint.Style.FILL
        }
        invalidate()
    }

    fun togglePixelGrid(show: Boolean) {
        showPixelGrid = show
        invalidate()
    }

    fun disablePixelMode() {
        isPixelMode = false
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        invalidate()
    }

    fun setGeometryTool(tool: GeometryTool) {
        geometryMode = tool
        isPixelMode = false
        isTextMode = false
        path.reset()
        tempPath.reset()
        isDrawingGeometry = false
        invalidate()
    }

    fun disableGeometryMode() {
        geometryMode = GeometryTool.NONE
        tempPath.reset()
        isDrawingGeometry = false
        invalidate()
    }

    private fun drawPixel(x: Float, y: Float) {
        val pixelX = (x / pixelSize).toInt() * pixelSize
        val pixelY = (y / pixelSize).toInt() * pixelSize

        val pixelPath = Path().apply {
            addRect(pixelX, pixelY, pixelX + pixelSize, pixelY + pixelSize, Path.Direction.CW)
        }

        paths.add(Pair(pixelPath, Paint(paint)))
        invalidate()
    }
}