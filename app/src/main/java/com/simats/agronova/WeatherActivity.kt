package com.simats.agronova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                WeatherScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = viewModel()) {
    val context = LocalContext.current
    val weather by viewModel.weatherData
    val isLoading by viewModel.isLoading
    val apiError by viewModel.apiError

    LaunchedEffect(Unit) {
        viewModel.fetchFullWeather(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(weather?.city?.name ?: "Weather", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AgroGreen // Replaced Blue with AgroGreen
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Beautiful Premium AgroGreen to Deep Forest Green Gradient
                .background(Brush.verticalGradient(listOf(AgroGreen, Color(0xFF1B5E20), Color(0xFF0D3B10))))
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else if (apiError != null) {
                // Shows the error if the API key is still pending
                Text(text = apiError!!, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            } else if (weather != null) {
                val current = weather!!.list.first()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. BIG HEADER (Current Temp)
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 40.dp)) {
                            Text("Now", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${current.main.temp.toInt()}°", fontSize = 80.sp, fontWeight = FontWeight.Light, color = Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(imageVector = getWeatherIconUI(current.weather.first().main), contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(60.dp))
                            }
                            Text(current.weather.first().description.replaceFirstChar { it.uppercase() }, fontSize = 20.sp, color = Color.White)
                            Text("High: ${current.main.temp_max.toInt()}° • Low: ${current.main.temp_min.toInt()}°", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    // 2. HOURLY FORECAST
                    item {
                        WeatherSurfaceCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Hourly forecast", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    items(weather!!.list.take(8)) { item ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${item.main.temp.toInt()}°", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Icon(getWeatherIconUI(item.weather.first().main), contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(formatTimeOnly(item.dt, weather!!.city.timezone), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. MULTI-DAY FORECAST
                    item {
                        WeatherSurfaceCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("5-day forecast", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                                val dailyMap = weather!!.list.groupBy { formatDayOnly(it.dt, weather!!.city.timezone) }

                                dailyMap.forEach { (dayString, items) ->
                                    val maxTemp = items.maxOf { it.main.temp_max }.toInt()
                                    val minTemp = items.minOf { it.main.temp_min }.toInt()
                                    val desc = items[items.size / 2].weather.first().main

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dayString, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        Icon(getWeatherIconUI(desc), contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(24.dp))
                                        Text("$maxTemp° / $minTemp°", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                    }
                                    Divider(color = Color.White.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }

                    // 4. CURRENT CONDITIONS GRID (2x2)
                    item {
                        Text("Current conditions", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // WIND
                            WeatherSurfaceCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Wind", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${(current.wind.speed * 3.6).toInt()}", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Light)
                                    Text("km/h", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    WindCompassDial(current.wind.deg.toFloat())
                                }
                            }

                            // HUMIDITY
                            WeatherSurfaceCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Humidity", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${current.main.humidity}%", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Light)
                                    Text("Dew point 16°", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HumidityBar(current.main.humidity)
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // PRESSURE
                            WeatherSurfaceCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Pressure", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${current.main.pressure}", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Light)
                                    Text("mBar", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PressureArcDial()
                                }
                            }

                            // UV INDEX
                            WeatherSurfaceCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("UV index", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("8", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Light)
                                    Text("Very high", color = Color(0xFFEF5350), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 5. SUNRISE / SUNSET
                    item {
                        WeatherSurfaceCard {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Sunrise & sunset", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Sunrise", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                        Text(formatTimeOnly(weather!!.city.sunrise, weather!!.city.timezone), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Sunset", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                        Text(formatTimeOnly(weather!!.city.sunset, weather!!.city.timezone), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                SunriseSunsetArc()
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// --- CUSTOM UI COMPONENTS ---

@Composable
fun WeatherSurfaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        // Changed from Dark Blue glass to bright translucent glass to fit the Green Theme
        color = Color.White.copy(alpha = 0.15f),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun WindCompassDial(degrees: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(50.dp)) {
            drawCircle(color = Color.White.copy(alpha = 0.2f), radius = size.width / 2)
            val angleRad = (degrees - 90) * (Math.PI / 180)
            val arrowX = (size.width / 2) + (size.width / 2.5f) * cos(angleRad).toFloat()
            val arrowY = (size.height / 2) + (size.height / 2.5f) * sin(angleRad).toFloat()
            drawCircle(color = Color(0xFF64B5F6), radius = 6.dp.toPx(), center = Offset(arrowX, arrowY))
            drawLine(color = Color(0xFF64B5F6), start = center, end = Offset(arrowX, arrowY), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HumidityBar(percentage: Int) {
    Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
        Box(modifier = Modifier.fillMaxWidth(percentage / 100f).fillMaxHeight().background(Color(0xFF64B5F6), CircleShape))
    }
}

@Composable
fun PressureArcDial() {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(50.dp)) {
            drawArc(color = Color.White.copy(alpha = 0.2f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = Color(0xFF81C784), startAngle = 135f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun SunriseSunsetArc() {
    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pathWidth = size.width
            val pathHeight = size.height
            drawLine(color = Color.White.copy(alpha = 0.3f), start = Offset(0f, pathHeight), end = Offset(pathWidth, pathHeight), strokeWidth = 2.dp.toPx())
            drawArc(
                color = Color(0xFFFFD54F),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(pathWidth * 0.1f, pathHeight * 0.2f),
                size = Size(pathWidth * 0.8f, pathHeight * 1.6f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawCircle(color = Color(0xFFFFD54F), radius = 8.dp.toPx(), center = Offset(pathWidth * 0.7f, pathHeight * 0.5f))
        }
    }
}

fun formatTimeOnly(dt: Long, timezoneOffset: Int): String {
    val date = Date((dt + timezoneOffset) * 1000L)
    val sdf = SimpleDateFormat("h a", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}

fun formatDayOnly(dt: Long, timezoneOffset: Int): String {
    val date = Date((dt + timezoneOffset) * 1000L)
    val sdf = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}

fun getWeatherIconUI(desc: String): ImageVector {
    return when (desc.lowercase()) {
        "clear" -> Icons.Filled.WbSunny
        "clouds" -> Icons.Filled.Cloud
        "rain", "drizzle" -> Icons.Filled.Umbrella
        "thunderstorm" -> Icons.Filled.FlashOn
        else -> Icons.Filled.WbCloudy
    }
}