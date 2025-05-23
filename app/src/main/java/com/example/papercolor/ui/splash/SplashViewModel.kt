package com.example.papercolor.ui.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.papercolor.R
import com.example.papercolor.model.SplashScreenData
import com.example.papercolor.utils.helpers.HandlerHelper


class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentScreen = MutableLiveData<Int>(0)
    val currentScreen: LiveData<Int> get() = _currentScreen

    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> get() = _progress

    private val _navigateToMainMenu = MutableLiveData<Boolean>()
    val navigateToMainMenu: LiveData<Boolean> get() = _navigateToMainMenu

    val screens = listOf(
        SplashScreenData(0, R.layout.fragment_splash),
        SplashScreenData(1, R.layout.fragment_splash),
        SplashScreenData(2, R.layout.fragment_splash),
        SplashScreenData(3, R.layout.fragment_splash)
    )

    init {
        simulateLoading()
    }

    private fun simulateLoading() {
        HandlerHelper.postDelayed(100) {
            val progress = _progress.value ?: 0
            if (progress < 100) {
                _progress.value = progress + 5
                simulateLoading()
            } else {
                nextScreen()
            }
        }
    }

    fun nextScreen() {
        val current = _currentScreen.value ?: 0
        if (current < screens.size - 1) {
            _currentScreen.value = current + 1
        }
    }

    fun startMainMenu() {
        _navigateToMainMenu.value = true
    }
}