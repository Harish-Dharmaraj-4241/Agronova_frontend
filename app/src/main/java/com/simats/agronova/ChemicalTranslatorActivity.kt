package com.simats.agronova

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.ChemicalTranslatorViewModel
import java.util.*

class ChemicalTranslatorActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            AgronovaTheme {
                ChemicalScannerScreen(
                    onBack = { finish() },
                    onSpeak = { text, langCode -> speakResult(text, langCode) },
                    onStopSpeak = { tts.stop() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { isTtsReady = true }
    }

    private fun speakResult(text: String, languageCode: String) {
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
                Toast.makeText(this, "Voice not supported for this language.", Toast.LENGTH_SHORT).show()
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
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

enum class ChemScanState { EMPTY, LOADING, ERROR, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemicalScannerScreen(
    viewModel: ChemicalTranslatorViewModel = viewModel(),
    onBack: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // Read the global language from Settings!
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("USER_LANGUAGE", "English") ?: "English") }
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val languages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    val result by viewModel.scanResult
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            viewModel.analyzeChemicalLabel(context, uri, selectedLanguage)
        }
    }

    val currentState = when {
        isLoading -> ChemScanState.LOADING
        error != null -> ChemScanState.ERROR
        result != null -> ChemScanState.SUCCESS
        else -> ChemScanState.EMPTY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chemical Translator", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                actions = {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F8F5),
                            modifier = Modifier.clickable { expandedLanguageMenu = true }.padding(end = 12.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Language, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(selectedLanguage, color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        DropdownMenu(expanded = expandedLanguageMenu, onDismissRequest = { expandedLanguageMenu = false }) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedLanguage = lang
                                        expandedLanguageMenu = false
                                        // Save back to global settings if changed here!
                                        sharedPrefs.edit().putString("USER_LANGUAGE", lang).apply()

                                        if (selectedImageUri != null && !isLoading) {
                                            viewModel.analyzeChemicalLabel(context, selectedImageUri!!, lang)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9) // Light grayish background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Top Area: Scanner OR Blurred Image
            if (selectedImageUri != null) {
                // Glassmorphism aesthetic: Blurred background with dark overlay
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Label",
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).blur(12.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).background(Color.Black.copy(alpha = 0.4f)))
            } else {
                // Premium Scanner Empty State
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).background(Color(0xFF1E293B)).clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CropFree, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tap to scan chemical label", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Sheet Content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.BottomCenter)
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp).verticalScroll(rememberScrollState())
                ) {
                    Box(modifier = Modifier.width(48.dp).height(5.dp).background(Color(0xFFE2E8F0), CircleShape).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(24.dp))

                    Crossfade(targetState = currentState, animationSpec = tween(500), label = "ChemScanState") { state ->
                        when (state) {
                            ChemScanState.EMPTY -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Science, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Waiting for Label", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text("Snap a clear photo of the back of any pesticide or fertilizer bottle.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                                }
                            }
                            ChemScanState.LOADING -> {
                                Column(modifier = Modifier.fillMaxWidth().height(250.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(color = Color(0xFF3B82F6), strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Reading Label Data...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Text("Translating dosage and safety warnings", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                            ChemScanState.ERROR -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(60.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(error!!, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(onClick = { imagePicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)) {
                                        Text("Try Another Image", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            ChemScanState.SUCCESS -> {
                                val toxLevel = result!!.toxicityLevel.lowercase()
                                val (toxColor, toxBg, toxIcon, toxText) = when (toxLevel) {
                                    "organic" -> listOf(Color(0xFF059669), Color(0xFFD1FAE5), Icons.Filled.Eco, "Organic")
                                    "low" -> listOf(Color(0xFFD97706), Color(0xFFFEF3C7), Icons.Filled.WarningAmber, "Low Toxicity")
                                    "medium" -> listOf(Color(0xFFEA580C), Color(0xFFFFEDD5), Icons.Filled.Warning, "Medium Toxicity")
                                    "high" -> listOf(Color(0xFFDC2626), Color(0xFFFEE2E2), Icons.Filled.Dangerous, "High Toxicity")
                                    else -> listOf(Color.Gray, Color(0xFFF1F5F9), Icons.Filled.Info, "Unknown Toxicity")
                                }

                                Column {
                                    // Title & Toxicity Badge
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(result!!.chemicalName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), lineHeight = 30.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(result!!.purpose, fontSize = 14.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                                        }
                                        Surface(color = toxBg as Color, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, (toxColor as Color).copy(alpha = 0.5f))) {
                                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(toxIcon as ImageVector, contentDescription = null, tint = toxColor, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(toxText as String, color = toxColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Dosage Card (Blue)
                                    InfoCard(
                                        icon = Icons.Filled.WaterDrop,
                                        iconColor = Color(0xFF2563EB),
                                        bgColor = Color(0xFFEFF6FF),
                                        title = "Mixing & Dosage",
                                        text = result!!.dosage
                                    )

                                    // Timing Card (Yellow/Orange)
                                    InfoCard(
                                        icon = Icons.Filled.AccessTimeFilled,
                                        iconColor = Color(0xFFD97706),
                                        bgColor = Color(0xFFFFFBEB),
                                        title = "Best Time to Apply",
                                        text = result!!.timing
                                    )

                                    // Safety Warnings Card (Red)
                                    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Safety Warnings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF991B1B))
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            result!!.safetyWarnings.forEach { warning ->
                                                Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                                                    Text("•", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                                                    Text(warning, fontSize = 14.sp, color = Color(0xFF7F1D1D), lineHeight = 20.sp)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Hear Instructions Button
                                    Button(
                                        onClick = {
                                            if (isSpeaking) {
                                                onStopSpeak()
                                                isSpeaking = false
                                            } else {
                                                val speechText = "Product: ${result!!.chemicalName}. Dosage: ${result!!.dosage}. Timing: ${result!!.timing}. Safety warnings: " + result!!.safetyWarnings.joinToString(". ")
                                                onSpeak(speechText, selectedLanguage)
                                                isSpeaking = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)) // Dark sleek button
                                    ) {
                                        Icon(if(isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(if(isSpeaking) "Stop Audio" else "Hear Instructions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                        Text("Scan Another Label", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, iconColor: Color, bgColor: Color, title: String, text: String) {
    Surface(color = bgColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = iconColor.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, fontSize = 15.sp, color = Color(0xFF334155), lineHeight = 22.sp)
        }
    }
}