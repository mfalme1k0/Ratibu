package com.ik0ha.ratibu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.ik0ha.ratibu.data.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheManager = CacheManager(application)
    
    private val _themePreference = MutableStateFlow<Boolean?>(cacheManager.getThemePreference())
    val themePreference: StateFlow<Boolean?> = _themePreference

    fun updateTheme(isDarkMode: Boolean?) {
        Log.d("MainViewModel", "Updating theme to: $isDarkMode")
        _themePreference.value = isDarkMode
        cacheManager.setThemePreference(isDarkMode)
    }
}
