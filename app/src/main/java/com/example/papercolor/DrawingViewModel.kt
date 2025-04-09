package com.example.papercolor

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class DrawingViewModel : ViewModel() {
    val currentColor = MutableLiveData<Int>(Color.BLACK)
    val currentBrush = MutableLiveData<String>("pencil")
}