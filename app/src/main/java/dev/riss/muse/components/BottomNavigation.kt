package dev.riss.muse.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isWideScreen = screenWidth > 600

    val items = listOf(
        Triple("discovery", "Discovery", Icons.Default.Search),
        Triple("main", "Home", Icons.Default.Home),
        Triple("profile", "Profile", Icons.Default.Person)
    )

    if (isWideScreen) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = modifier
                .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                .fillMaxHeight()
        ) {
            Spacer(Modifier.weight(1f))
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                NavigationRailItem(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(2.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            label,
                            style = if (selected) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                                    else MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = selected,
                    onClick = { navController.navigate(route) }
                )
            }
            Spacer(Modifier.weight(1f))
        }
    } else {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = modifier
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            items.forEach { (route, label, icon) ->
                val selected = currentRoute == route
                NavigationBarItem(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(2.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            label,
                            style = if (selected) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                                    else MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = selected,
                    onClick = { navController.navigate(route) }
                )
            }
        }
    }
} 