package com.example.papercolor.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.papercolor.R

class ColorAdapter(
    private val colors: List<Pair<String, Int>>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position].second
        holder.bind(color)
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.colorView)

        fun bind(color: Int) {
            val drawable = colorView.background as GradientDrawable
            drawable.setColor(color)
            drawable.setStroke(2, android.graphics.Color.BLACK)

            itemView.setOnClickListener {
                onColorSelected(color)
            }
        }
    }
}