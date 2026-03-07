package com.simats.agronova

import android.Manifest
import kotlinx.coroutines.tasks.await
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.simats.agronova.map.MapActivity
import com.simats.agronova.ui.theme.*
import com.simats.agronova.user.AgroBottomNav
import com.simats.agronova.user.NavScreen
import com.simats.agronova.viewmodel.HomeViewModel
import com.simats.agronova.viewmodel.CropCalendarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                HomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Inject ViewModels
    val homeViewModel: HomeViewModel = viewModel()
    val cropCalendarViewModel: CropCalendarViewModel = viewModel()

    val userName = sharedPrefs.getString("USER_NAME", "Farmer") ?: "Farmer"
    val userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: ""
    var savedLocation by remember { mutableStateOf(sharedPrefs.getString("FARM_LOCATION", "Tap to set location") ?: "Tap to set location") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    // Fetch live weather & calendar when location changes
    LaunchedEffect(savedLocation) {
        homeViewModel.fetchWeather(context)
        cropCalendarViewModel.fetchCalendars(context)
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> listOf("Bright morning,", "Early bird,", "Good morning,").random()
            in 12..16 -> listOf("Sunny afternoon,", "Great day,", "Good afternoon,").random()
            in 17..20 -> listOf("Golden sunset,", "Relaxing evening,", "Good evening,").random()
            else -> listOf("Marvelous night,", "Starry night,", "Good night,").random()
        }
    }

    val mapLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val locString = data?.getStringExtra("LOCATION_STRING") ?: "Unknown Location"
            val lat = data?.getDoubleExtra("LATITUDE", 0.0) ?: 0.0
            val lon = data?.getDoubleExtra("LONGITUDE", 0.0) ?: 0.0

            savedLocation = locString
            sharedPrefs.edit()
                .putString("FARM_LOCATION", locString)
                .putFloat("LATITUDE", lat.toFloat())
                .putFloat("LONGITUDE", lon.toFloat())
                .apply()

            if (userEmail.isNotEmpty()) {
                coroutineScope.launch {
                    try {
                        com.simats.agronova.service.RetrofitClient.apiService.updateLocation(
                            com.simats.agronova.model.UpdateLocationRequest(userEmail, lat, lon, locString)
                        )
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            isFetchingLocation = true

            coroutineScope.launch {
                val minimumWaitJob = launch { delay(2000) }
                var addressString = "Location not found"
                var finalLat = 0f
                var finalLon = 0f
                var success = false

                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        finalLat = location.latitude.toFloat()
                        finalLon = location.longitude.toFloat()
                        addressString = withContext(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown City"
                                    val state = addresses[0].adminArea ?: "Unknown State"
                                    "$city, $state"
                                } else {
                                    "Lat: $finalLat, Lng: $finalLon"
                                }
                            } catch (e: Exception) {
                                "Lat: $finalLat, Lng: $finalLon"
                            }
                        }
                        success = true
                    }
                } catch (e: Exception) { success = false }

                minimumWaitJob.join()

                if (success) {
                    savedLocation = addressString
                    sharedPrefs.edit()
                        .putString("FARM_LOCATION", addressString)
                        .putFloat("LATITUDE", finalLat)
                        .putFloat("LONGITUDE", finalLon)
                        .apply()

                    if (userEmail.isNotEmpty()) {
                        try {
                            com.simats.agronova.service.RetrofitClient.apiService.updateLocation(
                                com.simats.agronova.model.UpdateLocationRequest(userEmail, finalLat.toDouble(), finalLon.toDouble(), addressString)
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    Toast.makeText(context, "Location Grabbed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please turn on GPS and try again.", Toast.LENGTH_LONG).show()
                }
                isFetchingLocation = false
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Set Farm Location", fontWeight = FontWeight.Bold) },
            text = { Text("How would you like to set your location for accurate local market prices and weather?") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    } else {
                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }) { Text("Current GPS", color = AgroGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    mapLauncher.launch(Intent(context, MapActivity::class.java))
                }) { Text("Pick from Map", color = AgroGreen) }
            }
        )
    }

    if (isFetchingLocation) {
        Dialog(
            onDismissRequest = { /* Force wait */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulseAnimation()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Acquiring Satellites...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            AgroBottomNav(
                currentScreen = NavScreen.Home,
                onItemSelected = { screen ->
                    when (screen) {
                        NavScreen.Home -> {}
                        NavScreen.Assistant -> {
                            context.startActivity(Intent(context, AssistantActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        NavScreen.Tools -> {
                            context.startActivity(Intent(context, ToolsActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        NavScreen.Profile -> {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    }
                }
            )
        },
        containerColor = AgroBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(text = greeting, fontSize = 18.sp, color = Color.Gray, maxLines = 1)
                    Text(text = "$userName 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AgroGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showLocationDialog = true }.padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Location", tint = AgroGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = savedLocation, fontSize = 14.sp, color = AgroTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                    }
                }

                IconButton(
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                        (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
                    modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weather Card
            GoogleWeatherCard(homeViewModel, cropCalendarViewModel) {
                context.startActivity(Intent(context, WeatherActivity::class.java))
                (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(modifier = Modifier.weight(1f), title = "Disease Scanner", subtitle = "Instant Diagnosis", icon = Icons.Filled.CameraAlt, iconColor = Color(0xFFFF8A65), onClick = { context.startActivity(Intent(context, DiseaseScannerActivity::class.java)) })
                ActionCard(modifier = Modifier.weight(1f), title = "Chemical Translator", subtitle = "Safety Insights", icon = Icons.Filled.Science, iconColor = Color(0xFF64B5F6), onClick = { context.startActivity(Intent(context, ChemicalTranslatorActivity::class.java)) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(modifier = Modifier.weight(1f), title = "Crop Calendar", subtitle = "Plan Season", icon = Icons.Filled.CalendarMonth, iconColor = Color(0xFFBA68C8), onClick = { context.startActivity(Intent(context, CropCalendarActivity::class.java)) })
                ActionCard(modifier = Modifier.weight(1f), title = "Resource Hub", subtitle = "Local Marketplace", icon = Icons.Filled.Handshake, iconColor = Color(0xFF4DB6AC), onClick = { context.startActivity(Intent(context, ResourceHubActivity::class.java)) })
            }

            Spacer(modifier = Modifier.height(32.dp))

            // NEW: Clean, click-to-open Teaser Card for Market Intelligence
            Text("Market Intelligence", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 3.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(context, MarketIntelligenceActivity::class.java))
                    (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFE8F5E9), shape = CircleShape) {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = AgroGreen, modifier = Modifier.padding(12.dp).size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("AI Price Prediction", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap to view today's market trends and crop sowing advice.", fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "View", tint = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun GoogleWeatherCard(homeViewModel: HomeViewModel, calendarViewModel: CropCalendarViewModel, onWeatherClick: () -> Unit) {
    val weather by homeViewModel.weatherData
    val isLoading by homeViewModel.isLoading
    val apiError by homeViewModel.apiError

    val pendingTask = calendarViewModel.calendars.value.flatMap { it.tasks }.firstOrNull { !it.isCompleted }

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable { onWeatherClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(AgroGreen, Color(0xFF1B5E20)))
            ).padding(20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp))
            } else if (apiError != null) {
                Text(apiError!!, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            } else if (weather != null) {
                val current = weather!!.list.firstOrNull()
                val temp = current?.main?.temp?.toInt()?.toString() ?: "--"
                val desc = current?.weather?.firstOrNull()?.main ?: "Clear"
                val humidity = current?.main?.humidity?.toString() ?: "--"

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(text = "$temp°", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = getWeatherIcon(desc),
                                contentDescription = desc,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(40.dp).padding(top = 16.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = desc, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Text(text = "Humidity $humidity%", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(weather!!.list.take(6)) { item ->
                            val time = formatUnixTime(item.dt, weather!!.city.timezone)
                            val itemTemp = item.main.temp.toInt().toString()
                            val itemDesc = item.weather.firstOrNull()?.main ?: "Clear"

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$itemTemp°", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Icon(
                                    imageVector = getWeatherIcon(itemDesc),
                                    contentDescription = itemDesc,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = time, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    if (pendingTask != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(color = Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = "Alert", tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Task due: ${pendingTask.title}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            } else {
                Text("Tap location pin to refresh weather.", color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp))
            }
        }
    }
}

fun formatUnixTime(dt: Long, timezoneOffset: Int): String {
    val date = java.util.Date((dt + timezoneOffset) * 1000L)
    val sdf = java.text.SimpleDateFormat("h a", Locale.getDefault())
    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}

fun getWeatherIcon(desc: String): ImageVector {
    return when (desc.lowercase()) {
        "clear" -> Icons.Filled.WbSunny
        "clouds" -> Icons.Filled.Cloud
        "rain", "drizzle" -> Icons.Filled.Umbrella
        "thunderstorm" -> Icons.Filled.FlashOn
        else -> Icons.Filled.WbCloudy
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 4f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_scale")
    val alpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "pulse_alpha")
    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(80.dp).scale(scale).background(AgroGreen.copy(alpha = alpha), CircleShape))
        Box(modifier = Modifier.size(80.dp).background(AgroGreen, CircleShape).shadow(12.dp, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.LocationSearching, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp)) }
    }
}

@Composable
fun ActionCard(modifier: Modifier = Modifier, title: String, subtitle: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = modifier.height(140.dp).clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(50.dp).background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(26.dp)) }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AgroTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() { AgronovaTheme { HomeScreen() } }