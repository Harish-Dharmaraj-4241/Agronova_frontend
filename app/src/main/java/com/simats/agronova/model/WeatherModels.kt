package com.simats.agronova.model

data class WeatherResponse(
    val list: List<WeatherItem>,
    val city: City
)

data class WeatherItem(
    val dt: Long,
    val main: MainTemp,
    val weather: List<WeatherDesc>,
    val wind: Wind
)

data class MainTemp(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Int,
    val pressure: Int
)

data class WeatherDesc(
    val main: String,
    val description: String
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class City(
    val name: String,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)