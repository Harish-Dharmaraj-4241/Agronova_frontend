package com.simats.agronova

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                SignupScreen()
            }
        }
    }
}

@Composable
fun SignupScreen(viewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

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
                val msg = (authState as AuthState.Success).message
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.resetState()

                context.startActivity(Intent(context, LoginActivity::class.java))
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

    fun validateAndSignup() {
        val nameRegex = Regex("^[A-Za-z ]+$")
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@(gmail\\.com|mail\\.com|saveetha\\.com)$")
        val phoneRegex = Regex("^[0-9]{10}$")
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,8}$")

        when {
            fullName.isBlank() || !nameRegex.matches(fullName.trim()) -> {
                Toast.makeText(context, "Only letters allowed in Full Name.", Toast.LENGTH_SHORT).show()
            }
            email.isBlank() || !emailRegex.matches(email.trim()) -> {
                Toast.makeText(context, "Email must end with @gmail.com, @mail.com or @saveetha.com", Toast.LENGTH_SHORT).show()
            }
            phone.isBlank() || !phoneRegex.matches(phone.trim()) -> {
                Toast.makeText(context, "Phone number must contain exactly 10 digits.", Toast.LENGTH_SHORT).show()
            }
            password.isBlank() || !passwordRegex.matches(password) -> {
                Toast.makeText(context, "Password must contain 1 uppercase, 1 lowercase, 1 number, 1 special char (6-8 chars).", Toast.LENGTH_LONG).show()
            }
            password != confirmPassword -> {
                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Trigger the ViewModel API Call instead of skipping straight to success
                viewModel.signup(fullName.trim(), email.trim(), phone.trim(), password)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))
            AgroNovaLogo(subtitle = "Create Your Account")
            Spacer(modifier = Modifier.height(30.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Full Name
                    TextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = { Text("Full Name", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = "Name", tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email Address", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = "Email", tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone
                    TextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("Phone Number", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = "Phone", tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
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
                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Confirm Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = "Confirm Password", tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(icon, contentDescription = "Toggle Confirm Password", tint = Color.Gray)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { validateAndSignup() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                        enabled = authState != AuthState.Loading // Prevent double clicks
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Sign Up", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.padding(bottom = 32.dp)) {
                Text("Already have an account? ", color = AgroTextSecondary)
                Text(
                    text = "Login",
                    color = AgroGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(context, LoginActivity::class.java))
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
fun SignupPreview() {
    AgronovaTheme {
        SignupScreen()
    }
}