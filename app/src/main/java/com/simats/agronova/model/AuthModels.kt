package com.simats.agronova.model

// Login Request
data class LoginRequest(
    val email: String,
    val password: String
)

// Signup Request (Added phone)
data class SignupRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)
// Login Response
data class LoginResponse(
    val message: String,
    val token: String?,
    val name: String?,
    val location_string: String?,
    val latitude: Double?,
    val longitude: Double?
)

// Request to save location to database
data class UpdateLocationRequest(
    val email: String,
    val latitude: Double,
    val longitude: Double,
    val location_string: String
)
// AI Assistant Requests
data class AskRequest(
    val message: String,
    val language: String,
    val location: String?, // Added location
    val imageBase64: String? = null
)

data class AskResponse(val reply: String?, val error: String?)

// Common Response
data class GenericResponse(
    val message: String
)