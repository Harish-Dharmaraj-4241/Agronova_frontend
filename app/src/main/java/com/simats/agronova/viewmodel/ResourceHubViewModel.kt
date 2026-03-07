package com.simats.agronova.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.DeleteResourceRequest
import com.simats.agronova.model.EditResourceRequest
import com.simats.agronova.model.PostResourceRequest
import com.simats.agronova.model.ResourceItem
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class ResourceHubViewModel : ViewModel() {
    val resources = mutableStateOf<List<ResourceItem>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    var currentUserEmail = "" // NEW: Track the user to manage their posts

    fun fetchLocalResources(context: Context) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        currentUserEmail = prefs.getString("USER_EMAIL", "") ?: ""
        val lat = prefs.getFloat("LATITUDE", 0f).toDouble()
        val lon = prefs.getFloat("LONGITUDE", 0f).toDouble()

        if (lat == 0.0 && lon == 0.0) {
            errorMessage.value = "Please set your location on the Home page first."
            return
        }

        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getResources(lat, lon)
                if (response.isSuccessful && response.body() != null) {
                    resources.value = response.body()!!
                } else {
                    errorMessage.value = "Could not load marketplace."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network error while loading resources."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun postResource(context: Context, itemName: String, category: String, onSuccess: () -> Unit) {
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("USER_EMAIL", "") ?: return
        val name = prefs.getString("USER_NAME", "Farmer") ?: "Farmer"
        val phone = prefs.getString("USER_PHONE", "Not Provided") ?: "Not Provided"
        val lat = prefs.getFloat("LATITUDE", 0f).toDouble()
        val lon = prefs.getFloat("LONGITUDE", 0f).toDouble()

        viewModelScope.launch {
            try {
                val request = PostResourceRequest(email, name, phone, itemName, category, lat, lon)
                val response = RetrofitClient.apiService.postResource(request)
                if (response.isSuccessful) {
                    onSuccess()
                    fetchLocalResources(context) // Refresh the list!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editResource(context: Context, id: Int, itemName: String, category: String, isAvailable: Boolean) {
        viewModelScope.launch {
            try {
                val request = EditResourceRequest(id, itemName, category, isAvailable)
                val response = RetrofitClient.apiService.editResource(request)
                if (response.isSuccessful) {
                    fetchLocalResources(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteResource(context: Context, id: Int) {
        viewModelScope.launch {
            try {
                val request = DeleteResourceRequest(id)
                val response = RetrofitClient.apiService.deleteResource(request)
                if (response.isSuccessful) {
                    fetchLocalResources(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}