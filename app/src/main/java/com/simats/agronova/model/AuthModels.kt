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
    val longitude: Double?,
    val preferred_language: String? // NEW
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

data class DiseaseScanRequest(val imageBase64: String, val language: String)
data class DiseaseScanResponse(
    val diseaseName: String,
    val confidence: String,
    val description: String,
    val treatmentSteps: List<String>
)

data class ChemicalScanRequest(val imageBase64: String, val language: String)

data class ChemicalScanResponse(
    val chemicalName: String,
    val toxicityLevel: String, // "Organic", "Low", "Medium", "High"
    val purpose: String,
    val dosage: String,
    val timing: String,
    val safetyWarnings: List<String>
)

// --- CROP CALENDAR MODELS ---

data class GenerateCalendarRequest(
    val email: String,
    val cropName: String,
    val sowingDate: String // Format: YYYY-MM-DD
)

data class TaskResponse(
    val id: Int,
    val cropId: Int,
    val day: Int,
    val date: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)

data class CropCalendarResponse(
    val cropId: Int,
    val cropName: String,
    val sowingDate: String,
    val harvestDate: String,
    val tasks: List<TaskResponse>
)

data class MarkTaskRequest(val taskId: Int)

// --- LOCAL RESOURCE HUB MODELS ---

// --- LOCAL RESOURCE HUB MODELS ---

data class ResourceItem(
    val id: Int,
    val userEmail: String, // NEW: To identify who posted it
    val ownerName: String,
    val ownerPhone: String,
    val itemName: String,
    val category: String,
    val isAvailable: Boolean, // NEW: Out of stock toggle
    val distanceKm: Double
)

data class PostResourceRequest(
    val email: String,
    val ownerName: String,
    val ownerPhone: String,
    val itemName: String,
    val category: String,
    val latitude: Double,
    val longitude: Double
)

// NEW: Requests for managing your own posts
data class EditResourceRequest(val id: Int, val itemName: String, val category: String, val isAvailable: Boolean)
data class DeleteResourceRequest(val id: Int)

// --- MARKET PREDICTION MODELS ---

data class CropTrend(
    val cropName: String,
    val emoji: String,
    val trend: String, // "UP" or "DOWN"
    val percentage: String,
    val reason: String
)

data class MarketPredictionData(
    val summary: String,
    val trends: List<CropTrend>
)

data class MarketPredictionResponse(
    val data: MarketPredictionData?,
    val error: String? // Catches the QUOTA_EXHAUSTED error
)

// --- TOOLS & HUB MODELS ---
data class CalculateInputsRequest(
    val cropName: String,
    val landSize: String,
    val unit: String,
    val language: String
)

data class FarmInputsData(
    val seedRequirement: String,
    val fertilizerRequirement: String,
    val proTip: String
)

data class CalculateInputsResponse(
    val data: FarmInputsData?,
    val error: String? // Handles the Quota error gracefully
)

data class AgriNewsItem(
    val title: String,
    val source: String,
    val link: String,
    val pubDate: String
)

// --- PROFILE MODELS ---
data class UserProfileResponse(
    val name: String,
    val username: String?,
    val email: String,
    val phone: String,
    val profileImageBase64: String?
)

data class UpdateProfileRequest(
    val email: String, // Used to identify the user (Read-Only)
    val name: String,
    val username: String,
    val phone: String,
    val profileImageBase64: String? // Send new image, or null if unchanged
)

// --- SETTINGS MODELS ---
data class VerifyPasswordRequest(val email: String, val currentPassword: String)
data class ChangePasswordRequest(val email: String, val newPassword: String)
data class DeleteAccountRequest(val email: String)
data class UpdateLanguageRequest(val email: String, val language: String)