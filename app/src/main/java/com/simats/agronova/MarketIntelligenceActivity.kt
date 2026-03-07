package com.simats.agronova

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.model.CropTrend
import com.simats.agronova.model.MarketPredictionData
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.MarketIntelligenceViewModel
import java.util.Locale

class MarketIntelligenceActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            AgronovaTheme {
                MarketIntelligenceScreen(
                    onBack = { finish() },
                    onSpeak = { data, lang -> speakPrediction(data, lang) },
                    onStopSpeak = { tts.stop() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { isTtsReady = true }
    }

    private fun speakPrediction(data: MarketPredictionData, languageCode: String) {
        if (isTtsReady) {
            val locale = when (languageCode) {
                "Tamil" -> Locale("ta", "IN")
                "Hindi" -> Locale("hi", "IN")
                "Telugu" -> Locale("te", "IN")
                "Malayalam" -> Locale("ml", "IN")
                "Kannada" -> Locale("kn", "IN")
                else -> Locale("en", "IN")
            }

            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Voice not supported. Please download this language in your phone's TTS settings.", Toast.LENGTH_LONG).show()
            } else {
                // Construct a seamless sentence from the JSON data!
                val trendsText = data.trends.joinToString(". ") { trend ->
                    "${trend.cropName}, ${trend.trend}, ${trend.percentage}. ${trend.reason}"
                }
                val fullTextToSpeak = "${data.summary} ... $trendsText"

                val maxLength = TextToSpeech.getMaxSpeechInputLength()
                if (fullTextToSpeak.length > maxLength) {
                    val chunks = fullTextToSpeak.chunked(maxLength)
                    chunks.forEachIndexed { index, chunk ->
                        tts.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, null)
                    }
                } else {
                    tts.speak(fullTextToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        } else {
            Toast.makeText(this, "Audio engine is still loading...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::tts.isInitialized) tts.stop()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketIntelligenceScreen(
    viewModel: MarketIntelligenceViewModel = viewModel(),
    onBack: () -> Unit,
    onSpeak: (MarketPredictionData, String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }

    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("USER_LANGUAGE", "English") ?: "English") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val availableLanguages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    val prediction = viewModel.marketPrediction.value
    val errorMessage = viewModel.errorMessage.value
    val isLoading = viewModel.isPredictionLoading.value

    LaunchedEffect(Unit) {
        viewModel.fetchMarketPrediction(context, selectedLanguage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Intelligence", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(selectedLanguage, color = AgroGreen, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AgroGreen)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            availableLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = if (lang == selectedLanguage) AgroGreen else Color.Black, fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedLanguage = lang
                                        showLanguageMenu = false
                                        onStopSpeak()
                                        isSpeaking = false
                                        sharedPrefs.edit().putString("USER_LANGUAGE", lang).apply()
                                        viewModel.fetchMarketPrediction(context, lang, forceRefresh = true)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (prediction != null && !isLoading) {
                FloatingActionButton(
                    onClick = {
                        if (isSpeaking) {
                            onStopSpeak()
                            isSpeaking = false
                        } else {
                            onSpeak(prediction, selectedLanguage)
                            isSpeaking = true
                        }
                    },
                    containerColor = if (isSpeaking) Color(0xFFEF4444) else AgroGreen,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp, contentDescription = "Audio")
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AgroGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing live market data...", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (errorMessage != null) {
                // Quota Exhausted / Error State
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (errorMessage.contains("quota", ignoreCase = true)) Icons.Filled.HourglassEmpty else Icons.Filled.WifiOff, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage, fontSize = 16.sp, color = Color(0xFF334155), textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 24.sp)
                    }
                }
            } else if (prediction != null) {

                // 1. SUMMARY CARD
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFFE8F5E9), shape = CircleShape) {
                                Icon(Icons.Filled.Newspaper, contentDescription = null, tint = AgroGreen, modifier = Modifier.padding(8.dp).size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Market Overview", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(prediction.summary, fontSize = 15.sp, color = Color(0xFF334155), lineHeight = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Current Trends", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))

                // 2. TREND CARDS (Generated dynamically from JSON)
                prediction.trends.forEach { trend ->
                    TrendCard(trend)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disclaimer Box
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.background(Color(0xFFFFFBEB), RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = "Warning", tint = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Disclaimer: This is an AI generated prediction. Market prices heavily fluctuate. Do not use this as absolute financial advice.", fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 18.sp)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TrendCard(trend: CropTrend) {
    val isUp = trend.trend.uppercase() == "UP"
    val backgroundColor = if (isUp) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
    val contentColor = if (isUp) Color(0xFF059669) else Color(0xFFDC2626)
    val icon = if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown

    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            // Emoji Box
            Box(modifier = Modifier.size(50.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(text = trend.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = trend.cropName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                    // Trend Pill
                    Surface(color = backgroundColor, shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(trend.percentage, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = trend.reason, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
        }
    }
}