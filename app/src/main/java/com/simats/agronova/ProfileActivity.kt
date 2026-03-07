package com.simats.agronova

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.user.AgroBottomNav
import com.simats.agronova.user.NavScreen
import com.simats.agronova.viewmodel.ProfileViewModel
import java.io.ByteArrayOutputStream

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                ProfileScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
    val userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: ""

    // Image Picker Setup
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                // Compress image so it's not too heavy for the network
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                // Update UI instantly
                viewModel.profileImageBase64.value = base64String
                viewModel.pendingImageUploadBase64 = base64String
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (userEmail.isNotEmpty()) {
            viewModel.fetchProfile(userEmail)
        }
    }

    LaunchedEffect(viewModel.successMessage.value) {
        viewModel.successMessage.value?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            // Update SharedPreferences so the Home Page greets them properly
            sharedPrefs.edit().putString("USER_NAME", viewModel.name.value).apply()
            viewModel.successMessage.value = null
        }
    }

    LaunchedEffect(viewModel.errorMessage.value) {
        viewModel.errorMessage.value?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMessage.value = null
        }
    }

    Scaffold(
        bottomBar = {
            AgroBottomNav(
                currentScreen = NavScreen.Profile,
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
                        NavScreen.Tools -> {
                            context.startActivity(Intent(context, ToolsActivity::class.java))
                            (context as? Activity)?.overridePendingTransition(0, 0)
                        }
                        NavScreen.Profile -> {} // We are here
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Clean Header aligned to the left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Edit Profile",
                    color = AgroGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Avatar Box
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.size(130.dp)
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val base64String = viewModel.profileImageBase64.value
                    if (base64String != null) {
                        // 1. Decode the image inside try-catch (NO Composables here)
                        val bitmap = try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }

                        // 2. Call the Composables outside based on the result
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                        }
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                    }
                }

                // Camera Button
                Surface(
                    shape = CircleShape,
                    color = AgroGreen,
                    modifier = Modifier
                        .size(36.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shadowElevation = 4.dp
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Upload", tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Header Names
            Text(text = if (viewModel.name.value.isEmpty()) "Your Name" else viewModel.name.value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            if (viewModel.username.value.isNotEmpty()) {
                Text(text = "@${viewModel.username.value}", fontSize = 14.sp, color = AgroGreen, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // FULL NAME
                    OutlinedTextField(
                        value = viewModel.name.value,
                        onValueChange = { viewModel.name.value = it },
                        label = { Text("Full Name (No spaces)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (viewModel.name.value.isNotEmpty()) {
                                Icon(
                                    imageVector = if (viewModel.isNameValid()) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (viewModel.isNameValid()) AgroGreen else Color.Red
                                )
                            }
                        }
                    )
                    if (viewModel.name.value.isNotEmpty() && !viewModel.isNameValid()) {
                        Text("Only letters allowed, no spaces or numbers.", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // USERNAME
                    OutlinedTextField(
                        value = viewModel.username.value,
                        onValueChange = { viewModel.username.value = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (viewModel.username.value.isNotEmpty()) {
                                Icon(
                                    imageVector = if (viewModel.isUsernameValid()) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (viewModel.isUsernameValid()) AgroGreen else Color.Red
                                )
                            }
                        }
                    )
                    if (viewModel.username.value.isNotEmpty() && !viewModel.isUsernameValid()) {
                        Text("No spaces allowed in username.", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PHONE NUMBER
                    OutlinedTextField(
                        value = viewModel.phone.value,
                        onValueChange = { viewModel.phone.value = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (viewModel.phone.value.isNotEmpty()) {
                                Icon(
                                    imageVector = if (viewModel.isPhoneValid()) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (viewModel.isPhoneValid()) AgroGreen else Color.Red
                                )
                            }
                        }
                    )
                    if (viewModel.phone.value.isNotEmpty() && !viewModel.isPhoneValid()) {
                        Text("Must be exactly 10 digits.", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // EMAIL (Read Only / Disabled)
                    OutlinedTextField(
                        value = viewModel.email.value,
                        onValueChange = { }, // Does nothing
                        readOnly = true,
                        enabled = false, // Greys out the box
                        label = { Text("Email Address (Unchangeable)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Color(0xFFE2E8F0),
                            disabledLabelColor = Color.Gray,
                            disabledTextColor = Color.Gray,
                            disabledContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Button
                    Button(
                        onClick = { viewModel.saveProfile() },
                        enabled = !viewModel.isLoading.value && viewModel.isNameValid() && viewModel.isUsernameValid() && viewModel.isPhoneValid(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (viewModel.isLoading.value) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}