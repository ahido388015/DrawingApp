package com.example.papercolor

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.papercolor.databinding.ActivityViewDetailBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import java.io.IOException
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener


class ViewDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewDetailBinding
    private lateinit var originalBitmap: Bitmap
    private lateinit var currentBitmap: Bitmap

    // Các thông số chỉnh sửa
    private var brightness = 0f
    private var contrast = 1f
    private var backgroundColor = Color.WHITE

    // Xử lý quyền truy cập
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveToGallery()
        } else {
            showPermissionDeniedToast()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nhận bitmap từ MainActivity
        originalBitmap = intent.getParcelableExtra<Bitmap>("BITMAP_DATA")!!
        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        binding.imageView.setImageBitmap(currentBitmap)

        setupControls()
        setupSeekBars()
    }

    private fun setupControls() {
        binding.apply {
            // Nút thay đổi màu nền
//            changeBackgroundButton.setOnClickListener {
//                showColorPickerDialog()
//            }

            // Nút lưu ảnh
            saveImageButton.setOnClickListener {
                saveImageWithChanges()
            }

            // Nút reset về ảnh gốc
            resetButton.setOnClickListener {
                resetImage()
            }

            // Nút quay lại
//            backButton.setOnClickListener {
//                onBackPressed()
//            }
        }
    }

    private fun setupSeekBars() {
        binding.brightnessSeekBar.apply {
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    brightness = (progress - 50) * 2f
                    applyChanges()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        binding.contrastSeekBar.apply {
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    contrast = 0.1f + (progress / 50f)
                    applyChanges()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

//    private fun showColorPickerDialog() {
//        ColorPickerDialog.Builder(this)
//            .setTitle("Chọn màu nền")
//            .setPositiveButton("OK") { envelope, _ ->
//                backgroundColor = envelope.color
//                applyChanges()
//            }
//            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
//            .show()
//    }

    private fun applyChanges() {
        val adjustedBitmap = applyBrightnessContrast(originalBitmap, brightness, contrast)
        val finalBitmap = addBackground(adjustedBitmap, backgroundColor)
        currentBitmap = finalBitmap
        binding.imageView.setImageBitmap(finalBitmap)
    }

    private fun applyBrightnessContrast(src: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val bitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val matrix = floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return bitmap
    }

    private fun addBackground(src: Bitmap, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(color)
        canvas.drawBitmap(src, 0f, 0f, null)

        return bitmap
    }

    private fun resetImage() {
        brightness = 0f
        contrast = 1f
        backgroundColor = Color.WHITE
        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        binding.imageView.setImageBitmap(currentBitmap)
        resetControls()
    }

    private fun resetControls() {
        binding.brightnessSeekBar.progress = 50
        binding.contrastSeekBar.progress = 50
    }

    private fun saveImageWithChanges() {
        if (checkPermission()) {
            saveToGallery()
        } else {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            showPermissionRationaleDialog()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cần quyền truy cập")
            .setMessage("Ứng dụng cần quyền ghi bộ nhớ để lưu ảnh của bạn")
            .setPositiveButton("Đồng ý") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(
            this,
            "Không thể lưu ảnh do thiếu quyền truy cập",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveToGallery() {
        try {
            val filename = "drawing_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }

                    showSaveSuccessToast()
                    setResult(RESULT_OK)
                    finish()
                } ?: throw IOException("Không thể mở OutputStream")
            } ?: throw IOException("Không thể tạo URI")
        } catch (e: Exception) {
            showSaveErrorToast(e.message)
        }
    }

    private fun showSaveSuccessToast() {
        Toast.makeText(
            this,
            "Đã lưu ảnh thành công vào thư viện",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showSaveErrorToast(errorMessage: String?) {
        Toast.makeText(
            this,
            "Lỗi khi lưu ảnh: ${errorMessage ?: "Không xác định"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}