package com.simats.agronova.service

import com.simats.agronova.model.AskRequest
import com.simats.agronova.model.LoginRequest
import com.simats.agronova.model.SignupRequest

class AuthRepository {

    suspend fun login(email: String, password: String) =
        RetrofitClient.apiService.loginUser(
            LoginRequest(email, password)
        )

    suspend fun signup(name: String, email: String, phone: String, password: String) =
        RetrofitClient.apiService.signupUser(
            SignupRequest(name, email, phone, password)
        )
    suspend fun askAssistant(message: String, language: String, location: String?, imageBase64: String? = null) =
        RetrofitClient.apiService.askAssistant(
            AskRequest(message, language, location, imageBase64)
        )
}