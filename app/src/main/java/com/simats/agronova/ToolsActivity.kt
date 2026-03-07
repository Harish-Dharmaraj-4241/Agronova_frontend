package com.simats.agronova

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.model.AgriNewsItem
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.user.AgroBottomNav
import com.simats.agronova.user.NavScreen
import com.simats.agronova.viewmodel.ToolsViewModel

class ToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                ToolsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(viewModel: ToolsViewModel = viewModel()) {
    val context = LocalContext.current
    var showCalculatorDialog by remember { mutableStateOf(false) }

    // Language Dropdown Setup
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("USER_LANGUAGE", "English") ?: "English") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val availableLanguages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    // Fetch initial news
    LaunchedEffect(Unit) { viewModel.fetchNews(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Tools & Hub", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    // NEW: The Missing Language Selector Dropdown!
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
                                        // Save chosen language and FORCE a fresh fetch for the new language news!
                                        sharedPrefs.edit().putString("USER_LANGUAGE", lang).apply()
                                        viewModel.fetchNews(context, forceRefresh = true)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AgroBottomNav(
                currentScreen = NavScreen.Tools,
                onItemSelected = { screen ->
                    when (screen) {
                        NavScreen.Home -> {
                            context.startActivity(Intent(context, HomeActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(0, 0)
                        }
                        NavScreen.Assistant -> {
                            context.startActivity(Intent(context, AssistantActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(0, 0)
                        }
                        NavScreen.Tools -> {} // We are here
                        NavScreen.Profile -> {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(0, 0)
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            // TOP SECTION: Premium Hero Card (Combines Seed & Fertilizer)
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().clickable { showCalculatorDialog = true }
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(AgroGreen, Color(0xFF1B5E20))))
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.padding(14.dp).size(30.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("AI Farm Input Calculator", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap to get exact seed & fertilizer estimates instantly.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("🗞️ Live Agri-News", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(16.dp))

            // BOTTOM SECTION: News Feed
            if (viewModel.isNewsLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                        CircularProgressIndicator(color = AgroGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching latest headlines...", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (viewModel.newsList.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Text("No news available right now.", color = Color.Gray, modifier = Modifier.padding(top = 40.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                    items(viewModel.newsList.value) { news ->
                        NewsCard(news = news, userLanguage = selectedLanguage)
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }

        if (showCalculatorDialog) {
            AIInputCalculatorDialog(
                viewModel = viewModel,
                onDismiss = {
                    showCalculatorDialog = false
                    viewModel.resetCalculator()
                }
            )
        }
    }
}

@Composable
fun NewsCard(news: AgriNewsItem, userLanguage: String) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable {
            if (news.link.isNotEmpty()) {
                // FIX: Open the link directly!
                // The Python backend is already fetching native regional websites,
                // so we don't need the Google Translate wrapper here anymore!
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                context.startActivity(intent)
            }
        }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Article, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(22.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(news.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp)) {
                    Text(news.source, fontSize = 11.sp, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Read Article", fontSize = 13.sp, color = AgroGreen, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInputCalculatorDialog(viewModel: ToolsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var cropName by remember { mutableStateOf("") }
    var landSize by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Acres") }
    var expandedMenu by remember { mutableStateOf(false) }
    val units = listOf("Acres", "Hectares", "Sq Meters")

    val result = viewModel.calculatorResult.value
    val isLoading = viewModel.isCalcLoading.value
    val error = viewModel.calcError.value

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Farm Inputs Calculator", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (result != null) {
                    // SHOW RESULTS
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("🌱 Seeds Needed", fontWeight = FontWeight.Bold, color = AgroGreen, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(result.seedRequirement, fontSize = 15.sp, color = Color(0xFF1E293B), lineHeight = 22.sp)

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("🧪 Fertilizer Needed", fontWeight = FontWeight.Bold, color = AgroGreen, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(result.fertilizerRequirement, fontSize = 15.sp, color = Color(0xFF1E293B), lineHeight = 22.sp)

                            Spacer(modifier = Modifier.height(20.dp))
                            Divider(color = AgroGreen.copy(alpha = 0.2f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.TipsAndUpdates, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(result.proTip, fontSize = 13.sp, color = Color(0xFF92400E), lineHeight = 18.sp)
                            }
                        }
                    }
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AgroGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calculating requirements...", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    // SHOW INPUTS
                    Text("Enter your farm details to instantly calculate the exact seeds and fertilizers required.", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = cropName, onValueChange = { cropName = it },
                        label = { Text("Crop Name (e.g. Tomato)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AgroGreen)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = landSize, onValueChange = { landSize = it },
                            label = { Text("Size") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AgroGreen)
                        )

                        ExposedDropdownMenuBox(expanded = expandedMenu, onExpandedChange = { expandedMenu = !expandedMenu }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedUnit, onValueChange = {}, readOnly = true, label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                                modifier = Modifier.menuAnchor(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AgroGreen)
                            )
                            ExposedDropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                                units.forEach { unit ->
                                    DropdownMenuItem(text = { Text(unit) }, onClick = { selectedUnit = unit; expandedMenu = false })
                                }
                            }
                        }
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(error, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result == null && !isLoading) {
                Button(
                    onClick = { if (cropName.isNotEmpty() && landSize.isNotEmpty()) viewModel.calculateInputs(context, cropName, landSize, selectedUnit) },
                    enabled = cropName.isNotEmpty() && landSize.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                    modifier = Modifier.height(45.dp).padding(horizontal = 8.dp)
                ) { Text("Calculate Now", fontWeight = FontWeight.Bold) }
            } else if (result != null) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AgroGreen), modifier = Modifier.height(45.dp)) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            if (result == null && !isLoading) { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) } }
        }
    )
}