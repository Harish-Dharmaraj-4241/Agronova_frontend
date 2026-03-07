package com.simats.agronova.service

import com.simats.agronova.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
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

    @POST("scan-disease")
    suspend fun scanDisease(
        @Body request: DiseaseScanRequest
    ): Response<DiseaseScanResponse>

    @POST("translate-chemical")
    suspend fun translateChemical(
        @Body request: ChemicalScanRequest
    ): Response<ChemicalScanResponse>

    @POST("generate-calendar")
    suspend fun generateCropCalendar(
        @Body request: GenerateCalendarRequest
    ): Response<CropCalendarResponse>

    @GET("get-calendar")
    suspend fun getCropCalendar(
        @retrofit2.http.Query("email") email: String
    ): Response<List<CropCalendarResponse>>

    @POST("mark-task")
    suspend fun markTaskComplete(
        @Body request: MarkTaskRequest
    ): Response<GenericResponse>

    @GET("market-prediction")
    suspend fun getMarketPrediction(
        @retrofit2.http.Query("location") location: String,
        @retrofit2.http.Query("language") language: String
    ): Response<MarketPredictionResponse>

    @POST("post-resource")
    suspend fun postResource(
        @Body request: PostResourceRequest
    ): Response<GenericResponse>

    @GET("get-resources")
    suspend fun getResources(
        @retrofit2.http.Query("lat") lat: Double,
        @retrofit2.http.Query("lon") lon: Double
    ): Response<List<ResourceItem>>

    @POST("edit-resource")
    suspend fun editResource(
        @Body request: EditResourceRequest
    ): Response<GenericResponse>

    @POST("delete-resource")
    suspend fun deleteResource(
        @Body request: DeleteResourceRequest
    ): Response<GenericResponse>

    @POST("calculate-farm-inputs")
    suspend fun calculateFarmInputs(
        @Body request: CalculateInputsRequest
    ): Response<CalculateInputsResponse>

    @GET("agri-news")
    suspend fun getAgriNews(
        @retrofit2.http.Query("language") language: String
    ): Response<List<AgriNewsItem>>

    @GET("get-profile")
    suspend fun getProfile(
        @retrofit2.http.Query("email") email: String
    ): Response<UserProfileResponse>

    @POST("update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<GenericResponse>

    @POST("verify-password")
    suspend fun verifyPassword(@Body request: VerifyPasswordRequest): Response<GenericResponse>

    @POST("change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<GenericResponse>

    @POST("delete-account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<GenericResponse>
}