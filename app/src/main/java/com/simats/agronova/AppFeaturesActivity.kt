package com.simats.agronova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme

class AppFeaturesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme { FeaturesScreen(onBack = { finish() }) }
        }
    }
}

data class Feature(val title: String, val icon: ImageVector, val desc: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    val features = listOf(
        Feature("Smart AI Assistant", Icons.Filled.SmartToy, "Chat with AgroNova AI in your regional language. Ask about weather, soil health, and farming tips!"),
        Feature("Crop Disease Scanner", Icons.Filled.DocumentScanner, "Upload a photo of a sick plant, and the AI will identify the disease and provide treatment steps."),
        Feature("Market Intelligence", Icons.Filled.TrendingUp, "Get daily, AI-powered predictions on local crop prices and market trends."),
        Feature("Smart Tools & Calculators", Icons.Filled.Science, "Calculate exact seed requirements and NPK fertilizer mixtures based on your farm size."),
        Feature("Local Resource Hub", Icons.Filled.Handshake, "Rent or buy tractors, fertilizers, and tools from nearby farmers.")
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Features", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) },
        containerColor = AgroBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 10.dp)) {
            items(features) { feature ->
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(feature.icon, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(feature.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Color.Gray)
                        }
                        AnimatedVisibility(visible = expanded) {
                            Text(feature.desc, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp, start = 44.dp))
                        }
                    }
                }
            }
        }
    }
}