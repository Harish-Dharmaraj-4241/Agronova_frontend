package com.simats.agronova

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme

class LanguagePreferenceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                LanguageScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("USER_LANGUAGE", "English") ?: "English") }

    val languages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    Scaffold(
        topBar = { TopAppBar(title = { Text("App Language", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) },
        containerColor = AgroBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text("Select your preferred language. This will apply to the AI Assistant, Market Insights, and Crop Tools globally.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn {
                items(languages) { lang ->
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                            selectedLanguage = lang
                            sharedPrefs.edit().putString("USER_LANGUAGE", lang).apply()
                            Toast.makeText(context, "Language set to $lang", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedLanguage == lang, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AgroGreen))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(lang, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}