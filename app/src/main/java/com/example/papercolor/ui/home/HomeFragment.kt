package com.example.papercolor.ui.home

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.papercolor.R
import java.io.File



class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var newPaperButton: ImageView
    private val savedDrawings = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewDrawings)
        newPaperButton = view.findViewById(R.id.imageViewNewPaper)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = DrawingAdapter(savedDrawings) { file ->
            val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(file.absolutePath)
            findNavController().navigate(action)
        }

        newPaperButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_drawingFragment)
        }

        loadSavedDrawings()
    }

    override fun onResume() {
        super.onResume()
        loadSavedDrawings()
    }

    private fun loadSavedDrawings() {
        savedDrawings.clear()
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        dir?.listFiles()
            ?.filter { it.extension == "png" && !it.name.startsWith("draft_") }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { savedDrawings.add(it) }
        recyclerView.adapter?.notifyDataSetChanged()
    }
}