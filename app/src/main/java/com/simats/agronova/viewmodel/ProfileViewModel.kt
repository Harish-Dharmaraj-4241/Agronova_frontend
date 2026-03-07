package com.simats.agronova.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.UpdateProfileRequest
import com.simats.agronova.model.UserProfileResponse
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    val isLoading = mutableStateOf(false)
    val successMessage = mutableStateOf<String?>(null)
    val errorMessage = mutableStateOf<String?>(null)

    val name = mutableStateOf("")
    val username = mutableStateOf("")
    val email = mutableStateOf("")
    val phone = mutableStateOf("")
    val profileImageBase64 = mutableStateOf<String?>(null)

    // Only stores new image if user changes it
    var pendingImageUploadBase64: String? = null

    fun fetchProfile(userEmail: String) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProfile(userEmail)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    name.value = data.name
                    username.value = data.username ?: ""
                    email.value = data.email
                    phone.value = data.phone
                    profileImageBase64.value = data.profileImageBase64
                }
            } catch (e: Exception) {
                errorMessage.value = "Failed to load profile."
            } finally {
                isLoading.value = false
            }
        }
    }

    fun saveProfile() {
        if (!isNameValid() || !isUsernameValid() || !isPhoneValid()) {
            errorMessage.value = "Please fix the errors in the form."
            return
        }

        isLoading.value = true
        errorMessage.value = null
        successMessage.value = null

        viewModelScope.launch {
            try {
                val request = UpdateProfileRequest(
                    email = email.value, // Read only, used as ID
                    name = name.value,
                    username = username.value,
                    phone = phone.value,
                    profileImageBase64 = pendingImageUploadBase64
                )
                val response = RetrofitClient.apiService.updateProfile(request)

                if (response.isSuccessful) {
                    successMessage.value = "Profile updated successfully!"
                    pendingImageUploadBase64 = null // Clear pending
                } else {
                    errorMessage.value = "Failed to update profile."
                }
            } catch (e: Exception) {
                errorMessage.value = "Network error."
            } finally {
                isLoading.value = false
            }
        }
    }

    // STRICT LIVE VALIDATION RULES
    fun isNameValid(): Boolean {
        // Only letters, absolutely no spaces
        return name.value.isNotEmpty() && name.value.matches(Regex("^[a-zA-Z]+$"))
    }

    fun isUsernameValid(): Boolean {
        // Anything allowed EXCEPT spaces
        return username.value.isNotEmpty() && username.value.matches(Regex("^\\S+$"))
    }

    fun isPhoneValid(): Boolean {
        // Exactly 10 digits
        return phone.value.isNotEmpty() && phone.value.matches(Regex("^\\d{10}$"))
    }
}