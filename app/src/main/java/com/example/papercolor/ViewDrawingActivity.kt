package com.example.papercolor

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import android.Manifest
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager


class ViewDrawingActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var shareButton: Button
    private lateinit var deleteButton: Button
    private lateinit var backButton: ImageView
    private lateinit var drawingFile: File
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_drawing)

        imageView = findViewById(R.id.imageViewDrawing)
        shareButton = findViewById(R.id.buttonShare)
        deleteButton = findViewById(R.id.buttonDelete)
        backButton = findViewById(R.id.backButton)

        // lấy đường dẫn
        val drawingPath = intent.getStringExtra("DRAWING_PATH")
        if (drawingPath == null) {
            Toast.makeText(this, "Drawing not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        drawingFile = File(drawingPath)
        if (!drawingFile.exists()) {
            Toast.makeText(this, "Drawing file does not exist", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        val bitmap = BitmapFactory.decodeFile(drawingFile.absolutePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Unable to load drawing", Toast.LENGTH_SHORT).show()
        }

        // share button
        shareButton.setOnClickListener {
            shareDrawing()
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // kiểm tra  storage permission
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("ViewDrawingActivity", "Storage permission already granted")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    Toast.makeText(this, "Storage permission is required to delete the file", Toast.LENGTH_SHORT).show()
                    requestStoragePermission()
                }
                else -> {
                    requestStoragePermission()
                }
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ViewDrawingActivity", "Storage permission granted successfully")
            } else {
                Toast.makeText(this, "Storage permission denied, cannot delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareDrawing() {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                drawingFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share picture"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error while sharing: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ViewDrawingActivity", "Share error: ${e.message}")
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirmation, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Đặt nền trong suốt và căn giữa dialog
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // Căn giữa dialog trên màn hình
            setGravity(Gravity.CENTER)
            // Tùy chọn: Điều chỉnh kích thước dialog (nếu cần)
            val params = attributes
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }

        val btnKeep = dialogView.findViewById<Button>(R.id.btnKeep)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        btnKeep.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            deleteDrawing()
        }

        dialog.show()
    }

    private fun deleteDrawing() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (drawingFile.delete()) {
                    Log.d("ViewDrawingActivity", "File deleted: ${drawingFile.absolutePath}")
                    Toast.makeText(this, "Drawing deleted", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainMenuActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("ViewDrawingActivity", "Failed to delete file: ${drawingFile.absolutePath}")
                    Toast.makeText(this, "Unable to delete drawing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Storage permission denied, cannot delete", Toast.LENGTH_SHORT).show()
                checkStoragePermission()
            }
        } catch (e: Exception) {
            Log.e("ViewDrawingActivity", "Delete error: ${e.message}")
            Toast.makeText(this, "Error while deleting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}