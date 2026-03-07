package com.simats.agronova.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.ChangePasswordRequest
import com.simats.agronova.model.DeleteAccountRequest
import com.simats.agronova.model.VerifyPasswordRequest
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val successMessage = mutableStateOf<String?>(null)
    val isPasswordVerified = mutableStateOf(false)
    val accountDeleted = mutableStateOf(false)

    fun verifyCurrentPassword(email: String, currentPass: String) {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.verifyPassword(VerifyPasswordRequest(email, currentPass))
                if (response.isSuccessful) {
                    isPasswordVerified.value = true
                } else {
                    errorMessage.value = "Incorrect current password."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network Error."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updatePassword(email: String, newPass: String) {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.changePassword(ChangePasswordRequest(email, newPass))
                if (response.isSuccessful) {
                    successMessage.value = "Password Updated Successfully!"
                } else {
                    errorMessage.value = "Failed to update password."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network Error."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteAccount(email: String) {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteAccount(DeleteAccountRequest(email))
                if (response.isSuccessful) {
                    accountDeleted.value = true
                } else {
                    errorMessage.value = "Failed to delete account."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network Error."
            } finally {
                isLoading.value = false
            }
        }
    }
}