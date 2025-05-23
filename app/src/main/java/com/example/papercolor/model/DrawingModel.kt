package com.example.papercolor.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import com.example.papercolor.ui.drawing.DrawingView

class DrawingModel {

    // Lưu bản nháp
    fun saveDraft(context: Context, bitmap: Bitmap, paint: Paint, backgroundColor: Int, isPixelMode: Boolean, pixelSize: Float, canvasWidthInPixels: Int, canvasHeightInPixels: Int, showPixelGrid: Boolean, isTextMode: Boolean, currentText: String?, currentFontFamily: String, currentTextSize: Float, currentTextColor: Int, scaleFactor: Float, posX: Float, posY: Float, geometryMode: DrawingView.GeometryTool, isEraseMode: Boolean) {
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

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "draft_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    // Khôi phục bản nháp
    fun restoreDraft(context: Context, draftPath: String?): DrawingState? {
        val sharedPrefs = context.getSharedPreferences("DrawingPrefs", Context.MODE_PRIVATE)
        val state = DrawingState(
            paintColor = sharedPrefs.getInt("paintColor", Color.BLACK),
            paintStrokeWidth = sharedPrefs.getFloat("paintStrokeWidth", 5f),
            paintStyle = sharedPrefs.getString("paintStyle", Paint.Style.STROKE.toString()) ?: Paint.Style.STROKE.toString(),
            backgroundColor = sharedPrefs.getInt("backgroundColor", Color.WHITE),
            isPixelMode = sharedPrefs.getBoolean("isPixelMode", false),
            pixelSize = sharedPrefs.getFloat("pixelSize", 10f),
            canvasWidthInPixels = sharedPrefs.getInt("canvasWidthInPixels", 50),
            canvasHeightInPixels = sharedPrefs.getInt("canvasHeightInPixels", 50),
            showPixelGrid = sharedPrefs.getBoolean("showPixelGrid", false),
            isTextMode = sharedPrefs.getBoolean("isTextMode", false),
            currentText = sharedPrefs.getString("currentText", null),
            currentFontFamily = sharedPrefs.getString("currentFontFamily", "sans-serif") ?: "sans-serif",
            currentTextSize = sharedPrefs.getFloat("currentTextSize", 24f),
            currentTextColor = sharedPrefs.getInt("currentTextColor", Color.BLACK),
            scaleFactor = sharedPrefs.getFloat("scaleFactor", 1.0f),
            posX = sharedPrefs.getFloat("posX", 0f),
            posY = sharedPrefs.getFloat("posY", 0f),
            geometryMode = sharedPrefs.getString("geometryMode", DrawingView.GeometryTool.NONE.name)?.let { DrawingView.GeometryTool.valueOf(it) } ?: DrawingView.GeometryTool.NONE,
            isEraseMode = sharedPrefs.getBoolean("isEraseMode", false)
        )

        val bitmap = draftPath?.let { BitmapFactory.decodeFile(it) }
        return if (bitmap != null) state.copy(backgroundBitmap = bitmap) else null
    }

    // Lưu bản vẽ
    fun saveDrawing(context: Context, bitmap: Bitmap) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "drawing_${System.currentTimeMillis()}.png")

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Toast.makeText(
                    context,
                    "Saved successfully:\n${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()

                MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    file.absolutePath,
                    file.name,
                    "Drawing"
                )
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Điều chỉnh độ sáng và độ tương phản
    fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val cm = android.graphics.ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }

        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        val canvas = android.graphics.Canvas(adjustedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return adjustedBitmap
    }

    // Lớp dữ liệu để lưu trạng thái
    data class DrawingState(
        val paintColor: Int,
        val paintStrokeWidth: Float,
        val paintStyle: String,
        val backgroundColor: Int,
        val isPixelMode: Boolean,
        val pixelSize: Float,
        val canvasWidthInPixels: Int,
        val canvasHeightInPixels: Int,
        val showPixelGrid: Boolean,
        val isTextMode: Boolean,
        val currentText: String?,
        val currentFontFamily: String,
        val currentTextSize: Float,
        val currentTextColor: Int,
        val scaleFactor: Float,
        val posX: Float,
        val posY: Float,
        val geometryMode: DrawingView.GeometryTool,
        val isEraseMode: Boolean,
        val backgroundBitmap: Bitmap? = null
    )
}