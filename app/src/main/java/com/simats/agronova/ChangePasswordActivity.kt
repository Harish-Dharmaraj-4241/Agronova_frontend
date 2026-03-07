package com.simats.agronova

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.SettingsViewModel

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                ChangePasswordScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val userEmail = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE).getString("USER_EMAIL", "") ?: ""

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }

    // Validation Logic
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasLower = newPassword.any { it.isLowerCase() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val hasNoSpace = !newPassword.contains(" ")
    val isLengthValid = newPassword.length in 6..8
    val isNewValid = hasUpper && hasLower && hasSpecial && hasNoSpace && isLengthValid
    val isConfirmMatch = newPassword.isNotEmpty() && newPassword == confirmPassword

    LaunchedEffect(viewModel.successMessage.value) {
        if (viewModel.successMessage.value != null) {
            Toast.makeText(context, viewModel.successMessage.value, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(viewModel.errorMessage.value) {
        if (viewModel.errorMessage.value != null) {
            Toast.makeText(context, viewModel.errorMessage.value, Toast.LENGTH_SHORT).show()
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Change Password", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }) },
        containerColor = AgroBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {

            if (!viewModel.isPasswordVerified.value) {
                // STEP 1: VERIFY CURRENT PASSWORD
                Text("Verify Current Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPassword, onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = if (showCurrentPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                            Icon(if (showCurrentPass) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.verifyCurrentPassword(userEmail, currentPassword) },
                    enabled = currentPassword.isNotEmpty() && !viewModel.isLoading.value,
                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                ) { Text("Verify Password") }
            } else {
                // STEP 2: ENTER NEW PASSWORD
                Text("Create New Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showNewPass = !showNewPass }) { Icon(if (showNewPass) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                // Password Live Rules Checklist
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    RuleText("6 to 8 characters", isLengthValid)
                    RuleText("1 Uppercase & 1 Lowercase", hasUpper && hasLower)
                    RuleText("1 Special Character (!@#$%)", hasSpecial)
                    RuleText("No Spaces", hasNoSpace && newPassword.isNotEmpty())
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    isError = confirmPassword.isNotEmpty() && !isConfirmMatch
                )
                if (confirmPassword.isNotEmpty() && !isConfirmMatch) {
                    Text("Passwords do not match", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.updatePassword(userEmail, newPassword) },
                    enabled = isNewValid && isConfirmMatch && !viewModel.isLoading.value,
                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                ) { Text("Update Password") }
            }
        }
    }
}

@Composable
fun RuleText(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isValid) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, tint = if (isValid) AgroGreen else Color.Red, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = if (isValid) AgroGreen else Color.Gray)
    }
}