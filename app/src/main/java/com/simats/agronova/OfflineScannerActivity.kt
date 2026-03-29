package com.simats.agronova

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class OfflineScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                OfflineScannerScreen(onBackClick = { finish() })
            }
        }
    }
}

// --- THIS IS THE CUSTOM AI ENGINE ---
fun runOfflineInference(context: Context, bitmap: Bitmap): String {
    try {
        // 1. Load the Disease Names
        val labels = context.assets.open("labels.txt").bufferedReader().readLines()

        // 2. Load the AI Model (.tflite)
        val fileDescriptor = context.assets.openFd("agronova_disease_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        val interpreter = Interpreter(modelBuffer)

        // 3. Prepare the Image (Resize exactly how we trained it in Colab)
        // 3. Prepare the Image (Resize exactly how we trained it in Colab)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // 🛑 THE BOUNCER: Reject non-plants immediately!
        if (!isLikelyPlant(resizedBitmap)) {
            return "🚫 No Plant Detected\nThis does not look like a plant. Please take a clear photo of a leaf."
        }

        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixelValue in intValues) {
            inputBuffer.putFloat(((pixelValue shr 16 and 0xFF) / 255.0f)) // Red
            inputBuffer.putFloat(((pixelValue shr 8 and 0xFF) / 255.0f))  // Green
            inputBuffer.putFloat(((pixelValue and 0xFF) / 255.0f))        // Blue
        }

        // 4. Run the Scan
        val outputArray = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputBuffer, outputArray)
        interpreter.close()

        // 5. Find the Highest Match
        val probabilities = outputArray[0]
        var maxIndex = 0
        var maxProb = 0.0f
        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        // 6. Format the Output
// 6. Format the Output
        val detectedDisease = labels[maxIndex].replace("_", " ") // Removes underscores for clean text
        val confidence = (maxProb * 100).toInt()

        // --- NEW LOGIC: SMART VERIFICATION ---

        // Scenario A: It's a person, object, or uncertain disease.
        if (confidence <80) {
            return "❓ Unrecognized Image\nI am only $confidence% sure. Please verify in the online scanner, I do not know about this disease or object."
        }

        // Scenario B: The plant is perfectly healthy!
        if (detectedDisease.contains("healthy", ignoreCase = true)) {
            return "✅ Good News!\nThe plant is not affected. It is in good condition. (Confidence: $confidence%)"
        }

        // Scenario C: A disease is confidently detected.
        return "🔴 Disease Detected: $detectedDisease\n🎯 Confidence: $confidence%"

    } catch (e: Exception) {
        e.printStackTrace()
        return "⚠️ Scan Failed! Make sure the model files are in the 'assets' folder."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScannerScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiResultText by remember { mutableStateOf("Upload or take a photo of a crop leaf to scan for diseases.") }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selectedImageBitmap = bitmap
            aiResultText = "Image loaded! Ready to scan."
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            selectedImageBitmap = bitmap
            aiResultText = "Image loaded! Ready to scan."
        }
    }

    // NEW: Runtime Permission request handler
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            aiResultText = "⚠️ Camera permission is required to take photos."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Plant Doctor", fontWeight = FontWeight.Bold, color = AgroGreen) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AgroBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Preview Area
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageBitmap != null) {
                    Image(bitmap = selectedImageBitmap!!.asImageBitmap(), contentDescription = "Selected Crop", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Image Selected", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // CHANGED: Button now safely checks permissions first!
                Button(
                    onClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            takePictureLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }

                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF92400E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Diagnostic Result", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = aiResultText, color = if (aiResultText.contains("Ready")) AgroGreen else if (aiResultText.contains("🟢")) Color(0xFF1B5E20) else Color.Gray, textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { aiResultText = runOfflineInference(context, selectedImageBitmap!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = selectedImageBitmap != null
                    ) {
                        Text("Analyze Crop Disease", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
// --- ADVANCED HSV COMPUTER VISION FILTER ---
fun isLikelyPlant(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var plantColorCount = 0
    val totalPixels = width * height
    val hsv = FloatArray(3)

    for (pixel in pixels) {
        // Convert the raw color to HSV (Hue, Saturation, Value)
        android.graphics.Color.colorToHSV(pixel, hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        // Hue Ranges:
        // 25 to 60 = Yellows/Dead leaves
        // 60 to 160 = True Greens/Healthy plants
        // We also force Saturation and Value > 0.2 to ignore gray shadows and dark blacks
        if (hue in 25f..160f && saturation > 0.2f && value > 0.2f) {
            plantColorCount++
        }
    }

    // Require at least 20% of the image to be actual organic plant colors
    val plantPercentage = (plantColorCount.toFloat() / totalPixels) * 100
    return plantPercentage > 20f
}