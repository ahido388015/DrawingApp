package com.example.papercolor

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.papercolor.ui.home.DrawingAdapter
import java.io.File

class MainMenuActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var newPaperButton: android.widget.ImageView
    private val savedDrawings = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        recyclerView = findViewById(R.id.recyclerViewDrawings)
        newPaperButton = findViewById(R.id.imageViewNewPaper)

        // Thiết lập RecyclerView với GridLayoutManager (2 cột)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = DrawingAdapter(savedDrawings) { file ->
            // Khi chọn tranh, chuyển đến ViewDrawingActivity với đường dẫn tranh
            val intent = Intent(this, ViewDrawingActivity::class.java)
            intent.putExtra("DRAWING_PATH", file.absolutePath)
            startActivity(intent)
        }
        newPaperButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        loadSavedDrawings()
    }

    override fun onResume() {
        super.onResume()
        loadSavedDrawings()
    }

    private fun loadSavedDrawings() {
        savedDrawings.clear()
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        dir?.listFiles()
            ?.filter { it.extension == "png" && !it.name.startsWith("draft_") }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { savedDrawings.add(it) }
        recyclerView.adapter?.notifyDataSetChanged()
    }
}