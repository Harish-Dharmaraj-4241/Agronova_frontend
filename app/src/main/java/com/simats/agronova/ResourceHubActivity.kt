package com.simats.agronova

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.agronova.model.ResourceItem
import com.simats.agronova.ui.theme.AgroGreen
import com.simats.agronova.ui.theme.AgronovaTheme
import com.simats.agronova.viewmodel.ResourceHubViewModel

class ResourceHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgronovaTheme {
                ResourceHubScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceHubScreen(
    viewModel: ResourceHubViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resources by viewModel.resources
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var showPostDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ResourceItem?>(null) }

    // Added "My Posts" to the top of the filters
    val categories = listOf("All", "My Posts", "Machinery", "Pesticides", "Seeds/Fertilizer", "Transport", "Other")

    LaunchedEffect(Unit) {
        viewModel.fetchLocalResources(context)
    }

    // Filter Logic: Separate your posts from the public marketplace
    val filteredList = resources.filter { item ->
        val matchesSearch = item.itemName.contains(searchQuery, ignoreCase = true)

        if (selectedCategory == "My Posts") {
            item.userEmail == viewModel.currentUserEmail && matchesSearch
        } else {
            // Public Feed: Hide items I posted AND hide items that are Out of Stock
            val isOthersAndAvailable = item.userEmail != viewModel.currentUserEmail && item.isAvailable
            val matchesCat = selectedCategory == "All" || item.category == selectedCategory
            isOthersAndAvailable && matchesCat && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Resource Hub", fontWeight = FontWeight.ExtraBold, color = AgroGreen, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AgroGreen) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPostDialog = true },
                containerColor = AgroGreen,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, "Post") },
                text = { Text("Post Resource", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar & Filter Chips
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search resources...") },
                        leadingIcon = { Icon(Icons.Filled.Search, tint = Color.Gray, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgroGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) AgroGreen else Color(0xFFE2E8F0),
                                modifier = Modifier.clickable { selectedCategory = category }
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // List of Resources
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AgroGreen)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = Color.Red, fontWeight = FontWeight.Medium)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (selectedCategory == "My Posts") Icons.Filled.Inventory else Icons.Filled.Storefront, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (selectedCategory == "My Posts") "You haven't posted anything yet." else "No resources found nearby.", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { item ->
                        val isMyPost = item.userEmail == viewModel.currentUserEmail
                        ResourceCard(
                            item = item,
                            isMyPost = isMyPost,
                            onEdit = { itemToEdit = item },
                            onDelete = { viewModel.deleteResource(context, item.id) },
                            onToggleAvailability = { available ->
                                viewModel.editResource(context, item.id, item.itemName, item.category, available)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
                }
            }
        }

        if (showPostDialog) {
            PostResourceDialog(
                onDismiss = { showPostDialog = false },
                onPost = { name, cat ->
                    viewModel.postResource(context, name, cat) { showPostDialog = false }
                }
            )
        }

        if (itemToEdit != null) {
            EditResourceDialog(
                item = itemToEdit!!,
                onDismiss = { itemToEdit = null },
                onSave = { id, name, cat ->
                    viewModel.editResource(context, id, name, cat, itemToEdit!!.isAvailable)
                    itemToEdit = null
                }
            )
        }
    }
}

@Composable
fun ResourceCard(
    item: ResourceItem,
    isMyPost: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val (icon, bgColor, iconColor) = when (item.category) {
        "Machinery" -> Triple(Icons.Filled.Agriculture, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "Pesticides" -> Triple(Icons.Filled.Science, Color(0xFFE3F2FD), Color(0xFF1565C0))
        "Seeds/Fertilizer" -> Triple(Icons.Filled.EnergySavingsLeaf, Color(0xFFFFF8E1), Color(0xFFF57F17))
        "Transport" -> Triple(Icons.Filled.LocalShipping, Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        else -> Triple(Icons.Filled.Category, Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(bgColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!item.isAvailable && isMyPost) Color.Gray else Color(0xFF0F172A),
                    textDecoration = if (!item.isAvailable && isMyPost) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!isMyPost) {
                    Text(item.ownerName, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AgroGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("${item.distanceKm} KM AWAY", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AgroGreen)
                    }
                } else {
                    Text(item.category, fontSize = 13.sp, color = Color.Gray)
                }
            }

            // Dynamic Management Options if it's MY POST
            if (isMyPost) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Switch(
                            checked = item.isAvailable,
                            onCheckedChange = { onToggleAvailability(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AgroGreen, checkedTrackColor = AgroGreen.copy(alpha = 0.5f))
                        )
                        Text(if (item.isAvailable) "Available" else "Hidden", fontSize = 10.sp, color = if (item.isAvailable) AgroGreen else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Surface(
                    color = AgroGreen,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.ownerPhone}"))
                        context.startActivity(intent)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostResourceDialog(onDismiss: () -> Unit, onPost: (String, String) -> Unit) {
    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Machinery") }
    val categories = listOf("Machinery", "Pesticides", "Seeds/Fertilizer", "Transport", "Other")
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Post a Resource", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A)) },
        text = {
            Column {
                Text("List your items for neighbors to rent or buy. You will negotiate the price over the phone.", color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("What are you offering?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expandedMenu, onExpandedChange = { expandedMenu = !expandedMenu }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(text = { Text(selectionOption) }, onClick = { selectedCategory = selectionOption; expandedMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotEmpty()) onPost(itemName, selectedCategory) },
                enabled = itemName.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = AgroGreen), modifier = Modifier.height(45.dp)
            ) { Text("Post Item", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditResourceDialog(item: ResourceItem, onDismiss: () -> Unit, onSave: (Int, String, String) -> Unit) {
    var itemName by remember { mutableStateOf(item.itemName) }
    var selectedCategory by remember { mutableStateOf(item.category) }
    val categories = listOf("Machinery", "Pesticides", "Seeds/Fertilizer", "Transport", "Other")
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Edit Resource", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A)) },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(expanded = expandedMenu, onExpandedChange = { expandedMenu = !expandedMenu }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(text = { Text(selectionOption) }, onClick = { selectedCategory = selectionOption; expandedMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotEmpty()) onSave(item.id, itemName, selectedCategory) },
                enabled = itemName.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = AgroGreen), modifier = Modifier.height(45.dp)
            ) { Text("Save Changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}