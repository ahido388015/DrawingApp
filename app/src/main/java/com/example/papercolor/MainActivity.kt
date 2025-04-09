package com.example.papercolor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.papercolor.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: DrawingViewModel by viewModels()
    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.brushButton.setOnClickListener {
            val brushOptions = arrayOf("Marker", "Feather", "Pencil", "Ink")
            android.app.AlertDialog.Builder(this)
                .setTitle("Chọn kiểu bút")
                .setItems(brushOptions) { _, which ->
                    when (which) {
                        0 -> viewModel.currentBrush.value = "marker"
                        1 -> viewModel.currentBrush.value = "feather"
                        2 -> viewModel.currentBrush.value = "pencil"
                        3 -> viewModel.currentBrush.value = "ink"
                    }
                }
                .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        binding.eraseButton.setOnClickListener {
            binding.drawingView.erase()
        }

        binding.undoButton.setOnClickListener {
            binding.drawingView.undo()
        }

        binding.redoButton.setOnClickListener {
            binding.drawingView.redo()
        }
//
//        binding.clearButton.setOnClickListener {
//            binding.drawingView.clear()
//        }

        binding.cancelButton.setOnClickListener(){
            binding.drawingView.clear()
        }

        binding.saveButton.setOnClickListener {
            if (checkPermission()) {
                saveDrawing()
            } else {
                requestPermission()
            }
        }
    }

    // Các hàm permission và saveDrawing giữ nguyên như trước
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveDrawing()
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDrawing() {
        val bitmap = binding.drawingView.getBitmap()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "drawing_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            Toast.makeText(this, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}