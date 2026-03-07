package com.simats.agronova.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.AgriNewsItem
import com.simats.agronova.model.CalculateInputsRequest
import com.simats.agronova.model.FarmInputsData
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class ToolsViewModel : ViewModel() {
    // News Feed State
    val newsList = mutableStateOf<List<AgriNewsItem>>(emptyList())
    val isNewsLoading = mutableStateOf(false)

    // Calculator State
    val calculatorResult = mutableStateOf<FarmInputsData?>(null)
    val isCalcLoading = mutableStateOf(false)
    val calcError = mutableStateOf<String?>(null)

    fun fetchNews(context: Context, forceRefresh: Boolean = false) {
        // If we change language, forceRefresh will be true, bypassing this check!
        if (!forceRefresh && newsList.value.isNotEmpty()) return

        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("USER_LANGUAGE", "English") ?: "English"

        isNewsLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAgriNews(language)
                if (response.isSuccessful && response.body() != null) {
                    newsList.value = response.body()!!
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { isNewsLoading.value = false }
        }
    }

    fun calculateInputs(context: Context, cropName: String, size: String, unit: String) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("USER_LANGUAGE", "English") ?: "English"

        isCalcLoading.value = true
        calcError.value = null
        calculatorResult.value = null

        viewModelScope.launch {
            try {
                val request = CalculateInputsRequest(cropName, size, unit, language)
                val response = RetrofitClient.apiService.calculateFarmInputs(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.error == "QUOTA_EXHAUSTED") {
                        calcError.value = "Today's AI limit reached. Please try again tomorrow!"
                    } else if (body.error != null) {
                        calcError.value = body.error
                    } else {
                        calculatorResult.value = body.data
                    }
                } else {
                    calcError.value = "Server error. Please try again."
                }
            } catch (e: Exception) {
                calcError.value = "Network error. Check connection."
            } finally {
                isCalcLoading.value = false
            }
        }
    }

    fun resetCalculator() {
        calculatorResult.value = null
        calcError.value = null
    }
}