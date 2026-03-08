package com.simats.agronova.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.service.AuthRepository
import kotlinx.coroutines.launch

// Represents the different states of our API call
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    // Added fields to hold user data
    data class Success(val message: String, val name: String? = null, val locationString: String? = null, val lat: Double? = null, val lon: Double? = null, val language: String? = null) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    fun signup(name: String, email: String, phone: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.signup(name, email, phone, pass)
                if (response.isSuccessful) {
                    _authState.value = AuthState.Success("Signup Successful!")
                } else {
                    // Extracting the error message from the backend if possible
                    _authState.value = AuthState.Error("Signup Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network Error: ${e.message}")
            }
        }
    }

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = repository.login(email, pass)
                if (response.isSuccessful) {
                    val body = response.body()
                    _authState.value = AuthState.Success(
                        message = "Login Successful!",
                        name = body?.name,
                        locationString = body?.location_string,
                        lat = body?.latitude,
                        lon = body?.longitude,
                        language = body?.preferred_language
                    )
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Network Error: ${e.message}")
            }
        }
    }

    // Reset state after showing a toast
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}