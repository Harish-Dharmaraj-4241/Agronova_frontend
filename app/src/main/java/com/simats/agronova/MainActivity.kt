package com.simats.agronova

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.agronova.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                SplashScreen {
                    startActivity(Intent(this, LoginActivity::class.java))
                    // Applies the fade transition to the next screen
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Spring Bounce Animation State
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate the logo popping in with a bounce
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Wait 2 seconds, then trigger navigation
        delay(2000)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo using the new drawable we created
            Image(
                painter = painterResource(id = R.drawable.ic_agronova_logo),
                contentDescription = "AgroNova Logo",
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value) // Applies the bounce scale
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AgroNova",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AgroGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Crisper, smaller slogan text
            Text(
                text = "Smart Farming. Powered by Voice.\nDriven by AI.",
                fontSize = 14.sp,
                color = AgroTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    AgronovaTheme {
        SplashScreen {}
    }
}