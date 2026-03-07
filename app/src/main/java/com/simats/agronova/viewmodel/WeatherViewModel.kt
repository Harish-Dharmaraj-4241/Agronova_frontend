package com.simats.agronova.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.WeatherResponse
import com.simats.agronova.service.WeatherClient
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    val weatherData = mutableStateOf<WeatherResponse?>(null)
    val isLoading = mutableStateOf(false)
    val apiError = mutableStateOf<String?>(null)

    fun fetchFullWeather(context: Context) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("LATITUDE", 0f).toDouble()
        val lon = prefs.getFloat("LONGITUDE", 0f).toDouble()

        if (lat == 0.0 && lon == 0.0) return

        isLoading.value = true
        apiError.value = null
        viewModelScope.launch {
            try {
                val response = WeatherClient.apiService.getWeatherForecast(lat, lon)
                if (response.isSuccessful) {
                    weatherData.value = response.body()
                } else {
                    apiError.value = "API Key is still activating. Try again in an hour!"
                }
            } catch (e: Exception) {
                apiError.value = "Network connection failed."
            } finally {
                isLoading.value = false
            }
        }
    }
}