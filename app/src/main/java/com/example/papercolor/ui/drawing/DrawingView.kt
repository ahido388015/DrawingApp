package com.example.papercolor.ui.drawing

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.graphics.Typeface
import android.view.ScaleGestureDetector
import kotlin.math.*

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // Vẽ mặc định
    var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.BLACK
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var path = Path()
    private val paths = mutableListOf<Pair<Path, Paint>>()
    private val undonePaths = mutableListOf<Pair<Path, Paint>>()

    // Lưu trạng thái paint trước khi xóa
    private var savedPaintState: Paint? = null

    // Đường
    var isPixelMode = false
    var pixelSize = 10f
    var canvasWidthInPixels = 50
    var canvasHeightInPixels = 50
    private val pixelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        color = Color.BLACK //mặc định cho pixel mode
    }

    // List text
    private val texts = mutableListOf<TextItem>()
    private val undoneTexts = mutableListOf<TextItem>()
    var currentText: String? = null
    var isTextMode = false
    var currentFontFamily: String = "sans-serif"
    var currentTextSize: Float = 24f
    var currentTextColor: Int = Color.BLACK
    private var previewTextItem: TextItem? = null

    // Zoom và pan
    var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    var posX = 0f
    var posY = 0f
    private val scaleDetector: ScaleGestureDetector

    // Shape
    enum class GeometryTool {
        NONE, LINE, RECTANGLE, CIRCLE, TRIANGLE
    }

    var geometryMode = GeometryTool.NONE
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var tempPath = Path()
    private var isDrawingGeometry = false

    // List chứa tất cả ảnh
    private val imageItems = mutableListOf<ImageItem>()
    private var selectedImageIndex = -1
    private var currentResizeHandle = ResizeHandle.NONE
    private var lastTouchTime: Long = 0
    private var isResizingImage = false
    private var isMovingImage = false
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    // trạng thái bút xóa
    var isEraseMode = false

    // trạng thái điều chỉnh ảnh
    var isAdjustingImageMode = false

    // Enum để xác định handle resize
    private enum class ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE
    }

    // lưu thông tin ảnh
    private data class ImageItem(
        val bitmap: Bitmap,
        var rect: RectF,
        var isSelected: Boolean = false,
        var originalWidth: Int = bitmap.width,
        var originalHeight: Int = bitmap.height,
        var isAdjustable: Boolean = true
    )
    //líst ảnh bị undo để có redo
    private val undoneImages = mutableListOf<ImageItem>()

    // Template canvas
    private var currentTemplate: String = "white"

    // Paint cho cục tẩy
    private val erasePaint = Paint().apply {
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        shader = null
        maskFilter = null
        pathEffect = null
    }

    // Paint cho pixel mode
    private val pixelDefaultPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        strokeWidth = 1f
        color = Color.BLACK
    }

    // Paint cho template
    private val templatePaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    // Enum để xác định loại hành động
    private enum class ActionType {
        PATH, TEXT, IMAGE
    }
    // lưu thông tin hành động
    private data class Action(
        val type: ActionType,
        val path: Pair<Path, Paint>? = null,
        val text: TextItem? = null,
        val image: ImageItem? = null
    )
    // lưu lịch sử hành động
    private val actions = mutableListOf<Action>()
    private val undoneActions = mutableListOf<Action>()

    private fun drawBackgroundTemplate(canvas: Canvas) {
        val viewWidth = width.toFloat() / scaleFactor
        val viewHeight = height.toFloat() / scaleFactor

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor)

        when (currentTemplate) {
            "white" -> {
                //blank
            }
            "lined" -> {
                val lineSpacing = 50f
                val maxLines = (viewHeight / lineSpacing).toInt() + 1
                for (y in 0..maxLines) {
                    canvas.drawLine(0f, y * lineSpacing, viewWidth, y * lineSpacing, templatePaint)
                }
            }
            "grid" -> {
                val gridSize = 50f
                val maxCols = (viewWidth / gridSize).toInt() + 1
                val maxRows = (viewHeight / gridSize).toInt() + 1
                for (x in 0..maxCols) {
                    canvas.drawLine(x * gridSize, 0f, x * gridSize, viewHeight, templatePaint)
                }
                for (y in 0..maxRows) {
                    canvas.drawLine(0f, y * gridSize, viewWidth, y * gridSize, templatePaint)
                }
            }
            "dotted" -> {
                val dotSpacing = 50f
                val maxCols = (viewWidth / dotSpacing).toInt() + 1
                val maxRows = (viewHeight / dotSpacing).toInt() + 1
                for (x in 0..maxCols) {
                    for (y in 0..maxRows) {
                        canvas.drawCircle(x * dotSpacing, y * dotSpacing, 2f / scaleFactor, templatePaint)
                    }
                }
            }
        }

        canvas.restore()
    }

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            Log.d("DrawingView", "Scale begin: scaleFactor=$scaleFactor")
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val previousScale = scaleFactor
            val newScaleFactor = detector.scaleFactor
            Log.d("DrawingView", "Scale: detector.scaleFactor=$newScaleFactor, previousScale=$previousScale")
            scaleFactor *= newScaleFactor
            scaleFactor = scaleFactor.coerceIn(0.1f, 5.0f)
            Log.d("DrawingView", "After coerce: scaleFactor=$scaleFactor")

            val focusX = detector.focusX
            val focusY = detector.focusY
            posX = focusX - (focusX - posX) * (scaleFactor / previousScale)
            posY = focusY - (focusY - posY) * (scaleFactor / previousScale)
            Log.d("DrawingView", "Position: posX=$posX, posY=$posY")

            constrainPan()
            invalidate()
            return true
        }
    }

    private fun constrainPan() {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scaledWidth = viewWidth * scaleFactor
        val scaledHeight = viewHeight * scaleFactor

        //không di chuyển ra ngoài tầm nhìn
        val minX = if (scaledWidth < viewWidth) (viewWidth - scaledWidth) else viewWidth - scaledWidth
        val maxX = if (scaledWidth < viewWidth) 0f else 0f
        val minY = if (scaledHeight < viewHeight) (viewHeight - scaledHeight) else viewHeight - scaledHeight
        val maxY = if (scaledHeight < viewHeight) 0f else 0f

        posX = posX.coerceIn(minX, maxX)
        posY = posY.coerceIn(minY, maxY)
        Log.d("DrawingView", "ConstrainPan: minX=$minX, maxX=$maxX, minY=$minY, maxY=$maxY, posX=$posX, posY=$posY")
    }

    var showPixelGrid = false
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        alpha = 100
        isAntiAlias = true
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

    private fun getResizeHandleForPoint(x: Float, y: Float, rect: RectF): ResizeHandle {
        val handleSize = 30f / scaleFactor

        if (abs(x - rect.left) < handleSize && abs(y - rect.top) < handleSize) return ResizeHandle.TOP_LEFT
        if (abs(x - rect.right) < handleSize && abs(y - rect.top) < handleSize) return ResizeHandle.TOP_RIGHT
        if (abs(x - rect.left) < handleSize && abs(y - rect.bottom) < handleSize) return ResizeHandle.BOTTOM_LEFT
        if (abs(x - rect.right) < handleSize && abs(y - rect.bottom) < handleSize) return ResizeHandle.BOTTOM_RIGHT
        if (rect.contains(x, y)) return ResizeHandle.MOVE
        return ResizeHandle.NONE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        try {
            drawBackgroundTemplate(canvas)

            canvas.save()
            canvas.translate(posX, posY)
            canvas.scale(scaleFactor, scaleFactor)

            // Vẽ các phần tử theo thứ tự thời gian trong danh sách actions
            for (action in actions) {
                when (action.type) {
                    ActionType.PATH -> {
                        action.path?.let { (path, pathPaint) ->
                            canvas.drawPath(path, pathPaint)
                        }
                    }
                    ActionType.TEXT -> {
                        action.text?.let { textItem ->
                            canvas.drawText(textItem.text, textItem.x, textItem.y, textItem.paint)
                        }
                    }
                    ActionType.IMAGE -> {
                        action.image?.let { imageItem ->
                            val srcRect = Rect(0, 0, imageItem.originalWidth, imageItem.originalHeight)
                            canvas.drawBitmap(imageItem.bitmap, srcRect, imageItem.rect, null)
                        }
                    }
                }
            }

            // Vẽ các phần tử tạm thời (đang được vẽ hoặc điều chỉnh)
            if (!path.isEmpty) {
                canvas.drawPath(path, if (isEraseMode) erasePaint else paint)
            }

            if (geometryMode != GeometryTool.NONE && isDrawingGeometry) {
                canvas.drawPath(tempPath, paint)
            }

            previewTextItem?.let {
                canvas.drawText(it.text, it.x, it.y, it.paint)
            }

            // Vẽ viền và handle cho ảnh đang được chọn
            if (isAdjustingImageMode && selectedImageIndex != -1) {
                val imageItem = imageItems.getOrNull(selectedImageIndex)
                if (imageItem != null && imageItem.isSelected) {
                    val borderPaint = Paint().apply {
                        color = Color.BLUE
                        style = Paint.Style.STROKE
                        strokeWidth = 2f / scaleFactor // Điều chỉnh theo scaleFactor
                    }
                    canvas.drawRect(imageItem.rect, borderPaint)

                    val handlePaint = Paint().apply {
                        color = Color.BLUE
                        style = Paint.Style.FILL
                    }
                    val handleSize = 15f / scaleFactor
                    canvas.drawCircle(imageItem.rect.left, imageItem.rect.top, handleSize, handlePaint)
                    canvas.drawCircle(imageItem.rect.right, imageItem.rect.top, handleSize, handlePaint)
                    canvas.drawCircle(imageItem.rect.left, imageItem.rect.bottom, handleSize, handlePaint)
                    canvas.drawCircle(imageItem.rect.right, imageItem.rect.bottom, handleSize, handlePaint)
                }
            }

            // Vẽ lưới pixel nếu ở pixel mode
            if (isPixelMode && showPixelGrid) {
                drawPixelGrid(canvas)
            }

            canvas.restore()
        } finally {
            canvas.restoreToCount(layer)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Xử lý scale gesture
        scaleDetector.onTouchEvent(event)

        // Chuyển tọa độ từ screen sang canvas
        val invertedMatrix = Matrix()
        val transformMatrix = Matrix().apply {
            postScale(scaleFactor, scaleFactor)
            postTranslate(posX, posY)
            invert(invertedMatrix)
        }
        val pts = floatArrayOf(event.x, event.y)
        invertedMatrix.mapPoints(pts)
        val x = pts[0] // x trên canvas
        val y = pts[1] // y trên canvas

        // double click để reset zoom
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTouchTime < 300 && scaleFactor > 1.0f) {
                resetZoom()
                lastTouchTime = 0
                invalidate()
                return true
            }
            lastTouchTime = currentTime
        }

        // Xử lý text mode
        if (isTextMode && currentText != null) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val textPaint = Paint(paint).apply {
                        style = Paint.Style.FILL
                        textSize = currentTextSize // Không nhân với scaleFactor
                        typeface = Typeface.create(currentFontFamily, Typeface.NORMAL)
                        color = currentTextColor
                        isAntiAlias = true
                    }
                    previewTextItem = TextItem(currentText!!, x, y, textPaint)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val textPaint = Paint(paint).apply {
                        style = Paint.Style.FILL
                        textSize = currentTextSize
                        typeface = Typeface.create(currentFontFamily, Typeface.NORMAL)
                        color = currentTextColor
                        isAntiAlias = true
                    }
                    val textItem = TextItem(currentText!!, x, y, textPaint)
                    texts.add(textItem)
                    actions.add(Action(ActionType.TEXT, text = textItem))
                    undoneTexts.clear()
                    undoneActions.clear() // Xóa redo
                    previewTextItem = null
                    invalidate()
                    return true
                }
            }
        } else if (isTextMode) {
            isTextMode = false
            previewTextItem = null
            invalidate()
        }

        // Xử lý pixel mode
        if (isPixelMode) {

            if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val adjustedPixelSize = pixelSize
                        val pixelX = ((x / adjustedPixelSize).toInt() * adjustedPixelSize)
                        val pixelY = ((y / adjustedPixelSize).toInt() * adjustedPixelSize)

                        val pixelPath = Path().apply {
                            addRect(pixelX, pixelY, pixelX + adjustedPixelSize, pixelY + adjustedPixelSize, Path.Direction.CW)
                        }

                        val currentPaint = if (isEraseMode) {
                            Paint().apply {
                                style = Paint.Style.FILL
                                isAntiAlias = false
                                color = Color.WHITE // Màu trắng cho tẩy trong pixel mode
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
                            }
                        } else {
                            Paint(pixelDefaultPaint).apply {
                                strokeWidth = adjustedPixelSize
                            }
                        }

                        paths.add(Pair(pixelPath, currentPaint))
                        actions.add(Action(ActionType.PATH, path = Pair(pixelPath, currentPaint))) // Ghi lại hành động
                        undonePaths.clear()
                        undoneActions.clear() // Xóa redo
                        invalidate()
                        return true
                    }
                }
            }
            return true
        }

        // Xử lý geometry mode
        if (geometryMode != GeometryTool.NONE) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    startX = x
                    startY = y
                    endX = x
                    endY = y
                    isDrawingGeometry = true
                    updateGeometryPath(startX, startY, endX, endY)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDrawingGeometry) {
                        endX = x
                        endY = y
                        updateGeometryPath(startX, startY, endX, endY)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDrawingGeometry) {
                        endX = x
                        endY = y
                        val finalPath = Path()
                        updateGeometryPath(startX, startY, endX, endY, finalPath)

                        if (!finalPath.isEmpty) {
                            val newPath = Pair(finalPath, Paint(paint))
                            paths.add(newPath)
                            actions.add(Action(ActionType.PATH, path = newPath)) // Ghi lại hành động
                            undonePaths.clear()
                            undoneActions.clear() // Xóa redo khi thêm hành động mới
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

        // Xử lý image adjustment mode và vẽ thông thường
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (isAdjustingImageMode) {
                    for ((index, imageItem) in imageItems.withIndex()) {
                        if (imageItem.rect.contains(x, y) && imageItem.isAdjustable) {
                            imageItems.forEach { it.isSelected = false }
                            imageItem.isSelected = true
                            selectedImageIndex = index

                            currentResizeHandle = getResizeHandleForPoint(x, y, imageItem.rect)
                            isResizingImage = currentResizeHandle != ResizeHandle.NONE
                            isMovingImage = currentResizeHandle == ResizeHandle.MOVE

                            if (isMovingImage) {
                                touchOffsetX = x - imageItem.rect.left
                                touchOffsetY = y - imageItem.rect.top
                            }

                            invalidate()
                            return true
                        }
                    }

                    if (selectedImageIndex != -1) {
                        imageItems[selectedImageIndex].isSelected = false
                        selectedImageIndex = -1
                        isAdjustingImageMode = false
                        invalidate()
                    }
                    return true
                }

                // Vẽ thông thường
                path.reset()
                path.moveTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isAdjustingImageMode && selectedImageIndex != -1 && imageItems[selectedImageIndex].isAdjustable) {
                    val imageItem = imageItems[selectedImageIndex]

                    if (isResizingImage) {
                        when (currentResizeHandle) {
                            ResizeHandle.TOP_LEFT -> {
                                imageItem.rect.left = x
                                imageItem.rect.top = y
                            }
                            ResizeHandle.TOP_RIGHT -> {
                                imageItem.rect.right = x
                                imageItem.rect.top = y
                            }
                            ResizeHandle.BOTTOM_LEFT -> {
                                imageItem.rect.left = x
                                imageItem.rect.bottom = y
                            }
                            ResizeHandle.BOTTOM_RIGHT -> {
                                imageItem.rect.right = x
                                imageItem.rect.bottom = y
                            }
                            ResizeHandle.MOVE -> {
                                val newLeft = x - touchOffsetX
                                val newTop = y - touchOffsetY
                                val width = imageItem.rect.width()
                                val height = imageItem.rect.height()

                                imageItem.rect.left = newLeft
                                imageItem.rect.top = newTop
                                imageItem.rect.right = newLeft + width
                                imageItem.rect.bottom = newTop + height
                            }
                            ResizeHandle.NONE -> {}
                        }

                        // Giới hạn kích thước tối thiểu
                        if (imageItem.rect.width() < 20f) {
                            if (currentResizeHandle == ResizeHandle.TOP_LEFT || currentResizeHandle == ResizeHandle.BOTTOM_LEFT) {
                                imageItem.rect.left = imageItem.rect.right - 20f
                            } else {
                                imageItem.rect.right = imageItem.rect.left + 20f
                            }
                        }

                        if (imageItem.rect.height() < 20f) {
                            if (currentResizeHandle == ResizeHandle.TOP_LEFT || currentResizeHandle == ResizeHandle.TOP_RIGHT) {
                                imageItem.rect.top = imageItem.rect.bottom - 20f
                            } else {
                                imageItem.rect.bottom = imageItem.rect.top + 20f
                            }
                        }

                        invalidate()
                        return true
                    }
                }

                if (scaleDetector.isInProgress) {
                    return true
                } else if (event.pointerCount == 1) {
                    if (!isAdjustingImageMode) {
                        path.lineTo(x, y)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isAdjustingImageMode && selectedImageIndex != -1) {
                    isResizingImage = false
                    isMovingImage = false
                    currentResizeHandle = ResizeHandle.NONE
                    invalidate()
                    return true
                }

                if (!scaleDetector.isInProgress && !isAdjustingImageMode) {
                    if (!path.isEmpty) {
                        val newPath = Pair(Path(path), if (isEraseMode) Paint(erasePaint) else Paint(paint))
                        paths.add(newPath)
                        actions.add(Action(ActionType.PATH, path = newPath)) // Ghi lại hành động
                        undonePaths.clear()
                        undoneActions.clear() // Xóa redo khi thêm hành động mới
                        path.reset()
                        invalidate()
                    }
                }
                return true
            }
        }

        return true
    }

    private fun updateGeometryPath(startX: Float, startY: Float, endX: Float, endY: Float, targetPath: Path = tempPath) {
        targetPath.reset()
        when (geometryMode) {
            GeometryTool.LINE -> {
                targetPath.moveTo(startX, startY)
                targetPath.lineTo(endX, endY)
            }
            GeometryTool.RECTANGLE -> {
                targetPath.addRect(
                    min(startX, endX), min(startY, endY),
                    max(startX, endX), max(startY, endY),
                    Path.Direction.CW
                )
            }
            GeometryTool.CIRCLE -> {
                val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                targetPath.addCircle(startX, startY, radius, Path.Direction.CW)
            }
            GeometryTool.TRIANGLE -> {
                val radius = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
                val centerX = startX
                val centerY = startY
                targetPath.moveTo(centerX, centerY - radius)
                targetPath.lineTo(centerX - radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
                targetPath.lineTo(centerX + radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
                targetPath.close()
            }
            GeometryTool.NONE -> {}
        }
    }

    fun resetZoom() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        invalidate()
    }

    fun setColor(color: Int) {
        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    paint.color = color // Sử dụng màu mới
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        } else {
            paint.color = color
        }
        pixelDefaultPaint.color = color
    }

    fun setBrushStyle(style: String) {
        // Tắt chế độ tẩy nếu đang bật
        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    paint.style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = paint.color
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        }

        if (isPixelMode) return

        // Đặt lại tất cả thuộc tính của paint để tránh ảnh hưởng từ các kiểu trước
        paint = Paint().apply {
            isAntiAlias = true
            color = paint.color // Giữ màu hiện tại
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            shader = null
            pathEffect = null
            maskFilter = null
        }

        when (style) {
            "marker" -> {
                paint.strokeWidth = 10f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 255
                paint.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.INNER)
            }
            "feather" -> {
                paint.strokeWidth = 6f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 180
                paint.maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
            }
            "pencil" -> {
                paint.strokeWidth = 8f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 230
                paint.pathEffect = ComposePathEffect(
                    CornerPathEffect(5f),
                    PathDashPathEffect(
                        createPressurePath(),
                        5f,
                        0f,
                        PathDashPathEffect.Style.ROTATE
                    )
                )
                paint.maskFilter = BlurMaskFilter(1.7f, BlurMaskFilter.Blur.NORMAL)
            }
            "ink" -> {
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 180
            }
            "fountain_pen" -> {
                paint.strokeWidth = 10f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 250
                paint.maskFilter = BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL)
                paint.pathEffect = ComposePathEffect(
                    CornerPathEffect(5f),
                    PathDashPathEffect(
                        createPressurePath(),
                        8f,
                        0f,
                        PathDashPathEffect.Style.ROTATE
                    )
                )
            }
            "brush" -> {
                paint.strokeWidth = 20f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 120
                val baseColor = paint.color
                val startColor = Color.argb(120, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                val endColor = Color.argb(80, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                paint.shader = LinearGradient(
                    0f, 0f, paint.strokeWidth, 0f,
                    intArrayOf(startColor, endColor),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.maskFilter = BlurMaskFilter(2.5f, BlurMaskFilter.Blur.NORMAL)
            }
            "magicBrush" -> {
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 5f
                paint.alpha = 255
                paint.maskFilter = null // Đảm bảo không có hiệu ứng mờ
                paint.pathEffect = null // Đảm bảo không có path effect
            }
        }
        geometryMode = GeometryTool.NONE
        tempPath.reset()
        isDrawingGeometry = false
        isAdjustingImageMode = false
        invalidate()
    }
    private fun createPressurePath(): Path {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(3f, 2f)
        path.lineTo(5f, 1f)
        return path
    }

    fun erase() {
        if (isEraseMode) {
            isEraseMode = false
            if (isPixelMode) {
                pixelDefaultPaint.color = paint.color
            } else {
                savedPaintState?.let { paint = Paint(it) }
                savedPaintState = null
            }
        } else {
            isEraseMode = true
            if (!isPixelMode) {
                // Lưu state path hiẹn tại
                savedPaintState = Paint(paint)
                paint = Paint(erasePaint) //  erasePaint cho chế độ thường
            }
        }
        geometryMode = GeometryTool.NONE
        tempPath.reset()
        isDrawingGeometry = false
        isAdjustingImageMode = false
    }

//    fun setEraseMode(size: Float) {
//        isEraseMode = true
//        if (!isPixelMode) {
//            savedPaintState = Paint(paint)
//            erasePaint.strokeWidth = size
//            paint = Paint(erasePaint)
//        }
//        invalidate()
//    }

    fun setEraseMode(size: Float) {
        isEraseMode = true
        if (!isPixelMode) {
            savedPaintState = Paint(paint)
            erasePaint.strokeWidth = size
            paint = Paint(erasePaint)
        }
        invalidate()
    }
    fun undo(): Boolean {
        if (actions.isEmpty()) return false

        val lastAction = actions.removeAt(actions.size - 1)
        when (lastAction.type) {
            ActionType.PATH -> {
                lastAction.path?.let { paths.remove(it); undonePaths.add(it) }
            }
            ActionType.TEXT -> {
                lastAction.text?.let { texts.remove(it); undoneTexts.add(it) }
            }
            ActionType.IMAGE -> {
                lastAction.image?.let {
                    imageItems.remove(it)
                    undoneImages.add(it)
                    if (selectedImageIndex == imageItems.size) {
                        selectedImageIndex = -1
                        isAdjustingImageMode = false
                    }
                }
            }
        }
        undoneActions.add(lastAction)
        invalidate()
        return true
    }

    fun redo(): Boolean {
        if (undoneActions.isEmpty()) return false

        val lastUndoneAction = undoneActions.removeAt(undoneActions.size - 1)
        when (lastUndoneAction.type) {
            ActionType.PATH -> {
                lastUndoneAction.path?.let { paths.add(it); undonePaths.remove(it) }
            }
            ActionType.TEXT -> {
                lastUndoneAction.text?.let { texts.add(it); undoneTexts.remove(it) }
            }
            ActionType.IMAGE -> {
                lastUndoneAction.image?.let {
                    imageItems.add(it)
                    undoneImages.remove(it)
                    selectedImageIndex = imageItems.size - 1
                    isAdjustingImageMode = true
                }
            }
        }
        actions.add(lastUndoneAction)
        invalidate()
        return true
    }

    fun clear() {
        paths.clear()
        undonePaths.clear()
        path.reset()
        texts.clear()
        undoneTexts.clear()
        imageItems.clear()
        undoneImages.clear()
        actions.clear()
        undoneActions.clear()
        selectedImageIndex = -1
        isPixelMode = false
        isTextMode = false
        currentText = null
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        isEraseMode = false
        isAdjustingImageMode = false
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.BLACK
        }
        savedPaintState = null
        resetZoom()
        invalidate()
    }

    private var backgroundColor = Color.WHITE

    fun getBitmap(includeImages: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(backgroundColor)

        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        try {
            canvas.save()
            drawBackgroundTemplate(canvas)
            canvas.restore()

            canvas.save()
            canvas.translate(posX, posY)
            canvas.scale(scaleFactor, scaleFactor)

            if (includeImages) {
                for (imageItem in imageItems) {
                    val srcRect = Rect(0, 0, imageItem.originalWidth, imageItem.originalHeight)
                    canvas.drawBitmap(imageItem.bitmap, srcRect, imageItem.rect, null)
                }
            }

            for ((p, pt) in paths) {
                canvas.drawPath(p, pt)
            }

            for (textItem in texts) {
                canvas.drawText(textItem.text, textItem.x, textItem.y, textItem.paint)
            }
            canvas.restore()
        } finally {
            canvas.restoreToCount(saveCount)
        }

        return bitmap
    }

    fun setCanvasBackgroundColor(color: Int) {
        backgroundColor = color
    }

    fun getCurrentBackgroundColor(): Int {
        return backgroundColor
    }

    fun enableTextMode(text: String, fontFamily: String = "sans-serif", textSize: Float = 24f, textColor: Int = Color.BLACK) {
        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = paint.color
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        }

        isTextMode = true
        currentText = text
        currentFontFamily = fontFamily
        currentTextSize = textSize.coerceIn(12f, 100f)
        currentTextColor = textColor
        isPixelMode = false
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        isAdjustingImageMode = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

        val textPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            this.textSize = currentTextSize * scaleFactor
            typeface = Typeface.create(currentFontFamily, Typeface.NORMAL)
            color = currentTextColor
            isAntiAlias = true
        }
        previewTextItem = TextItem(currentText!!, 0f, 0f, textPaint)
        invalidate()
    }

    fun disableTextMode() {
        isTextMode = false
        currentText = null
        previewTextItem = null
        invalidate()
    }

    fun setBackgroundTemplate(templateType: String) {
        currentTemplate = templateType
        invalidate()
    }

    fun addImage(bitmap: Bitmap) {
        if (isPixelMode) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val defaultWidth = viewWidth / 4
        val defaultHeight = (defaultWidth * bitmap.height) / bitmap.width

        val offset = imageItems.size * 50f
        val left = (viewWidth - defaultWidth) / 2 + offset
        val top = (viewHeight - defaultHeight) / 2 + offset
        val rect = RectF(left, top, left + defaultWidth, top + defaultHeight)

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
        }

        val newImage = ImageItem(bitmap, rect, true, bitmap.width, bitmap.height, true)
        imageItems.add(newImage)
        actions.add(Action(ActionType.IMAGE, image = newImage)) // Ghi hành động
        undoneActions.clear() // Xóa redo khi thêm hành động mới
        selectedImageIndex = imageItems.size - 1
        isAdjustingImageMode = true
        isEraseMode = false
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        invalidate()
    }

    fun enableImageAdjustMode() {
        // Tắt chế độ tẩy nếu đang bật
        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    paint.style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = paint.color
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        }
        isAdjustingImageMode = true
        isPixelMode = false
        isTextMode = false
        geometryMode = GeometryTool.NONE
        isEraseMode = false
        isDrawingGeometry = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

        invalidate()
    }

    data class TextItem(val text: String, val x: Float, val y: Float, val paint: Paint)

    fun enablePixelMode(pixelSize: Int, widthInPixels: Int, heightInPixels: Int, showGrid: Boolean = true) {
        if (imageItems.isNotEmpty()) return

        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = paint.color
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        }

        isPixelMode = true
        this.pixelSize = pixelSize.toFloat()
        this.canvasWidthInPixels = widthInPixels
        this.canvasHeightInPixels = heightInPixels
        this.showPixelGrid = showGrid
        isTextMode = false
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        isAdjustingImageMode = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

        pixelDefaultPaint.color = paint.color
        setBackgroundTemplate("white")
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
        if (isEraseMode) {
            isEraseMode = false
            savedPaintState?.let { paint = Paint(it) } ?: run {
                paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    color = paint.color
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
            }
            savedPaintState = null
        }

        geometryMode = tool
        isPixelMode = false
        isTextMode = false
        isDrawingGeometry = false
        isAdjustingImageMode = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

        path.reset()
        tempPath.reset()
        invalidate()
    }


    fun disableGeometryMode() {
        geometryMode = GeometryTool.NONE
        tempPath.reset()
        isDrawingGeometry = false
        invalidate()
    }

    // Getter để ViewModel truy cập trạng thái
    fun getPaintColor(): Int = paint.color
    fun getPaintStrokeWidth(): Float = paint.strokeWidth
    fun getPaintStyle(): String = paint.style.toString()
}