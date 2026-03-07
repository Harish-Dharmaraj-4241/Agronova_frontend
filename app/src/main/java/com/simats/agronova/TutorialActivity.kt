package com.simats.agronova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.agronova.ui.theme.AgroBackground
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme

class TutorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme { TutorialScreen(onBack = { finish() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    val steps = listOf(
        Pair("1. Set Your Language & Location", "Go to Settings to pick your language. Set your farm location on the Home page to get accurate weather and market data."),
        Pair("2. Talk to the AI Assistant", "Use the Assistant tab to chat or upload photos. It can scan diseases and translate chemical labels instantly!"),
        Pair("3. Use the Tools & Hub", "Check the Tools page for live agricultural news and calculate your farm inputs before planting.")
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("How to Use", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) },
        containerColor = AgroBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(300.dp)) { page ->
                Surface(
                    shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp,
                    modifier = Modifier.padding(30.dp).fillMaxSize()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(steps[page].first, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AgroGreen, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(steps[page].second, fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager Dots
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(steps.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) AgroGreen else Color.LightGray
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(10.dp))
                }
            }
        }
    }
}