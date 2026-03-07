package com.simats.agronova.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.MarketPredictionData
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class MarketIntelligenceViewModel : ViewModel() {
    val marketPrediction = mutableStateOf<MarketPredictionData?>(null)
    val errorMessage = mutableStateOf<String?>(null)
    val isPredictionLoading = mutableStateOf(false)

    fun fetchMarketPrediction(context: Context, language: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && marketPrediction.value != null) return

        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val location = prefs.getString("FARM_LOCATION", "Tap to set location") ?: "Tap to set location"

        if (location == "Tap to set location" || location.isEmpty()) {
            errorMessage.value = "Please go back to the Home Page and set your location first to get accurate predictions."
            return
        }

        isPredictionLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMarketPrediction(location, language)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Smart Quota Handling
                    if (body.error == "QUOTA_EXHAUSTED") {
                        errorMessage.value = "Today's AI prediction quota has been completely used. Please check back tomorrow for fresh market insights!"
                    } else if (body.error != null) {
                        errorMessage.value = body.error
                    } else if (body.data != null) {
                        marketPrediction.value = body.data
                    } else {
                        errorMessage.value = "Failed to fetch market insights. Please try again later."
                    }
                } else {
                    errorMessage.value = "Server Error. Please try again later."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage.value = "Network Error: Please check your internet connection."
            } finally {
                isPredictionLoading.value = false
            }
        }
    }
}