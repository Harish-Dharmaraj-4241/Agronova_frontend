package com.simats.agronova.service

import com.simats.agronova.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("signup")
    suspend fun signupUser(
        @Body request: SignupRequest
    ): Response<GenericResponse>

    @POST("ask")
    suspend fun askAssistant(
        @Body request: AskRequest
    ): Response<AskResponse>

    @POST("update-location")
    suspend fun updateLocation(
        @Body request: UpdateLocationRequest
    ): Response<GenericResponse>
}