package com.simats.agronova.service

import com.simats.agronova.model.WeatherResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        // 👇 PASTE YOUR OPENWEATHER API KEY BELOW 👇
        @Query("appid") apiKey: String = "cd6ce0ab5bb11f656571a6d3af1263c3"
    ): Response<WeatherResponse>
}

object WeatherClient {
    val apiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}