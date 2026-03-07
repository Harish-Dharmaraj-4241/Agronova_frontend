package com.simats.agronova

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.SettingsViewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    val userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: ""

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    // Handle account deletion success
    LaunchedEffect(viewModel.accountDeleted.value) {
        if (viewModel.accountDeleted.value) {
            Toast.makeText(context, "Account Permanently Deleted", Toast.LENGTH_LONG).show()
            sharedPrefs.edit().clear().apply()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold, color = AgroGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroBackground)
            )
        },
        containerColor = AgroBackground
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp, vertical = 10.dp)) {

            SettingsItem("How to Use the Application", Icons.Filled.HelpOutline, Color(0xFFE8F5E9), AgroGreen) {
                context.startActivity(Intent(context, TutorialActivity::class.java))
            }
            SettingsItem("App Features", Icons.Filled.StarOutline, Color(0xFFE8F5E9), AgroGreen) {
                context.startActivity(Intent(context, AppFeaturesActivity::class.java))
            }
            SettingsItem("Change Password", Icons.Filled.LockReset, Color(0xFFE8F5E9), AgroGreen) {
                context.startActivity(Intent(context, ChangePasswordActivity::class.java))
            }
            SettingsItem("Language Preference", Icons.Filled.Language, Color(0xFFE8F5E9), AgroGreen) {
                context.startActivity(Intent(context, LanguagePreferenceActivity::class.java))
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsItem("Logout", Icons.Filled.Logout, Color(0xFFFEF2F2), Color(0xFFDC2626)) {
                showLogoutDialog = true
            }
            SettingsItem("Delete Account", Icons.Filled.DeleteOutline, Color(0xFFFEF2F2), Color(0xFFDC2626)) {
                showDeleteDialog = true
            }
        }

        // LOGOUT DIALOG
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out of AgroNova?") },
                confirmButton = {
                    Button(
                        onClick = {
                            sharedPrefs.edit().clear().apply() // Clear data!
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                    ) { Text("Yes, Logout") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }

        // DELETE ACCOUNT DIALOG
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account", fontWeight = FontWeight.Bold, color = Color.Red) },
                text = {
                    Column {
                        Text("This action is permanent and will wipe all your data (crops, posts, profile).")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Type 'DELETE' below to confirm:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deleteConfirmText,
                            onValueChange = { deleteConfirmText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Red)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteAccount(userEmail) },
                        enabled = deleteConfirmText == "DELETE" && !viewModel.isLoading.value,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        if(viewModel.isLoading.value) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("Permanently Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; deleteConfirmText = "" }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, iconBgColor: Color, iconColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = iconBgColor, modifier = Modifier.size(40.dp)) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(iconColor == AgroGreen) Color(0xFF0F172A) else iconColor)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}