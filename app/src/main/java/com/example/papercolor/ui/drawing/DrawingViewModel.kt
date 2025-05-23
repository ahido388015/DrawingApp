package com.example.papercolor.ui.drawing

import android.graphics.Paint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.papercolor.model.DrawingModel

class DrawingViewModel : ViewModel() {
    private val drawingModel = DrawingModel()

    // Trạng thái vẽ
    val currentColor = MutableLiveData<Int>(Color.BLACK)
    val currentBrush = MutableLiveData<String>("pencil")
    val currentGeometryTool = MutableLiveData<DrawingView.GeometryTool>(DrawingView.GeometryTool.NONE)
    val isEraseMode = MutableLiveData<Boolean>(false)
    val isTextMode = MutableLiveData<Boolean>(false)
    val isPixelMode = MutableLiveData<Boolean>(false)
    val isImageAdjustMode = MutableLiveData<Boolean>(false)
    val currentTemplate = MutableLiveData<String>("white")
    val backgroundColor = MutableLiveData<Int>(Color.WHITE)
    val showPixelGrid = MutableLiveData<Boolean>(true)

    // Trạng thái văn bản
    val currentText = MutableLiveData<String?>(null)
    val currentFontFamily = MutableLiveData<String>("sans-serif")
    val currentTextSize = MutableLiveData<Float>(24f)
    val currentTextColor = MutableLiveData<Int>(Color.BLACK)



    // Trạng thái pixel mode
    val pixelSize = MutableLiveData<Float>(10f)
    val canvasWidthInPixels = MutableLiveData<Int>(50)
    val canvasHeightInPixels = MutableLiveData<Int>(50)

    // Trạng thái zoom và pan
    val scaleFactor = MutableLiveData<Float>(1.0f)
    val posX = MutableLiveData<Float>(0f)
    val posY = MutableLiveData<Float>(0f)

    var isPixelModeEnabled = MutableLiveData<Boolean>(false)

    // Hàm để áp dụng màu
    fun setColor(color: Int, drawingView: DrawingView) {
        isEraseMode.value = false
        currentColor.value = color
        drawingView.setColor(color)
        if (!drawingView.isPixelMode) {
            disableOtherModes(drawingView, except = "brush")
        }
    }

    // Hàm để áp dụng kiểu bút
    fun setBrushStyle(style: String, drawingView: DrawingView) {
        isEraseMode.value = false
        currentBrush.value = style
        drawingView.setBrushStyle(style)
        disableOtherModes(drawingView, except = "brush")
    }

    fun toggleErase(drawingView: DrawingView): Boolean {
        val newEraseMode = !(isEraseMode.value ?: false)
        isEraseMode.value = newEraseMode
        drawingView.erase()
        if (newEraseMode && !drawingView.isPixelMode) {
            disableOtherModes(drawingView, except = "erase")
        }
        return newEraseMode
    }
    fun setEraseMode(size: Float, drawingView: DrawingView) {
        drawingView.setEraseMode(size)
    }


    // Hàm để bật chế độ văn bản
    fun enableTextMode(text: String, fontFamily: String, textSize: Float, textColor: Int, drawingView: DrawingView) {
        isTextMode.value = true
        currentText.value = text
        currentFontFamily.value = fontFamily
        currentTextSize.value = textSize.coerceIn(12f, 100f)
        currentTextColor.value = textColor
        drawingView.enableTextMode(text, fontFamily, textSize, textColor)
        disableOtherModes(drawingView, except = "text")
    }

    // Hàm để tắt chế độ văn bản
    fun disableTextMode(drawingView: DrawingView) {
        isTextMode.value = false
        currentText.value = null
        drawingView.disableTextMode()
    }

    // Hàm để bật chế độ pixel
    fun enablePixelMode(pixelSize: Int, widthInPixels: Int, heightInPixels: Int, showGrid: Boolean, drawingView: DrawingView) {
        isPixelMode.value = true
        this.pixelSize.value = pixelSize.toFloat()
        canvasWidthInPixels.value = widthInPixels
        canvasHeightInPixels.value = heightInPixels
        showPixelGrid.value = showGrid
        currentTemplate.value = "white" // Đặt template về plain trong pixel mode
        drawingView.enablePixelMode(pixelSize, widthInPixels, heightInPixels, showGrid)
        disableOtherModes(drawingView, except = "pixel")
    }

    // Hàm để tắt chế độ pixel
    fun disablePixelMode(drawingView: DrawingView) {
        isPixelMode.value = false
        drawingView.disablePixelMode()
    }

    // bật/tắt lưới pixel
    fun togglePixelGrid(show: Boolean, drawingView: DrawingView) {
        showPixelGrid.value = show
        drawingView.togglePixelGrid(show)
    }

    // chọn công cụ hình học
    fun setGeometryTool(tool: DrawingView.GeometryTool, drawingView: DrawingView) {
        currentGeometryTool.value = tool
        drawingView.setGeometryTool(tool)
        disableOtherModes(drawingView, except = "geometry")
    }

    // tắt công cụ hình học
    fun disableGeometryMode(drawingView: DrawingView) {
        currentGeometryTool.value = DrawingView.GeometryTool.NONE
        drawingView.disableGeometryMode()
    }

    //bật chế độ điều chỉnh ảnh
    fun enableImageAdjustMode(drawingView: DrawingView) {
        isImageAdjustMode.value = true
        drawingView.enableImageAdjustMode()
        disableOtherModes(drawingView, except = "image_adjust")
    }

    //đặt template nền
    fun setBackgroundTemplate(templateType: String, drawingView: DrawingView) {
        currentTemplate.value = templateType
        drawingView.setBackgroundTemplate(templateType)
    }

    // đặt màu nền
    fun setCanvasBackgroundColor(color: Int, drawingView: DrawingView) {
        backgroundColor.value = color
        drawingView.setCanvasBackgroundColor(color)
    }

    // Hàm để thêm ảnh
    fun addImage(bitmap: Bitmap, drawingView: DrawingView) {
        drawingView.addImage(bitmap)
        disableOtherModes(drawingView, except = "image_adjust")
    }

    // Hàm để xử lý undo
    fun undo(drawingView: DrawingView): Boolean {
        return drawingView.undo()
    }

    // Hàm để xử lý redo
    fun redo(drawingView: DrawingView): Boolean {
        return drawingView.redo()
    }

    // Hàm để xóa toàn bộ
    fun clear(drawingView: DrawingView) {
        drawingView.clear()
        resetState()
    }

    // Hàm để lưu bản nháp
    fun saveDraft(context: Context, drawingView: DrawingView) {
        drawingModel.saveDraft(
            context = context,
            bitmap = drawingView.getBitmap(),
            paint = drawingView.paint,
            backgroundColor = drawingView.getCurrentBackgroundColor(),
            isPixelMode = drawingView.isPixelMode,
            pixelSize = drawingView.pixelSize,
            canvasWidthInPixels = drawingView.canvasWidthInPixels,
            canvasHeightInPixels = drawingView.canvasHeightInPixels,
            showPixelGrid = drawingView.showPixelGrid,
            isTextMode = drawingView.isTextMode,
            currentText = drawingView.currentText,
            currentFontFamily = drawingView.currentFontFamily,
            currentTextSize = drawingView.currentTextSize,
            currentTextColor = drawingView.currentTextColor,
            scaleFactor = drawingView.scaleFactor,
            posX = drawingView.posX,
            posY = drawingView.posY,
            geometryMode = drawingView.geometryMode,
            isEraseMode = drawingView.isEraseMode
        )
    }

    // Hàm để khôi phục bản nháp
    fun restoreDraft(context: Context, draftPath: String?, drawingView: DrawingView) {
        val state = drawingModel.restoreDraft(context, draftPath) ?: return
        drawingView.clear()
        drawingView.paint.color = state.paintColor
        drawingView.paint.strokeWidth = state.paintStrokeWidth
        drawingView.paint.style = Paint.Style.valueOf(state.paintStyle)
        drawingView.setCanvasBackgroundColor(state.backgroundColor)
        drawingView.isPixelMode = state.isPixelMode
        drawingView.pixelSize = state.pixelSize
        drawingView.canvasWidthInPixels = state.canvasWidthInPixels
        drawingView.canvasHeightInPixels = state.canvasHeightInPixels
        drawingView.showPixelGrid = state.showPixelGrid
        drawingView.isTextMode = state.isTextMode
        drawingView.currentText = state.currentText
        drawingView.currentFontFamily = state.currentFontFamily
        drawingView.currentTextSize = state.currentTextSize
        drawingView.currentTextColor = state.currentTextColor
        drawingView.scaleFactor = state.scaleFactor
        drawingView.posX = state.posX
        drawingView.posY = state.posY
        drawingView.geometryMode = state.geometryMode
        drawingView.isEraseMode = state.isEraseMode
        updateStateFromDrawingView(drawingView)
        drawingView.invalidate()
    }

    // Hàm để lấy bitmap
    fun getBitmap(drawingView: DrawingView, includeImages: Boolean = true): Bitmap {
        return drawingView.getBitmap(includeImages)
    }

    // Hàm để điều chỉnh độ sáng và độ tương phản
    fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        return drawingModel.adjustBrightnessContrast(bitmap, brightness, contrast)
    }

    // Hàm để lưu bản vẽ
    fun saveDrawing(context: Context, bitmap: Bitmap) {
        drawingModel.saveDrawing(context, bitmap)
    }

    // diable các chế độ khác
    private fun disableOtherModes(drawingView: DrawingView, except: String) {
        when (except) {
            "brush" -> {
                isEraseMode.value = false
                isTextMode.value = false
                isImageAdjustMode.value = false
                currentGeometryTool.value = DrawingView.GeometryTool.NONE
                drawingView.disableTextMode()
                drawingView.disableGeometryMode()
                // Không tắt isPixelMode để giữ pixel mode khi chọn màu
            }
            "erase" -> {
                isTextMode.value = false
                isImageAdjustMode.value = false
                currentGeometryTool.value = DrawingView.GeometryTool.NONE
                drawingView.disableTextMode()
                drawingView.disableGeometryMode()
                // Không tắt isPixelMode để giữ pixel mode khi tẩy
            }
            "text" -> {
                isEraseMode.value = false
                isPixelMode.value = false
                isImageAdjustMode.value = false
                currentGeometryTool.value = DrawingView.GeometryTool.NONE
                drawingView.disablePixelMode()
                drawingView.disableGeometryMode()
            }
            "pixel" -> {
                isEraseMode.value = false
                isTextMode.value = false
                isImageAdjustMode.value = false
                currentGeometryTool.value = DrawingView.GeometryTool.NONE
                drawingView.disableTextMode()
                drawingView.disableGeometryMode()
            }
            "geometry" -> {
                isEraseMode.value = false
                isTextMode.value = false
                isPixelMode.value = false
                isImageAdjustMode.value = false
                drawingView.disableTextMode()
                drawingView.disablePixelMode()
            }
            "image_adjust" -> {
                isEraseMode.value = false
                isTextMode.value = false
                isPixelMode.value = false
                currentGeometryTool.value = DrawingView.GeometryTool.NONE
                drawingView.disableTextMode()
                drawingView.disablePixelMode()
                drawingView.disableGeometryMode()
            }
        }
    }

    // Hàm để cập nhật trạng thái từ DrawingView
    private fun updateStateFromDrawingView(drawingView: DrawingView) {
        currentColor.value = drawingView.getPaintColor()
        backgroundColor.value = drawingView.getCurrentBackgroundColor()
        isPixelMode.value = drawingView.isPixelMode
        pixelSize.value = drawingView.pixelSize
        canvasWidthInPixels.value = drawingView.canvasWidthInPixels
        canvasHeightInPixels.value = drawingView.canvasHeightInPixels
        showPixelGrid.value = drawingView.showPixelGrid
        isTextMode.value = drawingView.isTextMode
        currentText.value = drawingView.currentText
        currentFontFamily.value = drawingView.currentFontFamily
        currentTextSize.value = drawingView.currentTextSize
        currentTextColor.value = drawingView.currentTextColor
        scaleFactor.value = drawingView.scaleFactor
        posX.value = drawingView.posX
        posY.value = drawingView.posY
        currentGeometryTool.value = drawingView.geometryMode
        isEraseMode.value = drawingView.isEraseMode
    }

    // Hàm để reset trạng thái
    private fun resetState() {
        currentColor.value = Color.BLACK
        currentBrush.value = "pencil"
        currentGeometryTool.value = DrawingView.GeometryTool.NONE
        isEraseMode.value = false
        isTextMode.value = false
        isPixelMode.value = false
        isImageAdjustMode.value = false
        currentTemplate.value = "white"
        backgroundColor.value = Color.WHITE
        showPixelGrid.value = true
        currentText.value = null
        currentFontFamily.value = "sans-serif"
        currentTextSize.value = 24f
        currentTextColor.value = Color.BLACK
        pixelSize.value = 10f
        canvasWidthInPixels.value = 50
        canvasHeightInPixels.value = 50
        scaleFactor.value = 1.0f
        posX.value = 0f
        posY.value = 0f
    }
}