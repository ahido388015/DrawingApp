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


class ViewDrawingActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var shareButton: Button
    private lateinit var deleteButton: Button
    private lateinit var drawingFile: File
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_drawing)

        imageView = findViewById(R.id.imageViewDrawing)
        shareButton = findViewById(R.id.buttonShare)
        deleteButton = findViewById(R.id.buttonDelete)

        // Lấy đường dẫn tranh từ Intent
        val drawingPath = intent.getStringExtra("DRAWING_PATH")
        if (drawingPath == null) {
            Toast.makeText(this, "Không tìm thấy tranh", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        drawingFile = File(drawingPath)
        if (!drawingFile.exists()) {
            Toast.makeText(this, "File tranh không tồn tại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Hiển thị tranh
        val bitmap = BitmapFactory.decodeFile(drawingFile.absolutePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Không thể tải tranh", Toast.LENGTH_SHORT).show()
        }

        // Xử lý nút chia sẻ
        shareButton.setOnClickListener {
            shareDrawing()
        }

        // Xử lý nút xóa
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Kiểm tra và yêu cầu quyền truy cập bộ nhớ
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    // Quyền đã được cấp
                    Log.d("ViewDrawingActivity", "Quyền bộ nhớ đã được cấp")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    Toast.makeText(this, "Quyền bộ nhớ cần thiết để xóa file", Toast.LENGTH_SHORT).show()
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
                Log.d("ViewDrawingActivity", "Quyền bộ nhớ được cấp thành công")
            } else {
                Toast.makeText(this, "Quyền bộ nhớ bị từ chối, không thể xóa file", Toast.LENGTH_SHORT).show()
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

            startActivity(Intent.createChooser(shareIntent, "Chia sẻ tranh"))
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ViewDrawingActivity", "Share error: ${e.message}")
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xóa tranh")
            .setMessage("Bạn có chắc muốn xóa tranh này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteDrawing()
            }
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteDrawing() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (drawingFile.delete()) {
                    Log.d("ViewDrawingActivity", "File deleted: ${drawingFile.absolutePath}")
                    Toast.makeText(this, "Đã xóa tranh", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SplashActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("ViewDrawingActivity", "Failed to delete file: ${drawingFile.absolutePath}")
                    Toast.makeText(this, "Không thể xóa tranh", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Quyền bộ nhớ bị từ chối, không thể xóa", Toast.LENGTH_SHORT).show()
                checkStoragePermission()
            }
        } catch (e: Exception) {
            Log.e("ViewDrawingActivity", "Delete error: ${e.message}")
            Toast.makeText(this, "Lỗi khi xóa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}