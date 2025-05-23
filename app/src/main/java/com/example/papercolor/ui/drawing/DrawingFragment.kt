package com.example.papercolor.ui.drawing

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.papercolor.R
import com.example.papercolor.databinding.FragmentDrawingBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.content.Intent
import android.view.Window
import android.app.Activity
import android.os.Build

class DrawingFragment : Fragment() {

    private var _binding: FragmentDrawingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DrawingViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.addImage(bitmap, binding.drawingView)
                    } else {
                        Toast.makeText(requireContext(), "Không thể giải mã ảnh", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Chưa chọn ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Quyền truy cập bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("DRAWING_PATH")?.let { drawingPath ->
            viewModel.restoreDraft(requireContext(), drawingPath, binding.drawingView)
        }

        setupColorButton()
        setupBrushButton()
        setupEraseButton()
        setupUndoRedoButtons()
        setupCancelButton()
        setupSaveButton()
        setupTextButton()
        setupGeometryButton()
        setupAddImageButton()
        setupPixelButton()
        setupTemplateButton()
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveDraft(requireContext(), binding.drawingView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupColorButton() {
        binding.colorButton.setOnClickListener {
            ColorPickerDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setTitle("Chọn màu")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    viewModel.setColor(envelope.color, binding.drawingView)
                    binding.colorButton.setImageResource(R.drawable.ic_color_gray)
                })
                .setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                    binding.colorButton.setImageResource(R.drawable.ic_color_gray)
                }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show()
        }
    }

    private fun setupBrushButton() {
        binding.brushButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_brush_selection, null)
            val dialog = Dialog(requireContext()).apply {
                setContentView(dialogView)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

            dialogView.findViewById<LinearLayout>(R.id.brush_marker).setOnClickListener {
                viewModel.setBrushStyle("marker", binding.drawingView)
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
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
            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog.dismiss()
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialog.setOnDismissListener {
                binding.brushButton.setImageResource(R.drawable.ic_brush_gray)
            }
            dialog.show()
        }
    }

    private fun setupEraseButton() {
        binding.eraseButton.setOnClickListener {
            val isErasing = viewModel.toggleErase(binding.drawingView)
            binding.eraseButton.setImageResource(
                if (isErasing) R.drawable.ic_erasers_green else R.drawable.ic_erasers_gray
            )
        }
    }

    private fun setupUndoRedoButtons() {
        binding.undoButton.setOnClickListener { viewModel.undo(binding.drawingView) }
        binding.redoButton.setOnClickListener { viewModel.redo(binding.drawingView) }
    }

    private fun setupCancelButton() {
        binding.cancelButton.setOnClickListener {
            viewModel.clear(binding.drawingView)
            if (viewModel.isPixelModeEnabled.value == true) {
                viewModel.isPixelModeEnabled.value = false
                binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (checkMediaPermission()) {
                showAdjustDialog()
            } else {
                requestMediaPermission()
            }
        }
    }

    private fun setupTextButton() {
        binding.textButton.setOnClickListener { showTextInputDialog() }
    }

    private fun setupGeometryButton() {
        binding.geometryButton.setOnClickListener { showGeometryToolDialog() }
    }

    private fun setupAddImageButton() {
        binding.addImageButton.setOnClickListener {
            if (checkMediaPermission()) {
                openGallery()
            } else {
                requestMediaPermission()
            }
        }
    }

    private fun setupPixelButton() {
        binding.pixelButton.setOnClickListener {
            if (viewModel.isPixelModeEnabled.value == true) {
                viewModel.disablePixelMode(binding.drawingView)
                viewModel.isPixelModeEnabled.value = false
                binding.pixelButton.setImageResource(R.drawable.ic_pixelmode_green)
            } else {
                showPixelSettingsDialog()
            }
        }
    }

    private fun setupTemplateButton() {
        binding.templateButton.setOnClickListener { showTemplateSelectionDialog() }
    }

    private fun checkMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissions)
    }

    private fun showTextInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val editText = dialogView.findViewById<EditText>(R.id.editTextInput)
        val fontSpinner = dialogView.findViewById<Spinner>(R.id.fontSpinner)
        val sizeSeekBar = dialogView.findViewById<SeekBar>(R.id.sizeSeekBar)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        val fonts = listOf("sans-serif", "monospace", "serif")
        fontSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fonts).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

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
            }
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { binding.textButton.setImageResource(R.drawable.ic_textmode_gray) }
        dialog.show()
    }

    private fun showAdjustDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_adjust, null)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val brightnessSeekBar = dialogView.findViewById<SeekBar>(R.id.brightnessSeekBar)
        val contrastSeekBar = dialogView.findViewById<SeekBar>(R.id.contrastSeekBar)
        val backgroundButton = dialogView.findViewById<ImageButton>(R.id.backgroundButton)
        val exportImagesCheckBox = dialogView.findViewById<CheckBox>(R.id.exportImagesCheckBox)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        var selectedBackgroundColor = viewModel.backgroundColor.value ?: 0

        backgroundButton.setOnClickListener {
            ColorPickerDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setTitle("Chọn màu nền")
                .setPositiveButton("OK", ColorEnvelopeListener { envelope, _ ->
                    selectedBackgroundColor = envelope.color
                    Toast.makeText(requireContext(), "Đã chọn màu nền", Toast.LENGTH_SHORT).show()
                })
                .setNegativeButton("Đóng") { cpDialog, _ -> cpDialog.dismiss() }
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
            if (originalBitmap != null) {
                val adjusted = viewModel.adjustBrightnessContrast(originalBitmap, brightness, contrast)
                viewModel.saveDrawing(requireContext(), adjusted)
                binding.saveButton.setImageResource(R.drawable.ic_save_green)
            } else {
                Toast.makeText(requireContext(), "Không thể lấy bitmap", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            binding.saveButton.setImageResource(R.drawable.ic_save_green)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun showGeometryToolDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_geometry_selection, null)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        fun dismissDialog(shouldResetIcon: Boolean = false) {
            if (shouldResetIcon) binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray)
            dialog.dismiss()
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_line).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.LINE, binding.drawingView)
            Toast.makeText(requireContext(), "Chế độ đường thẳng", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_rectangle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.RECTANGLE, binding.drawingView)
            Toast.makeText(requireContext(), "Chế độ hình chữ nhật", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_circle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.CIRCLE, binding.drawingView)
            Toast.makeText(requireContext(), "Chế độ hình tròn", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }

        dialogView.findViewById<LinearLayout>(R.id.geometry_triangle).setOnClickListener {
            viewModel.setGeometryTool(DrawingView.GeometryTool.TRIANGLE, binding.drawingView)
            Toast.makeText(requireContext(), "Chế độ tam giác", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener { dismissDialog(true) }
        dialog.setOnDismissListener { binding.geometryButton.setImageResource(R.drawable.ic_geometry_gray) }
        dialog.show()
    }

    private fun showPixelSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pixel_settings, null)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val widthInput = dialogView.findViewById<EditText>(R.id.etWidth)
        val heightInput = dialogView.findViewById<EditText>(R.id.etHeight)
        val sizeInput = dialogView.findViewById<EditText>(R.id.etPixelSize)
        val gridCheckbox = dialogView.findViewById<CheckBox>(R.id.cbShowGrid)
        val okButton = dialogView.findViewById<Button>(R.id.ok_button)

        widthInput.setText("50")
        heightInput.setText("50")
        sizeInput.setText("15")

        val textWatcher = object : TextWatcher {
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

        dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showTemplateSelectionDialog() {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_template_selection)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

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
            binding.templateButton.setImageResource(R.drawable.ic_temp_line)
            dialog.dismiss()
        }

        gridTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("grid", binding.drawingView)
            dialog.dismiss()
        }

        dottedTemplate.setOnClickListener {
            viewModel.setBackgroundTemplate("dotted", binding.drawingView)
            binding.templateButton.setImageResource(R.drawable.ic_temp_dots)
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            binding.templateButton.setImageResource(R.drawable.ic_template_green)
            dialog.dismiss()
        }
        dialog.show()
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
        private const val PICK_IMAGE_REQUEST = 102
    }
}