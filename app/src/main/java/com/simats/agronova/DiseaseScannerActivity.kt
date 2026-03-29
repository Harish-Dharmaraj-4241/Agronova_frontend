package com.simats.agronova

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.DiseaseScannerViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.*

// Helper function to compress camera bitmap and convert to Uri for Gemini upload
fun bitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    val out = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) // 85% compression saves data!
    out.flush()
    out.close()
    return Uri.fromFile(file)
}

class DiseaseScannerActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            AgronovaTheme {
                DiseaseScannerScreen(
                    onBack = { finish() },
                    onSpeak = { text, langCode -> speakResult(text, langCode) },
                    onStopSpeak = { tts.stop() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        }
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
                Toast.makeText(this, "Voice not supported for this language. Install voice data in Settings.", Toast.LENGTH_LONG).show()
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

enum class ScanState { EMPTY, LOADING, ERROR, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseScannerScreen(
    viewModel: DiseaseScannerViewModel = viewModel(),
    onBack: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("USER_LANGUAGE", "English") ?: "English") }
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val languages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    val result by viewModel.scanResult
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage

    // Camera Launcher - Takes picture, compresses it, and sends to Gemini
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val compressedUri = bitmapToUri(context, bitmap)
            selectedImageUri = compressedUri
            viewModel.analyzeImage(context, compressedUri, selectedLanguage)
        }
    }

    // Gallery Launcher
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            viewModel.analyzeImage(context, uri, selectedLanguage)
        }
    }

    // Permission Handler
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required to scan.", Toast.LENGTH_SHORT).show()
        }
    }

    val currentState = when {
        isLoading -> ScanState.LOADING
        error != null -> ScanState.ERROR
        result != null -> ScanState.SUCCESS
        else -> ScanState.EMPTY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Disease Scanner", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 22.sp) },
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
                                        sharedPrefs.edit().putString("USER_LANGUAGE", lang).apply()

                                        if (selectedImageUri != null && !isLoading) {
                                            viewModel.analyzeImage(context, selectedImageUri!!, lang)
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
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Top Area: Scanner OR Blurred Image
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected Crop",
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).blur(12.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).background(Color.Black.copy(alpha = 0.4f)))
            } else {
                // Dual-Button Empty State (Camera + Gallery)
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f).background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CropFree, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        takePictureLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f).align(Alignment.BottomCenter).shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp).verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.width(48.dp).height(5.dp).background(Color(0xFFE2E8F0), CircleShape).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(24.dp))

                    Crossfade(targetState = currentState, animationSpec = tween(500), label = "ScanState") { state ->
                        when (state) {
                            ScanState.EMPTY -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.EnergySavingsLeaf, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Awaiting Crop Image", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text("Upload or capture an image to start AI diagnosis.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                                }
                            }
                            ScanState.LOADING -> {
                                Column(modifier = Modifier.fillMaxWidth().height(250.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(color = AgroGreen, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Analyzing Crop Health...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Text("Identifying diseases and treatment plans", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                            ScanState.ERROR -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(60.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(error!!, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(onClick = { imagePicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = AgroGreen), shape = RoundedCornerShape(12.dp)) {
                                        Text("Try Another Image", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            ScanState.SUCCESS -> {
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(result!!.diseaseName, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), lineHeight = 32.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Detected in Crop", fontSize = 14.sp, color = Color(0xFF64748B))
                                        }
                                        Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Verified, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(result!!.confidence, color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(result!!.description, fontSize = 15.sp, color = Color(0xFF334155), lineHeight = 24.sp)
                                    Spacer(modifier = Modifier.height(28.dp))

                                    Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Treatment Steps", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))

                                            result!!.treatmentSteps.forEachIndexed { index, step ->
                                                Row(modifier = Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
                                                    Box(modifier = Modifier.size(26.dp).background(Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) {
                                                        Text("${index + 1}", color = AgroGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(step, fontSize = 14.sp, color = Color(0xFF475569), lineHeight = 22.sp, modifier = Modifier.padding(top = 2.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(28.dp))

                                    Button(
                                        onClick = {
                                            if (isSpeaking) {
                                                onStopSpeak()
                                                isSpeaking = false
                                            } else {
                                                val speechText = "Disease detected: ${result!!.diseaseName}. ${result!!.description}. Treatment steps: " + result!!.treatmentSteps.joinToString(". ")
                                                onSpeak(speechText, selectedLanguage)
                                                isSpeaking = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(6.dp, RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                                    ) {
                                        Icon(if(isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(if(isSpeaking) "Stop Audio" else "Hear Instructions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    TextButton(
                                        onClick = { imagePicker.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth().height(50.dp)
                                    ) {
                                        Text("Scan Another Crop", color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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