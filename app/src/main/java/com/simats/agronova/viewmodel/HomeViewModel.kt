package com.simats.agronova.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.WeatherResponse
import com.simats.agronova.service.WeatherClient
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val weatherData = mutableStateOf<WeatherResponse?>(null)
    val isLoading = mutableStateOf(false)
    val apiError = mutableStateOf<String?>(null)

    private var lastFetchTime = 0L

    fun fetchWeather(context: Context) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("LATITUDE", 0f).toDouble()
        val lon = prefs.getFloat("LONGITUDE", 0f).toDouble()

        if (lat == 0.0 && lon == 0.0) return

        val currentTime = System.currentTimeMillis()
        if (weatherData.value != null && (currentTime - lastFetchTime) < 3600000) return

        isLoading.value = true
        apiError.value = null
        viewModelScope.launch {
            try {
                val response = WeatherClient.apiService.getWeatherForecast(lat, lon)
                if (response.isSuccessful) {
                    weatherData.value = response.body()
                    lastFetchTime = currentTime
                } else {
                    apiError.value = "API Error ${response.code()}: Key may need 2 hours to activate."
                }
            } catch (e: Exception) {
                apiError.value = "Network Error. Please check connection."
            } finally {
                isLoading.value = false
            }
        }
    }
}