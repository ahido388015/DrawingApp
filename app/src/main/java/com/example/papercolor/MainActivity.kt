package com.example.papercolor

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.papercolor.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.File
import java.io.FileOutputStream
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: DrawingViewModel
    private val STORAGE_PERMISSION_CODE = 101
    private val PICK_IMAGE_REQUEST = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(DrawingViewModel::class.java)

        // Kiểm tra nếu mở tranh từ SplashActivity
        val drawingPath = intent.getStringExtra("DRAWING_PATH")
        if (drawingPath != null) {
            binding.drawingView.restoreDraft(this, drawingPath)
        }

        viewModel.currentColor.observe(this) { color ->
            binding.drawingView.setColor(color)
        }
        viewModel.currentBrush.observe(this) { brush ->
            binding.drawingView.setBrushStyle(brush)
        }

        binding.colorButton.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Chọn màu")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    viewModel.currentColor.value = envelope.color
                })
                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show()
        }

//        binding.brushButton.setOnClickListener {
//            val brushOptions = arrayOf("Marker", "Feather", "Pencil", "Ink")
//            AlertDialog.Builder(this)
//                .setTitle("Chọn kiểu bút")
//                .setItems(brushOptions) { _, which ->
//                    binding.drawingView.disableTextMode()
//                    when (which) {
//                        0 -> viewModel.currentBrush.value = "marker"
//                        1 -> viewModel.currentBrush.value = "feather"
//                        2 -> viewModel.currentBrush.value = "pencil"
//                        3 -> viewModel.currentBrush.value = "ink"
//                    }
//                }
//                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
//                .show()
//        }

        binding.brushButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_brush_selection, null)
            val dialog = Dialog(this)
            dialog.setContentView(dialogView)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogView.findViewById<LinearLayout>(R.id.brush_marker).setOnClickListener {
                binding.drawingView.disableTextMode()
                viewModel.currentBrush.value = "marker"
                dialog.dismiss()
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_feather).setOnClickListener {
                binding.drawingView.disableTextMode()
                viewModel.currentBrush.value = "feather"
                dialog.dismiss()
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_pencil).setOnClickListener {
                binding.drawingView.disableTextMode()
                viewModel.currentBrush.value = "pencil"
                dialog.dismiss()
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_ink).setOnClickListener {
                binding.drawingView.disableTextMode()
                viewModel.currentBrush.value = "ink"
                dialog.dismiss()
            }
            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        binding.eraseButton.setOnClickListener {
            binding.drawingView.erase()
        }

        binding.undoButton.setOnClickListener {
            if (!binding.drawingView.undo()) {
                binding.drawingView.undoBackground()
            }
        }

        binding.redoButton.setOnClickListener {
            if (!binding.drawingView.redo()) {
                binding.drawingView.redoBackground()
            }
        }

        binding.cancelButton.setOnClickListener {
            binding.drawingView.clear()
        }

        binding.saveButton.setOnClickListener {
            if (checkPermission()) {
                showAdjustDialog()
            } else {
                requestPermission()
            }
        }

        binding.textButton.setOnClickListener {
            showTextInputDialog()
        }

        binding.geometryButton.setOnClickListener {
            showGeometryToolDialog()
        }

        binding.addImageButton.setOnClickListener {
            if (checkStoragePermission()) {
                openGallery()
            } else {
                requestStoragePermission()
            }
        }

        binding.pixelButton.setOnClickListener {
            showPixelSettingsDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        // Lưu trạng thái vẽ khi ứng dụng bị tạm dừng
        binding.drawingView.saveDraft(this)
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun showTextInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        val fontSpinner = dialogView.findViewById<Spinner>(R.id.fontSpinner)
        val sizeSeekBar = dialogView.findViewById<SeekBar>(R.id.sizeSeekBar)

        val fonts = listOf("sans-serif", "monospace", "serif")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fonts)
       // adapter.setDropDown gViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Nhập chữ")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val inputText = editText.text.toString()
                val selectedFont = fonts[fontSpinner.selectedItemPosition]
                val selectedSize = sizeSeekBar.progress

                if (inputText.isNotBlank()) {
                    binding.drawingView.enableTextMode(
                        text = inputText,
                        fontFamily = selectedFont,
                        textSize = selectedSize.toFloat()
                    )
                }
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveDrawing() {
        val bitmap = binding.drawingView.getBitmap()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "drawing_${System.currentTimeMillis()}.png")

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Toast.makeText(
                    this,
                    "Đã lưu ảnh thành công:\n${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()

                MediaStore.Images.Media.insertImage(
                    contentResolver,
                    file.absolutePath,
                    file.name,
                    "Drawing"
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi lưu ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun adjustBrightnessContrast(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        val canvas = Canvas(adjustedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return adjustedBitmap
    }

    private fun showAdjustDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_adjust, null)
        val brightnessSeekBar = dialogView.findViewById<SeekBar>(R.id.brightnessSeekBar)
        val contrastSeekBar = dialogView.findViewById<SeekBar>(R.id.contrastSeekBar)
        val backgroundButton = dialogView.findViewById<ImageButton>(R.id.backgroundButton)

        var selectedBackgroundColor = binding.drawingView.getCurrentBackgroundColor()

        backgroundButton.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("Chọn màu nền")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    selectedBackgroundColor = envelope.color
                    Toast.makeText(this, "Đã chọn màu nền", Toast.LENGTH_SHORT).show()
                })
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show()
        }

        AlertDialog.Builder(this)
            .setTitle("Điều chỉnh và lưu ảnh")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val brightness = brightnessSeekBar.progress - 100f
                val contrast = contrastSeekBar.progress / 100f

                binding.drawingView.setCanvasBackgroundColor(selectedBackgroundColor)

                val originalBitmap = binding.drawingView.getBitmap()
                val adjusted = adjustBrightnessContrast(originalBitmap, brightness, contrast)

                saveDrawing()
            }
            .setNegativeButton("HỦY", null)
            .show()
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PICK_IMAGE_REQUEST
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveDrawing()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            PICK_IMAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Cần quyền truy cập để chọn ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            try {
                uri?.let {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.drawingView.setBackgroundImage(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

//    private fun showGeometryToolDialog() {
//        val geometryOptions = arrayOf("Đường thẳng", "Hình chữ nhật", "Hình tròn", "Hình tam giác", "Tắt công cụ")
//        AlertDialog.Builder(this)
//            .setTitle("Chọn công cụ hình học")
//            .setItems(geometryOptions) { _, which ->
//                when (which) {
//                    0 -> {
//                        binding.drawingView.setGeometryTool(DrawingView.GeometryTool.LINE)
//                        Toast.makeText(this, "Chọn Đường thẳng", Toast.LENGTH_SHORT).show()
//                    }
//                    1 -> {
//                        binding.drawingView.setGeometryTool(DrawingView.GeometryTool.RECTANGLE)
//                        Toast.makeText(this, "Chọn Hình chữ nhật", Toast.LENGTH_SHORT).show()
//                    }
//                    2 -> {
//                        binding.drawingView.setGeometryTool(DrawingView.GeometryTool.CIRCLE)
//                        Toast.makeText(this, "Chọn Hình tròn", Toast.LENGTH_SHORT).show()
//                    }
//                    3 -> {
//                        binding.drawingView.setGeometryTool(DrawingView.GeometryTool.TRIANGLE)
//                        Toast.makeText(this, "Chọn Hình tam giác", Toast.LENGTH_SHORT).show()
//                    }
//                    4 -> {
//                        binding.drawingView.disableGeometryMode()
//                        Toast.makeText(this, "Tắt công cụ hình học", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
//            .show()
//    }

    private fun showGeometryToolDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_geometry_selection, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<LinearLayout>(R.id.geometry_line).setOnClickListener {
            binding.drawingView.setGeometryTool(DrawingView.GeometryTool.LINE)
            Toast.makeText(this, "Line Mode", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<LinearLayout>(R.id.geometry_rectangle).setOnClickListener {
            binding.drawingView.setGeometryTool(DrawingView.GeometryTool.RECTANGLE)
            Toast.makeText(this, "Rectangle Mode", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<LinearLayout>(R.id.geometry_circle).setOnClickListener {
            binding.drawingView.setGeometryTool(DrawingView.GeometryTool.CIRCLE)
            Toast.makeText(this, "Circle Mode", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<LinearLayout>(R.id.geometry_triangle).setOnClickListener {
            binding.drawingView.setGeometryTool(DrawingView.GeometryTool.TRIANGLE)
            Toast.makeText(this, "Triangle Mode", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<LinearLayout>(R.id.geometry_disable).setOnClickListener {
            binding.drawingView.disableGeometryMode()
            Toast.makeText(this, "Turn Off Geometry Tool", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

//    fun showPixelSettingsDialog() {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_pixel_settings, null)
//        val widthInput = dialogView.findViewById<EditText>(R.id.etWidth)
//        val heightInput = dialogView.findViewById<EditText>(R.id.etHeight)
//        val sizeInput = dialogView.findViewById<EditText>(R.id.etPixelSize)
//        val gridCheckbox = dialogView.findViewById<CheckBox>(R.id.cbShowGrid)
//
//        widthInput.setText("50")
//        heightInput.setText("50")
//        sizeInput.setText("15")
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Cài đặt Pixel Art")
//            .setView(dialogView)
//            .setPositiveButton("OK") { _, _ ->
//                val width = (widthInput.text.toString().toIntOrNull() ?: 50).coerceAtLeast(10)
//                val height = (heightInput.text.toString().toIntOrNull() ?: 50).coerceAtLeast(10)
//                val size = (sizeInput.text.toString().toIntOrNull() ?: 10).coerceAtLeast(10)
//
//                binding.drawingView.enablePixelMode(
//                    size, width, height,
//                    gridCheckbox.isChecked
//                )
//            }
//            .setNegativeButton("Hủy", null)
//            .create()
//
//        dialog.show()
//
//        val textWatcher = object : android.text.TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
//                val widthValid = widthInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
//                val heightValid = heightInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
//                val sizeValid = sizeInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
//                positiveButton.isEnabled = widthValid && heightValid && sizeValid
//            }
//        }
//
//        widthInput.addTextChangedListener(textWatcher)
//        heightInput.addTextChangedListener(textWatcher)
//        sizeInput.addTextChangedListener(textWatcher)
//    }
fun showPixelSettingsDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_pixel_settings, null)
    val dialog = Dialog(this)
    dialog.setContentView(dialogView)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val widthInput = dialogView.findViewById<EditText>(R.id.etWidth)
    val heightInput = dialogView.findViewById<EditText>(R.id.etHeight)
    val sizeInput = dialogView.findViewById<EditText>(R.id.etPixelSize)
    val gridCheckbox = dialogView.findViewById<CheckBox>(R.id.cbShowGrid)

    widthInput.setText("50")
    heightInput.setText("50")
    sizeInput.setText("15")

    val okButton = dialogView.findViewById<Button>(R.id.ok_button)
    okButton.isEnabled = true // Mặc định bật

    val textWatcher = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val widthValid = widthInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
            val heightValid = heightInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
            val sizeValid = sizeInput.text.toString().toIntOrNull()?.let { it >= 10 } ?: false
            okButton.isEnabled = widthValid && heightValid && sizeValid
        }
    }

    widthInput.addTextChangedListener(textWatcher)
    heightInput.addTextChangedListener(textWatcher)
    sizeInput.addTextChangedListener(textWatcher)

    okButton.setOnClickListener {
        val width = (widthInput.text.toString().toIntOrNull() ?: 50).coerceAtLeast(10)
        val height = (heightInput.text.toString().toIntOrNull() ?: 50).coerceAtLeast(10)
        val size = (sizeInput.text.toString().toIntOrNull() ?: 10).coerceAtLeast(10)

        binding.drawingView.enablePixelMode(
            size, width, height,
            gridCheckbox.isChecked
        )
        dialog.dismiss()
    }

    dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
        dialog.dismiss()
    }

    dialog.show()
}
}