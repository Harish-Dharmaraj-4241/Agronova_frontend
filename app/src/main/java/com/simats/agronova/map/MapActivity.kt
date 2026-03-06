package com.simats.agronova.map

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid requires this configuration to load map tiles
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            AgronovaTheme {
                MapScreen()
            }
        }
    }
}

@Composable
fun MapScreen() {
    val context = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isConfirming by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Embed the OSMDroid MapView
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    // Default center (Set to a central coordinate like India)
                    controller.setCenter(GeoPoint(20.5937, 78.9629))
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Crosshair
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Target", tint = Color.Red, modifier = Modifier.size(24.dp))
        }

        // Instructions
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Text("Drag map to pin your farm", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = AgroGreen)
        }

        // Confirm Button
        Button(
            onClick = {
                val centerMap = mapView?.mapCenter
                if (centerMap != null) {
                    isConfirming = true
                    coroutineScope.launch {
                        val lat = centerMap.latitude
                        val lon = centerMap.longitude

                        val addressString = withContext(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(lat, lon, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown City"
                                    val state = addresses[0].adminArea ?: ""
                                    if (state.isNotEmpty()) "$city, $state" else city
                                } else {
                                    "Lat: ${lat.toFloat()}, Lng: ${lon.toFloat()}"
                                }
                            } catch (e: Exception) {
                                "Lat: ${lat.toFloat()}, Lng: ${lon.toFloat()}"
                            }
                        }

                        // Return the data to HomeActivity
                        val resultIntent = Intent().apply {
                            putExtra("LOCATION_STRING", addressString)
                            putExtra("LATITUDE", lat)
                            putExtra("LONGITUDE", lon)
                        }
                        context.setResult(Activity.RESULT_OK, resultIntent)
                        context.finish()
                    }
                } else {
                    Toast.makeText(context, "Map not ready yet", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
        ) {
            if (isConfirming) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Confirm Location", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}