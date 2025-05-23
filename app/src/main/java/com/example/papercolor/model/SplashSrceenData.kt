package com.example.papercolor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplashScreenData(
    val screenId: Int,
    val layoutResId: Int
) : Parcelable