package com.simats.agronova

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.agronova.ui.theme.*
import com.simats.agronova.user.AgroBottomNav
import com.simats.agronova.user.NavScreen

// --- Data Models ---
data class Transaction(val title: String, val date: String, val amount: Double, val isIncome: Boolean, val category: String)
data class MarketInsight(val cropName: String, val insightText: String, val isPositiveTrend: Boolean)

class LedgerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                LedgerScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val context = LocalContext.current

    // Mock Data (Replace with data from your ViewModel/Backend)
    val transactions = listOf(
        Transaction("Tomato Harvest Sale", "Oct 22 • Sales", 2100.0, true, "Sales"),
        Transaction("Organic Fertilizer", "Oct 24 • Supplies", 450.0, false, "Supplies"),
        Transaction("Irrigation Repair", "Oct 20 • Equipment", 820.0, false, "Equipment")
    )

    val insights = listOf(
        MarketInsight("Tomato", "Current: ₹40/kg. Expected to rise 5% next week due to heavy rains.", true),
        MarketInsight("Urea Fertilizer", "Prices stable globally. Govt subsidies active for this month.", true)
    )

    Scaffold(
        bottomBar = {
            AgroBottomNav(
                currentScreen = NavScreen.Ledger,
                onItemSelected = { screen ->
                    when (screen) {
                        NavScreen.Home -> {
                            context.startActivity(Intent(context, HomeActivity::class.java))
                            val activity = context as? android.app.Activity
                            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            activity?.finish()
                        }
                        NavScreen.Assistant -> {
                            context.startActivity(Intent(context, AssistantActivity::class.java))
                            val activity = context as? android.app.Activity
                            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            activity?.finish()
                        }
                        NavScreen.Ledger -> {
                            // Already on Ledger, do nothing
                        }
                        NavScreen.Profile -> {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                            val activity = context as? android.app.Activity
                            activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            activity?.finish()
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // TODO: Trigger Voice Recognition Intent here
                },
                icon = { Icon(Icons.Filled.Mic, contentDescription = "Voice Assistant", tint = Color.White) },
                text = { Text("Ask / Add Entry", color = Color.White) },
                containerColor = AgroGreen // Using theme color matching HomeActivity
            )
        },
        containerColor = AgroBackground // Matching background from HomeActivity
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp), // Matched padding with HomeActivity
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Farm Ledger", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AgroGreen)
                    IconButton(onClick = { /* Export Logic */ }) {
                        Icon(Icons.Filled.Download, contentDescription = "Export Ledger", tint = Color.Gray)
                    }
                }
            }

            // Net Profit Card
            item { NetProfitCard(totalIncome = 6540.0, totalExpense = 2260.0) }

            // Gemini Market Insights Section
            item {
                Text("Market Insights & Predictions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AgroTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(insights) { insight ->
                        MarketInsightCard(insight)
                    }
                }
            }

            // Recent Transactions List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AgroTextPrimary)
                    TextButton(onClick = { /* View All Logic */ }) {
                        Text("VIEW ALL", color = AgroGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(transactions) { transaction ->
                TransactionItem(transaction)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Padding for FAB
        }
    }
}

@Composable
fun NetProfitCard(totalIncome: Double, totalExpense: Double) {
    val netProfit = totalIncome - totalExpense
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), // Matched border radius with HomeActivity
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // Matched elevation with HomeActivity
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NET PROFIT", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("₹${netProfit}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = AgroGreen)

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL INCOME", fontSize = 10.sp, color = Color.Gray)
                    Text("↑ ₹${totalIncome}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL EXPENSE", fontSize = 10.sp, color = Color.Gray)
                    Text("↓ ₹${totalExpense}", color = Color(0xFFF44336), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MarketInsightCard(insight: MarketInsight) {
    Card(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (insight.isPositiveTrend) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (insight.isPositiveTrend) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (insight.isPositiveTrend) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(insight.cropName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AgroTextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = insight.insightText,
                fontSize = 13.sp,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background mapped similarly to ActionCards in HomeActivity
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AgroTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(transaction.date, color = Color.Gray, fontSize = 12.sp)
            }

            Text(
                text = "${if(transaction.isIncome) "+" else "-"}₹${transaction.amount}",
                color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerPreview() {
    AgronovaTheme {
        LedgerScreen()
    }
}