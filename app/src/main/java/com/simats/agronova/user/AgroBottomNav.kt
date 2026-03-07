package com.simats.agronova.user

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.simats.agronova.ui.theme.AgroGreen

// Define the screens to easily track the active state
enum class NavScreen {
    Home, Assistant, Tools, Profile
}

data class BottomNavItem(
    val route: NavScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AgroBottomNav(
    currentScreen: NavScreen,
    onItemSelected: (NavScreen) -> Unit
) {
    val items = listOf(
        BottomNavItem(NavScreen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(NavScreen.Assistant, "Assistant", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
        BottomNavItem(NavScreen.Tools, "Tools", Icons.Filled.Widgets, Icons.Outlined.Widgets),
        BottomNavItem(NavScreen.Profile, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Gray,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item.route) },
                label = { Text(text = item.title) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AgroGreen,
                    selectedTextColor = AgroGreen,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent // Removes default pill background
                )
            )
        }
    }
}