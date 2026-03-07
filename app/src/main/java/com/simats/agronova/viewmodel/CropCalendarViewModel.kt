package com.simats.agronova.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.CropCalendarResponse
import com.simats.agronova.model.GenerateCalendarRequest
import com.simats.agronova.model.MarkTaskRequest
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class CropCalendarViewModel : ViewModel() {
    val calendars = mutableStateOf<List<CropCalendarResponse>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    fun fetchCalendars(context: Context) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getCropCalendar(email)
                if (response.isSuccessful && response.body() != null) {
                    calendars.value = response.body()!!
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to load calendars."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun generateCalendar(context: Context, cropName: String, sowingDate: String) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("USER_EMAIL", "") ?: ""
        if (email.isEmpty()) return

        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                val request = GenerateCalendarRequest(email, cropName, sowingDate)
                val response = RetrofitClient.apiService.generateCropCalendar(request)
                if (response.isSuccessful && response.body() != null) {
                    // Refresh the list after generating
                    fetchCalendars(context)
                } else {
                    errorMessage.value = "Failed to generate AI schedule."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network Error. Please try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun markTaskCompleted(context: Context, taskId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.markTaskComplete(MarkTaskRequest(taskId))
                if (response.isSuccessful) {
                    fetchCalendars(context) // Refresh the UI to show it as completed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}