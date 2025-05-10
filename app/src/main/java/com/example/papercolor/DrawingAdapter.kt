package com.example.papercolor

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DrawingAdapter(
    private val drawings: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<DrawingAdapter.DrawingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawing, parent, false)
        return DrawingViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawingViewHolder, position: Int) {
        val file = drawings[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int {
        return drawings.size
    }

    inner class DrawingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewDrawing)

        fun bind(file: File) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
            itemView.setOnClickListener { onClick(file) }
        }
    }
}