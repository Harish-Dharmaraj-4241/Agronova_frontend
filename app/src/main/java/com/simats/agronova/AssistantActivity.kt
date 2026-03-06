package com.simats.agronova

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simats.agronova.ui.theme.*
import com.simats.agronova.user.AgroBottomNav
import com.simats.agronova.user.NavScreen
import com.simats.agronova.viewmodel.AssistantViewModel
import com.simats.agronova.viewmodel.ChatMessage
import java.util.*

class AssistantActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    // State to track if AI is currently talking
    val isTtsSpeaking = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isTtsSpeaking.value = true }
            override fun onDone(utteranceId: String?) { isTtsSpeaking.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isTtsSpeaking.value = false }
        })

        setContent {
            AgronovaTheme {
                AssistantScreen(
                    viewModel = viewModel(),
                    isTtsSpeaking = isTtsSpeaking.value,
                    onSpeakRequested = { text -> speakOut(text) },
                    onStopAudio = { stopSpeaking() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    private fun speakOut(text: String) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ai_reply")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ai_reply")
    }

    private fun stopSpeaking() {
        if (tts.isSpeaking) {
            tts.stop()
            isTtsSpeaking.value = false
        }
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
fun AssistantScreen(
    viewModel: AssistantViewModel,
    isTtsSpeaking: Boolean,
    onSpeakRequested: (String) -> Unit,
    onStopAudio: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.latestVoiceResponse.value) {
        viewModel.latestVoiceResponse.value?.let { onSpeakRequested(it) }
    }
    LaunchedEffect(viewModel.chatHistory.size, viewModel.isAiTyping.value) {
        if (viewModel.chatHistory.isNotEmpty()) listState.animateScrollToItem(viewModel.chatHistory.size)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isRecording = true
            speechRecognizer.startListening(speechIntent)
        } else {
            Toast.makeText(context, "Microphone permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(error: Int) { isRecording = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Append spoken text to existing input text (supports image + text + voice)
                    inputText = if (inputText.isEmpty()) matches[0] else "$inputText ${matches[0]}"
                }
                isRecording = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }

    Scaffold(
        bottomBar = {
            AgroBottomNav(
                currentScreen = NavScreen.Assistant,
                onItemSelected = { screen ->
                    when (screen) {
                        NavScreen.Home -> {
                            context.startActivity(Intent(context, HomeActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        NavScreen.Assistant -> {}
                        NavScreen.Ledger -> {
                            context.startActivity(Intent(context, LedgerActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        NavScreen.Profile -> {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                    }
                }
            )
        },
        containerColor = AgroBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Header with Stop Button and Language Dropdown
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AgroNova AI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AgroGreen)

                    // Conditionally show Stop Voice button
                    if (isTtsSpeaking) {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = onStopAudio,
                            modifier = Modifier.size(32.dp).background(Color(0xFFFFEBEE), CircleShape)
                        ) {
                            Icon(Icons.Filled.VolumeOff, contentDescription = "Stop Voice", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White, shadowElevation = 2.dp,
                        modifier = Modifier.clickable { expandedLanguageMenu = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.selectedLanguage.value, fontWeight = FontWeight.Bold, color = AgroTextPrimary)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(expanded = expandedLanguageMenu, onDismissRequest = { expandedLanguageMenu = false }) {
                        viewModel.availableLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language) },
                                onClick = {
                                    viewModel.selectedLanguage.value = language
                                    expandedLanguageMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.chatHistory) { message -> ChatBubble(message) }
                if (viewModel.isAiTyping.value) item { TypingIndicatorBubble() }
            }

            // Input Area (Professional UI)
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Image Preview
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            AsyncImage(
                                model = selectedImageUri, contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).offset(x=8.dp, y=(-8).dp).background(Color.Black.copy(alpha=0.6f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add Image", tint = AgroGreen, modifier = Modifier.size(28.dp))
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (isRecording) "Listening..." else "Message AgroNova...") },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AgroGreen
                            )
                        )

                        // Separate Mic Button (Always available to append voice)
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    speechRecognizer.stopListening()
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        speechRecognizer.startListening(speechIntent)
                                        isRecording = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp).background(if (isRecording) Color.Red.copy(alpha=0.1f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Voice",
                                tint = if (isRecording) Color.Red else AgroGreen
                            )
                        }

                        // Send Button
                        if (inputText.isNotEmpty() || selectedImageUri != null) {
                            IconButton(
                                onClick = {
                                    if (!viewModel.isAiTyping.value) {
                                        viewModel.sendMessage(context, inputText, selectedImageUri)
                                        inputText = ""
                                        selectedImageUri = null
                                    }
                                },
                                modifier = Modifier.size(40.dp).background(AgroGreen, CircleShape)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!message.isUser) {
                Box(modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape).padding(6.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Eco, contentDescription = "AI", tint = AgroGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = if (message.isUser) 20.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 20.dp
                ),
                color = if (message.isUser) AgroGreen else Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (message.attachedImageUri != null) {
                        AsyncImage(
                            model = message.attachedImageUri, contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = if (message.isUser) Color.White else AgroTextPrimary,
                            fontSize = 15.sp, lineHeight = 22.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.timestampLabel, fontSize = 10.sp, color = Color.Gray,
            modifier = Modifier.padding(start = if (message.isUser) 0.dp else 40.dp, end = if (message.isUser) 4.dp else 0.dp)
        )
    }
}

@Composable
fun TypingIndicatorBubble() {
    Row(verticalAlignment = Alignment.Bottom) {
        Box(modifier = Modifier.size(32.dp).background(Color(0xFFE8F5E9), CircleShape).padding(6.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Eco, contentDescription = "AI", tint = AgroGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp), color = Color.White, shadowElevation = 1.dp) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                (0..2).forEach { index ->
                    val yOffset by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = -6f,
                        animationSpec = infiniteRepeatable(animation = tween(300, delayMillis = index * 100), repeatMode = RepeatMode.Reverse),
                        label = "dot"
                    )
                    Box(modifier = Modifier.offset(y = yOffset.dp).size(6.dp).background(Color.Gray.copy(alpha=0.5f), CircleShape))
                }
            }
        }
    }
}