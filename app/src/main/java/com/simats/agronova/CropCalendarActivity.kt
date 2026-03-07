package com.simats.agronova

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.model.CropCalendarResponse
import com.simats.agronova.model.TaskResponse
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.CropCalendarViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CropCalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                CropCalendarScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropCalendarScreen(
    viewModel: CropCalendarViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val calendars by viewModel.calendars
    val isLoading by viewModel.isLoading

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCropIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchCalendars(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Calendar", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { showAddDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Crop", tint = AgroGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Crop", color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9) // Light grayish background matching the rest of the app
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Crossfade(targetState = Pair(isLoading, calendars.isEmpty()), animationSpec = tween(500), label = "CalendarState") { (loading, empty) ->
                if (loading && empty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AgroGreen)
                    }
                } else if (empty) {
                    // Premium Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color(0xFFE8F5E9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Eco, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(60.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("No Active Crops", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Start tracking your farm's progress. Let AI create a complete day-by-day cultivation schedule for you.", color = Color(0xFF64748B), textAlign = TextAlign.Center, fontSize = 15.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.height(50.dp).shadow(4.dp, RoundedCornerShape(25.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreen)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Active Calendar View
                    val currentCalendar = calendars.getOrNull(selectedCropIndex) ?: calendars.first()

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Modern Crop Selector Dropdown
                        if (calendars.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedCropIndex,
                                containerColor = Color.White,
                                contentColor = AgroGreen,
                                edgePadding = 16.dp,
                                divider = {} // Remove harsh bottom line
                            ) {
                                calendars.forEachIndexed { index, crop ->
                                    Tab(
                                        selected = selectedCropIndex == index,
                                        onClick = { selectedCropIndex = index },
                                        text = {
                                            Text(
                                                crop.cropName,
                                                fontWeight = if (selectedCropIndex == index) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (selectedCropIndex == index) AgroGreen else Color.Gray,
                                                fontSize = 15.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                GrowthProgressCard(currentCalendar)
                            }

                            // Next Pending Task
                            val nextTask = currentCalendar.tasks.firstOrNull { !it.isCompleted }
                            if (nextTask != null) {
                                item {
                                    TodayTaskCard(nextTask) {
                                        viewModel.markTaskCompleted(context, nextTask.id)
                                    }
                                }
                            } else {
                                item {
                                    // All caught up state
                                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Verified, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("All Caught Up!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF065F46))
                                                Text("There are no pending tasks for today.", fontSize = 14.sp, color = Color(0xFF047857))
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Cultivation Timeline", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            }

                            // Vertical Timeline Feed
                            itemsIndexed(currentCalendar.tasks) { index, task ->
                                val isLast = index == currentCalendar.tasks.size - 1
                                TimelineItem(task, isLast)
                            }

                            item { Spacer(modifier = Modifier.height(40.dp)) }
                        }
                    }
                }
            }

            // Glassmorphism-style AI Generator Dialog
            if (showAddDialog) {
                AddCropDialog(
                    onDismiss = { showAddDialog = false },
                    onGenerate = { cropName, date ->
                        viewModel.generateCalendar(context, cropName, date)
                        showAddDialog = false
                    },
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
fun GrowthProgressCard(calendar: CropCalendarResponse) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sowDate = sdf.parse(calendar.sowingDate) ?: Date()
    val harvestDate = sdf.parse(calendar.harvestDate) ?: Date()
    val today = Date()

    val totalDays = TimeUnit.DAYS.convert(harvestDate.time - sowDate.time, TimeUnit.MILLISECONDS).coerceAtLeast(1)
    val passedDays = TimeUnit.DAYS.convert(today.time - sowDate.time, TimeUnit.MILLISECONDS).coerceIn(0, totalDays)
    val progress = passedDays.toFloat() / totalDays.toFloat()

    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Growth Progress", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                Surface(color = Color(0xFFF1F8F5), shape = RoundedCornerShape(8.dp)) {
                    Text("Day $passedDays of $totalDays", color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Thick Linear Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFE2E8F0), CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(AgroGreen, CircleShape))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sown: ${calendar.sowingDate}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("Est. Harvest: ${calendar.harvestDate}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TodayTaskCard(task: TaskResponse, onComplete: () -> Unit) {
    // Beautiful vertical gradient for the urgent task
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.background(Brush.verticalGradient(listOf(AgroGreen, Color(0xFF1B5E20)))).padding(24.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("URGENT TASK", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.9f), letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(task.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 30.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(task.description, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 22.sp)

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Completed", color = AgroGreen, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TimelineItem(task: TaskResponse, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Graphics Line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (task.isCompleted) AgroGreen else Color.White, CircleShape)
                    .border(2.dp, if (task.isCompleted) AgroGreen else Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(if (task.isCompleted) AgroGreen else Color(0xFFE2E8F0)))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Task Content Encased in a Card
        Surface(
            color = if (task.isCompleted) Color(0xFFF8FAFC) else Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = if (task.isCompleted) 0.dp else 2.dp,
            modifier = Modifier.weight(1f).padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Day ${task.day} • ${task.date}", fontSize = 12.sp, color = if (task.isCompleted) Color.LightGray else Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) Color.Gray else Color(0xFF0F172A),
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                )
                if (!task.isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(task.description, fontSize = 14.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCropDialog(onDismiss: () -> Unit, onGenerate: (String, String) -> Unit, isLoading: Boolean) {
    var cropName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // FIX: Set default date to TODAY automatically
    var selectedDate by remember {
        mutableStateOf(String.format(Locale.getDefault(), "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)))
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("AI Crop Calendar", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A)) },
        text = {
            Column {
                Text("Enter the crop you are planting, and AI will generate a complete day-by-day schedule.", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = { Text("Crop Name (e.g. Tomato)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FIX: Box wrapper catches the click so the DatePicker ALWAYS opens
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        label = { Text("Sowing Date") },
                        readOnly = true, // Prevents keyboard from opening
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = AgroGreen) }
                    )
                    // Invisible clickable layer exactly over the TextField
                    // Invisible clickable layer exactly over the TextField
Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (cropName.isNotEmpty() && selectedDate.isNotEmpty()) onGenerate(cropName, selectedDate) },
                enabled = cropName.isNotEmpty() && selectedDate.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AgroGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Generate Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) }
        }
    )
}