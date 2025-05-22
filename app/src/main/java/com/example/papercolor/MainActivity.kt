package com.example.papercolor

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.papercolor.adapter.ColorAdapter
import com.example.papercolor.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: DrawingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(DrawingViewModel::class.java)

        // Khôi phục bản nháp nếu có
        val drawingPath = intent.getStringExtra("DRAWING_PATH")
        if (drawingPath != null) {
            viewModel.restoreDraft(this, drawingPath, binding.drawingView)
        }

        binding.colorButton.setOnClickListener {
            isGeometryToolActive = false
            binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            binding.colorButton.setImageResource(R.drawable.ic_color_red)
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)

            ColorPickerDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Selected Color")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    viewModel.setColor(envelope.color, binding.drawingView)
                    binding.colorButton.setImageResource(R.drawable.ic_color_gray)
                })
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    binding.colorButton.setImageResource(R.drawable.ic_color_gray)
                }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show()
        }


        binding.brushButton.setOnClickListener {
            isGeometryToolActive = false
            binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            binding.brushButton.setImageResource(R.drawable.ic_brush_green)
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)

            val dialogView = layoutInflater.inflate(R.layout.dialog_brush_selection, null)
            val dialog = Dialog(this)
            dialog.setContentView(dialogView)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogView.findViewById<LinearLayout>(R.id.brush_marker).setOnClickListener {
                viewModel.setBrushStyle("marker", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray) // đổi về xám
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_feather).setOnClickListener {
                viewModel.setBrushStyle("feather", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_pencil).setOnClickListener {
                viewModel.setBrushStyle("pencil", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_ink).setOnClickListener {
                viewModel.setBrushStyle("ink", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_fountain_ink).setOnClickListener {
                viewModel.setBrushStyle("fountain_pen", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialogView.findViewById<LinearLayout>(R.id.brush_magic).setOnClickListener {
                viewModel.setBrushStyle("magicBrush", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }

//            dialogView.findViewById<LinearLayout>(R.id.brush_brush).setOnClickListener {
//                viewModel.setBrushStyle("brush", binding.drawingView)
//                dialog.dismiss()
//                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
//            }
            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialog.setOnDismissListener {
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialog.show()
        }


        binding.eraseButton.setOnClickListener {
            isGeometryToolActive = false
            binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
            val isErasing = viewModel.toggleErase(binding.drawingView)

            if (isErasing) {
                binding.eraseButton.setImageResource(R.drawable.ic_erasers_green)
            } else {
                binding.eraseButton.setImageResource(R.drawable.ic_erasers_gray)
            }
        }


        binding.undoButton.setOnClickListener {
            viewModel.undo(binding.drawingView)
        }

        binding.redoButton.setOnClickListener {
            viewModel.redo(binding.drawingView)
        }

        binding.cancelButton.setOnClickListener {
            isGeometryToolActive = false
            binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
            viewModel.clear(binding.drawingView)
            if (viewModel.isPixelModeEnabled.value == true) {
                viewModel.isPixelModeEnabled.value = false
                binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
            }
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
            if (viewModel.isPixelModeEnabled.value == true) {
                viewModel.disablePixelMode(binding.drawingView)
                viewModel.isPixelModeEnabled.value = false
                binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
            } else {
                showPixelSettingsDialog()
            }
        }

        binding.templateButton.setOnClickListener {
            showTemplateSelectionDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveDraft(this, binding.drawingView)
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

//    private fun showEraseSizeDialog(onSizeSelected: (Float) -> Unit) {
//        val dialog = BottomSheetDialog(this)
//        val view = layoutInflater.inflate(R.layout.dialog_erase_size, null)
//        dialog.setContentView(view)
//
//        // Ánh xạ các view kích thước
//        val size10 = view.findViewById<View>(R.id.btn_size_10)
//        val size20 = view.findViewById<View>(R.id.btn_size_20)
//        val size30 = view.findViewById<View>(R.id.btn_size_30)
//        val size40 = view.findViewById<View>(R.id.btn_size_40)
//
//        // Xử lý sự kiện click cho từng kích thước
//        size10.setOnClickListener {
//            onSizeSelected(10f)
//            dialog.dismiss()
//        }
//        size20.setOnClickListener {
//            onSizeSelected(20f)
//            dialog.dismiss()
//        }
//        size30.setOnClickListener {
//            onSizeSelected(30f)
//            dialog.dismiss()
//        }
//        size40.setOnClickListener {
//            onSizeSelected(40f)
//            dialog.dismiss()
//        }
//        dialog.show()
//    }

    private fun showTextInputDialog() {
        isGeometryToolActive = false
        binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
        binding.textButton.setImageResource(R.drawable.ic_textmode_green)
        binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)

        val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)

        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        val fontSpinner = dialogView.findViewById<Spinner>(R.id.fontSpinner)
        val sizeSeekBar = dialogView.findViewById<SeekBar>(R.id.sizeSeekBar)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val fonts = listOf("sans-serif", "monospace", "serif")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fonts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = adapter

        // Use Dialog instead of AlertDialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        // Apply rounded corners via dialog_background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        buttonOk.setOnClickListener {
            val inputText = editText.text.toString()
            val selectedFont = fonts[fontSpinner.selectedItemPosition]
            val selectedSize = sizeSeekBar.progress.toFloat().coerceIn(12f, 100f)

            if (inputText.isNotBlank()) {
                viewModel.enableTextMode(
                    inputText,
                    selectedFont,
                    selectedSize,
                    viewModel.currentColor.value ?: Color.BLACK,
                    binding.drawingView
                )
            } else {
                binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
            }
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
        }

        dialog.show()
    }


    private fun showAdjustDialog() {
        isGeometryToolActive = false
        binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
        binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
        binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
        binding.saveButton.setImageResource(R.drawable.ic_save_red)

        val dialogView = layoutInflater.inflate(R.layout.dialog_adjust, null)

        // Use Dialog instead of AlertDialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        // Make dialog background transparent to show rounded corners from background drawable
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val brightnessSeekBar = dialogView.findViewById<SeekBar>(R.id.brightnessSeekBar)
        val contrastSeekBar = dialogView.findViewById<SeekBar>(R.id.contrastSeekBar)
        val backgroundButton = dialogView.findViewById<ImageButton>(R.id.backgroundButton)
        val exportImagesCheckBox = dialogView.findViewById<CheckBox>(R.id.exportImagesCheckBox)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        var selectedBackgroundColor = viewModel.backgroundColor.value ?: Color.BLACK

        backgroundButton.setOnClickListener {
            ColorPickerDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Set Background Color")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    selectedBackgroundColor = envelope.color
                    Toast.makeText(this, "Selected background color", Toast.LENGTH_SHORT).show()
                })
                .setNegativeButton("Close") { cpDialog, _ ->
                    cpDialog.dismiss()
                }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show()
        }

        okButton.setOnClickListener {
            val brightness = brightnessSeekBar.progress - 100f
            val contrast = contrastSeekBar.progress / 100f
            val exportImages = exportImagesCheckBox.isChecked

            viewModel.setCanvasBackgroundColor(selectedBackgroundColor, binding.drawingView)
            val originalBitmap = viewModel.getBitmap(binding.drawingView, exportImages)
            val adjusted = viewModel.adjustBrightnessContrast(originalBitmap, brightness, contrast)
            viewModel.saveDrawing(this, adjusted)

            binding.saveButton.setImageResource(R.drawable.ic_save_green)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            binding.saveButton.setImageResource(R.drawable.ic_save_green)
            dialog.dismiss()
        }

        dialog.show()
    }


    //kiem tra quyen
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        requestPermissions(
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            PICK_IMAGE_REQUEST
        )
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri: Uri? = result.data?.data
            try {
                uri?.let {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    viewModel.addImage(bitmap, binding.drawingView)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
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
                    showAdjustDialog()
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

    private var isGeometryToolActive = false
    private fun showGeometryToolDialog() {
        binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
        binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
        if (!isGeometryToolActive) {
            binding.geometryButton.setImageResource(R.drawable.ic_geometry_green)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_geometry_selection, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun dismissDialogWithResetIcon(shouldResetIcon: Boolean = false) {
            if (shouldResetIcon) {
                isGeometryToolActive = false
                binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            }
            dialog.dismiss()
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_line).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.LINE, binding.drawingView)
            isGeometryToolActive = true
            Toast.makeText(this, "Line Mode", Toast.LENGTH_SHORT).show()
            dismissDialogWithResetIcon(false)
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_rectangle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.RECTANGLE, binding.drawingView)
            isGeometryToolActive = true
            Toast.makeText(this, "Rectangle Mode", Toast.LENGTH_SHORT).show()
            dismissDialogWithResetIcon(false)
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_circle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.CIRCLE, binding.drawingView)
            isGeometryToolActive = true
            Toast.makeText(this, "Circle Mode", Toast.LENGTH_SHORT).show()
            dismissDialogWithResetIcon(false)
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_triangle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.TRIANGLE, binding.drawingView)
            isGeometryToolActive = true
            Toast.makeText(this, "Triangle Mode", Toast.LENGTH_SHORT).show()
            dismissDialogWithResetIcon(false)
        }

//        dialogView.findViewById<LinearLayout>(R.id.geometry_disable).setOnClickListener {
//            viewModel.disableGeometryMode(binding.drawingView)
//            Toast.makeText(this, "Turn Off Geometry Tool", Toast.LENGTH_SHORT).show()
//            dismissDialogWithResetIcon(true)
//        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            // Cancel không thay đổi trạng thái icon
            dismissDialogWithResetIcon(false)
        }

        dialog.setOnCancelListener {
            // Cancel bằng nút back, cũng không thay đổi icon
            if (!isGeometryToolActive) {
                binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            }
        }

        dialog.show()
    }



    private fun showPixelSettingsDialog() {
        binding.textButton.setImageResource(R.drawable.ic_textmode_gray)
        isGeometryToolActive = false
        binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)

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
        okButton.isEnabled = true

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

            viewModel.enablePixelMode(size, width, height, gridCheckbox.isChecked, binding.drawingView)
            viewModel.isPixelModeEnabled.value = true
            binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_red)

            dialog.dismiss()
        }


        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTemplateSelectionDialog() {
        isGeometryToolActive = false
        binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)

        // Khởi tạo dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_template_selection)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // Khởi tạo các view trong dialog
        val whiteTemplate = dialog.findViewById<LinearLayout>(R.id.whiteTemplate)
        val linedTemplate = dialog.findViewById<LinearLayout>(R.id.linedTemplate)
        val gridTemplate = dialog.findViewById<LinearLayout>(R.id.gridTemplate)
        val dottedTemplate = dialog.findViewById<LinearLayout>(R.id.dottedTemplate)
        val closeButton = dialog.findViewById<Button>(R.id.btnCloseDialog)

        whiteTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("white", binding.drawingView)
            dialog.dismiss()
        }

        linedTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("lined", binding.drawingView)
            dialog.dismiss()
        }

        gridTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("grid", binding.drawingView)
            dialog.dismiss()
        }

        dottedTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("dotted", binding.drawingView)
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }



    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
        private const val PICK_IMAGE_REQUEST = 102
    }
}