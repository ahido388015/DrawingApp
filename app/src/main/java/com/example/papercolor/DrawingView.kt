package com.example.papercolor

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
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

    // List chứa tất cả ảnh
    private val imageItems = mutableListOf<ImageItem>()
    private var selectedImageIndex = -1
    private var currentResizeHandle = ResizeHandle.NONE
    private var lastTouchTime: Long = 0
    private var isResizingImage = false
    private var isMovingImage = false
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    // Stack để quản lý lịch sử ảnh nền
    private val backgroundStack = mutableListOf<Bitmap?>()
    private var currentBackgroundIndex = -1

    // Biến để kiểm soát trạng thái bút xóa
    private var isEraseMode = false

    // Biến kiểm soát trạng thái điều chỉnh ảnh
    private var isAdjustingImageMode = false // Chế độ điều chỉnh ảnh riêng biệt

    // Enum để xác định handle resize
    private enum class ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE
    }

    // Class để lưu thông tin ảnh
    private data class ImageItem(
        val bitmap: Bitmap,
        var rect: RectF,
        var isSelected: Boolean = false,
        var originalWidth: Int = bitmap.width,
        var originalHeight: Int = bitmap.height,
        var isAdjustable: Boolean = true // Biến để kiểm soát trạng thái điều chỉnh của ảnh
    )

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isAdjustingImageMode && selectedImageIndex != -1) {
                val imageItem = imageItems[selectedImageIndex]
                val scale = detector.scaleFactor
                val newWidth = imageItem.rect.width() * scale
                val newHeight = imageItem.rect.height() * scale
                val pivotX = imageItem.rect.centerX()
                val pivotY = imageItem.rect.centerY()

                imageItem.rect.left = pivotX - newWidth / 2
                imageItem.rect.top = pivotY - newHeight / 2
                imageItem.rect.right = pivotX + newWidth / 2
                imageItem.rect.bottom = pivotY + newHeight / 2

                invalidate()
                return true
            }
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

    // Hàm mới để kiểm tra touch vào handle resize
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

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor)

        // Vẽ tất cả ảnh từ imageItems
        for ((index, imageItem) in imageItems.withIndex()) {
            val srcRect = Rect(0, 0, imageItem.originalWidth, imageItem.originalHeight)
            canvas.drawBitmap(imageItem.bitmap, srcRect, imageItem.rect, null)

            if (imageItem.isSelected && isAdjustingImageMode) {
                val handlePaint = Paint().apply {
                    color = Color.BLUE
                    style = Paint.Style.FILL
                    strokeWidth = 2f
                }

                val borderPaint = Paint().apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }

                canvas.drawRect(imageItem.rect, borderPaint)

                val handleSize = 15f
                canvas.drawCircle(imageItem.rect.left, imageItem.rect.top, handleSize, handlePaint)
                canvas.drawCircle(imageItem.rect.right, imageItem.rect.top, handleSize, handlePaint)
                canvas.drawCircle(imageItem.rect.left, imageItem.rect.bottom, handleSize, handlePaint)
                canvas.drawCircle(imageItem.rect.right, imageItem.rect.bottom, handleSize, handlePaint)
            }
        }

        for ((p, pt) in paths) {
            canvas.drawPath(p, pt)
        }

        canvas.drawPath(path, paint)

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
                    tempPath.moveTo(centerX, centerY - radius)
                    tempPath.lineTo(centerX - radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
                    tempPath.lineTo(centerX + radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
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
                                finalPath.moveTo(centerX, centerY - radius)
                                finalPath.lineTo(centerX - radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
                                finalPath.lineTo(centerX + radius * cos(PI / 6).toFloat(), centerY + radius * sin(PI / 6).toFloat())
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

        val invertedMatrix = Matrix()
        val transformMatrix = Matrix().apply {
            postTranslate(posX, posY)
            postScale(scaleFactor, scaleFactor)
            invert(invertedMatrix)
        }
        val pts = floatArrayOf(event.x, event.y)
        invertedMatrix.mapPoints(pts)
        val x = pts[0]
        val y = pts[1]

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTouchTime < 300) { // Chạm đúp
                    if (selectedImageIndex != -1 && isAdjustingImageMode) {
                        // Khi chạm đúp, cố định ảnh
                        imageItems[selectedImageIndex].isSelected = false
                        imageItems[selectedImageIndex].isAdjustable = false
                        selectedImageIndex = -1
                        isAdjustingImageMode = false
                        invalidate()
                    }
                    lastTouchTime = 0
                    return true
                }
                lastTouchTime = currentTime

                if (isAdjustingImageMode) {
                    // Kiểm tra xem có chọn được ảnh nào để điều chỉnh không
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

                    // Nếu không chọn được ảnh nào, thoát chế độ điều chỉnh
                    if (selectedImageIndex != -1) {
                        imageItems[selectedImageIndex].isSelected = false
                        selectedImageIndex = -1
                        isAdjustingImageMode = false
                        invalidate()
                    }
                }

                // Nếu không ở chế độ điều chỉnh ảnh, bắt đầu vẽ đường path
                lastTouchX = event.x
                lastTouchY = event.y
                path.moveTo(x, y)
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
                    // Vẽ đường path nếu không ở chế độ điều chỉnh ảnh
                    path.lineTo(x, y)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isAdjustingImageMode && selectedImageIndex != -1) {
                    isResizingImage = false
                    isMovingImage = false
                    currentResizeHandle = ResizeHandle.NONE
                }

                if (!scaleDetector.isInProgress && !isAdjustingImageMode) {
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

    fun resetZoom() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        invalidate()
    }

    fun setColor(color: Int) {
        if (!isEraseMode) { // Chỉ thay đổi màu nếu không ở chế độ xóa
            paint.color = color
            pixelPaint.color = color
        }
    }

    fun setBrushStyle(style: String) {
        when (style) {
            "marker" -> {
                paint.strokeWidth = 10f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
            }
            "feather" -> {
                paint.strokeWidth = 7f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
            }
            "pencil" -> {
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
            }
            "ink" -> {
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE
            }
//            "fountain_pen" -> {
//                paint.strokeWidth = 6f
//                paint.style = Paint.Style.STROKE
//                paint.strokeCap = Paint.Cap.ROUND
//                paint.strokeJoin = Paint.Join.ROUND
//                paint.alpha = 200 // Giảm alpha để tạo hiệu ứng mực loang nhẹ
//                paint.shader = null // Bỏ shader để dùng màu từ paint.color
//                paint.maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
//                paint.pathEffect = ComposePathEffect(
//                    CornerPathEffect(5f),
//                    PathDashPathEffect(
//                        createPressurePath(),
//                        10f,
//                        0f,
//                        PathDashPathEffect.Style.ROTATE
//                    )
//                )
//            }
            "fountain_pen" -> {
                paint.strokeWidth = 6f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 200
                paint.shader = null
                paint.maskFilter = BlurMaskFilter(1.5f, BlurMaskFilter.Blur.NORMAL)
                paint.pathEffect = ComposePathEffect(
                    CornerPathEffect(5f),
                    PathDashPathEffect(
                        createPressurePath(),
                        5f,
                        0f,
                        PathDashPathEffect.Style.ROTATE
                    )
                )
            }
            "brush" -> {
                paint.strokeWidth = 30f // Tăng độ rộng để giống cọ hơn
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.alpha = 100 // Giảm alpha để tạo độ trong suốt, giống màu nước
                paint.shader = null // Đảm bảo dùng màu từ paint.color
                paint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL) // Tăng độ mờ để loang mạnh hơn
                // Điều chỉnh pathEffect để tạo hiệu ứng loang tự nhiên, bớt cứng
                paint.pathEffect = ComposePathEffect(
                    CornerPathEffect(20f), // Làm mềm góc hơn nữa
                    DiscretePathEffect(25f, 3f) // Tăng segment length, giảm deviation để loang tự nhiên
                )
            }
        }
        isEraseMode = false // Tắt chế độ xóa khi thay đổi kiểu bút
        geometryMode = GeometryTool.NONE // Tắt chế độ hình học khi thay đổi kiểu bút
        tempPath.reset()
        isDrawingGeometry = false
        isAdjustingImageMode = false // Tắt chế độ điều chỉnh ảnh
    }
    private fun createPressurePath(): Path {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(5f, 5f)
        path.lineTo(10f, 2f)
        return path
    }

    fun erase() {
        isEraseMode = true
        paint.color = Color.WHITE // Luôn là màu trắng
        paint.strokeWidth = 20f
        geometryMode = GeometryTool.NONE // Tắt chế độ hình học khi kích hoạt bút xóa
        tempPath.reset()
        isDrawingGeometry = false
        isAdjustingImageMode = false // Tắt chế độ điều chỉnh ảnh
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
        } else if (imageItems.isNotEmpty() && selectedImageIndex == imageItems.size - 1) {
            imageItems.removeAt(imageItems.size - 1)
            selectedImageIndex = -1
            isAdjustingImageMode = false
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

    private fun updateCurrentBackground() {
        val bitmap = backgroundStack.getOrNull(currentBackgroundIndex)
        if (bitmap != null && imageItems.isNotEmpty()) {
            imageItems[0] = ImageItem(
                bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                imageItems[0].rect,
                true,
                bitmap.width,
                bitmap.height,
                imageItems[0].isAdjustable
            )
            invalidate()
        }
    }

    fun clear() {
        paths.clear()
        undonePaths.clear()
        path.reset()
        texts.clear()
        undoneTexts.clear()
        imageItems.clear()
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
        backgroundStack.clear()
        currentBackgroundIndex = -1
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

        for (imageItem in imageItems) {
            val srcRect = Rect(0, 0, imageItem.originalWidth, imageItem.originalHeight)
            canvas.drawBitmap(imageItem.bitmap, srcRect, imageItem.rect, null)
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
        isEraseMode = false
        isDrawingGeometry = false
        isAdjustingImageMode = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

        invalidate()
    }

    fun disableTextMode() {
        isTextMode = false
        currentText = null
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        if (isPixelMode) return // Không thêm ảnh nếu đang ở chế độ pixel

        if (currentBackgroundIndex < backgroundStack.size - 1) {
            backgroundStack.subList(currentBackgroundIndex + 1, backgroundStack.size).clear()
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        val left = (viewWidth - scaledWidth) / 2
        val top = (viewHeight - scaledHeight) / 2

        val rect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        val newBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)

        backgroundStack.add(newBitmap)
        currentBackgroundIndex = backgroundStack.size - 1

        if (imageItems.isEmpty()) {
            imageItems.add(ImageItem(newBitmap, rect, true, bitmap.width, bitmap.height, true))
        } else {
            imageItems[0] = ImageItem(newBitmap, rect, true, bitmap.width, bitmap.height, true)
        }
        selectedImageIndex = 0
        isAdjustingImageMode = true
        disablePixelMode() // Vô hiệu hóa chế độ pixel khi thêm ảnh
        isEraseMode = false
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        invalidate()
    }

    fun addImage(bitmap: Bitmap) {
        if (isPixelMode) return // Không thêm ảnh nếu đang ở chế độ pixel

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val defaultWidth = viewWidth / 4
        val defaultHeight = (defaultWidth * bitmap.height) / bitmap.width

        val left = (viewWidth - defaultWidth) / 2
        val top = (viewHeight - defaultHeight) / 2

        val rect = RectF(left, top, left + defaultWidth, top + defaultHeight)

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
        }

        imageItems.add(ImageItem(bitmap, rect, true, bitmap.width, bitmap.height, true))
        selectedImageIndex = imageItems.size - 1
        isAdjustingImageMode = true
        disablePixelMode() // Vô hiệu hóa chế độ pixel khi thêm ảnh
        isEraseMode = false
        geometryMode = GeometryTool.NONE
        isDrawingGeometry = false
        invalidate()
    }

    fun enableImageAdjustMode() {
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
        if (imageItems.isNotEmpty()) return // Không kích hoạt pixel mode nếu có ảnh trên canvas

        isPixelMode = true
        this.pixelSize = pixelSize.toFloat()
        this.canvasWidthInPixels = widthInPixels
        this.canvasHeightInPixels = heightInPixels
        this.showPixelGrid = showGrid
        isTextMode = false
        geometryMode = GeometryTool.NONE
        isEraseMode = false
        isDrawingGeometry = false
        isAdjustingImageMode = false

        if (selectedImageIndex != -1) {
            imageItems[selectedImageIndex].isSelected = false
            selectedImageIndex = -1
        }

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
        isEraseMode = false
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

    private fun drawPixel(x: Float, y: Float) {
        val pixelX = (x / pixelSize).toInt() * pixelSize
        val pixelY = (y / pixelSize).toInt() * pixelSize

        val pixelPath = Path().apply {
            addRect(pixelX, pixelY, pixelX + pixelSize, pixelY + pixelSize, Path.Direction.CW)
        }

        paths.add(Pair(pixelPath, Paint(paint)))
        invalidate()
    }

    // Lưu trạng thái vẽ
    fun saveDraft(context: Context) {
        val sharedPrefs = context.getSharedPreferences("DrawingPrefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt("paintColor", paint.color)
            putFloat("paintStrokeWidth", paint.strokeWidth)
            putString("paintStyle", paint.style.toString())
            putInt("backgroundColor", backgroundColor)
            putBoolean("isPixelMode", isPixelMode)
            putFloat("pixelSize", pixelSize)
            putInt("canvasWidthInPixels", canvasWidthInPixels)
            putInt("canvasHeightInPixels", canvasHeightInPixels)
            putBoolean("showPixelGrid", showPixelGrid)
            putBoolean("isTextMode", isTextMode)
            putString("currentText", currentText)
            putString("currentFontFamily", currentFontFamily)
            putFloat("currentTextSize", currentTextSize)
            putInt("currentTextColor", currentTextColor)
            putFloat("scaleFactor", scaleFactor)
            putFloat("posX", posX)
            putFloat("posY", posY)
            putString("geometryMode", geometryMode.name)
            putBoolean("isEraseMode", isEraseMode)
            apply()
        }

        // Lưu bitmap hiện tại
        val bitmap = getBitmap()
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "draft_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    // Khôi phục trạng thái vẽ
    fun restoreDraft(context: Context, draftPath: String?) {
        clear()
        val sharedPrefs = context.getSharedPreferences("DrawingPrefs", Context.MODE_PRIVATE)

        paint.color = sharedPrefs.getInt("paintColor", Color.BLACK)
        paint.strokeWidth = sharedPrefs.getFloat("paintStrokeWidth", 5f)
        paint.style = Paint.Style.valueOf(sharedPrefs.getString("paintStyle", Paint.Style.STROKE.toString()) ?: Paint.Style.STROKE.toString())
        backgroundColor = sharedPrefs.getInt("backgroundColor", Color.WHITE)
        isPixelMode = sharedPrefs.getBoolean("isPixelMode", false)
        pixelSize = sharedPrefs.getFloat("pixelSize", 10f)
        canvasWidthInPixels = sharedPrefs.getInt("canvasWidthInPixels", 50)
        canvasHeightInPixels = sharedPrefs.getInt("canvasHeightInPixels", 50)
        showPixelGrid = sharedPrefs.getBoolean("showPixelGrid", false)
        isTextMode = sharedPrefs.getBoolean("isTextMode", false)
        currentText = sharedPrefs.getString("currentText", null)
        currentFontFamily = sharedPrefs.getString("currentFontFamily", "sans-serif") ?: "sans-serif"
        currentTextSize = sharedPrefs.getFloat("currentTextSize", 24f)
        currentTextColor = sharedPrefs.getInt("currentTextColor", Color.BLACK)
        scaleFactor = sharedPrefs.getFloat("scaleFactor", 1.0f)
        posX = sharedPrefs.getFloat("posX", 0f)
        posY = sharedPrefs.getFloat("posY", 0f)
        geometryMode = GeometryTool.valueOf(sharedPrefs.getString("geometryMode", GeometryTool.NONE.name) ?: GeometryTool.NONE.name)
        isEraseMode = sharedPrefs.getBoolean("isEraseMode", false)

        // Khôi phục bitmap
        draftPath?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            setBackgroundImage(bitmap)
        }

        invalidate()
    }
}