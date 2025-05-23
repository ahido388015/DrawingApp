package com.example.papercolor.ui.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.papercolor.R
import java.io.File

class DetailFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var shareButton: Button
    private lateinit var deleteButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var drawingFile: File
    private val STORAGE_PERMISSION_CODE = 100

    private val args: DetailFragmentArgs by navArgs()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("DetailFragment", "Storage permission granted successfully")
        } else {
            Toast.makeText(context, "Storage permission denied, cannot delete file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageViewDrawing)
        shareButton = view.findViewById(R.id.buttonShare)
        deleteButton = view.findViewById(R.id.buttonDelete)
        backButton = view.findViewById(R.id.backButton)

        // Lấy đường dẫn từ argument
        val drawingPath = args.drawingPath
        if (drawingPath == null) {
            Toast.makeText(context, "Drawing not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        drawingFile = File(drawingPath)
        if (!drawingFile.exists()) {
            Toast.makeText(context, "Drawing file does not exist", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val bitmap = BitmapFactory.decodeFile(drawingFile.absolutePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(context, "Unable to load drawing", Toast.LENGTH_SHORT).show()
        }

        // Sự kiện nút Share
        shareButton.setOnClickListener {
            shareDrawing()
        }

        // Sự kiện nút Back
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Sự kiện nút Delete
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Kiểm tra quyền truy cập storage
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Log.d("DetailFragment", "Storage permission already granted")
        }
    }

    private fun shareDrawing() {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                drawingFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share picture"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error while sharing: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DetailFragment", "Share error: ${e.message}")
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_confirmation, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(android.view.Gravity.CENTER)
            val params = attributes
            params.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT
            params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (drawingFile.delete()) {
                    Log.d("DetailFragment", "File deleted: ${drawingFile.absolutePath}")
                    Toast.makeText(context, "Drawing deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Log.e("DetailFragment", "Failed to delete file: ${drawingFile.absolutePath}")
                    Toast.makeText(context, "Unable to delete drawing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Storage permission denied, cannot delete", Toast.LENGTH_SHORT).show()
                checkStoragePermission()
            }
        } catch (e: Exception) {
            Log.e("DetailFragment", "Delete error: ${e.message}")
            Toast.makeText(context, "Error while deleting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}