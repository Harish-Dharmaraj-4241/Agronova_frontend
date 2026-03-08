package com.simats.agronova

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.ui.theme.*
import com.simats.agronova.viewmodel.AuthState
import com.simats.agronova.viewmodel.AuthViewModel

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                LoginScreen()
            }
        }
    }
}

// Reusable Logo Component
@Composable
fun AgroNovaLogo(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(80.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Eco,
                    contentDescription = "Leaf Logo",
                    tint = AgroGreen,
                    modifier = Modifier
                        .size(45.dp)
                        .align(Alignment.Center)
                        .rotate(-20f)
                )
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = "Sun Logo",
                    tint = AgroAccent,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AgroNova",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AgroGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AgroGreen
        )
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF5F5F5),
        unfocusedContainerColor = Color(0xFFF5F5F5),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = AgroGreen
    )

    // Handle ViewModel State Changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val successState = authState as AuthState.Success
                Toast.makeText(context, successState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()

                // 1. Save the login session, email, and user data using SharedPreferences
                val sharedPreferences = context.getSharedPreferences("AgroNovaPrefs", android.content.Context.MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("USER_EMAIL", email.trim())
                    putBoolean("IS_LOGGED_IN", true)

                    // Save the name and location safely if they exist
                    // Save the name and location safely if they exist
                    successState.name?.let { putString("USER_NAME", it) }
                    successState.locationString?.let { putString("FARM_LOCATION", it) }
                    successState.lat?.let { putFloat("LATITUDE", it.toFloat()) }
                    successState.lon?.let { putFloat("LONGITUDE", it.toFloat()) }

                    // NEW: Save the cloud language directly into local settings on login!
                    // If successState.language is null (e.g. your ViewModel didn't pass it), fallback to English
                    putString("USER_LANGUAGE", successState.language ?: "English")

                    apply()
                }

                // 2. Navigate to HomeActivity and apply the fade transition
                context.startActivity(Intent(context, HomeActivity::class.java))
                val activity = context as? android.app.Activity
                activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                activity?.finish()
            }
            is AuthState.Error -> {
                val err = (authState as AuthState.Error).error
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    fun handleLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.login(email.trim(), password)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AgroNovaLogo(subtitle = "Smart Farming. Powered by Voice.")

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email Address", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email", tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "Password", tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = "Toggle Password", tint = Color.Gray)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { handleLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                        enabled = authState != AuthState.Loading
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("Don't have an account? ", color = AgroTextSecondary)
                Text(
                    text = "Sign Up",
                    color = AgroGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, SignupActivity::class.java))
                        val activity = context as? android.app.Activity
                        activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        activity?.finish()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    AgronovaTheme {
        LoginScreen()
    }
}