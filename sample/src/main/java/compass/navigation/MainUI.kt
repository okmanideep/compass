package compass.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import compass.getNavController
import compass.navigation.home.HomeFeed
import compass.tab.TabNavHost

enum class MainTab(
    val key: String,
    val icon: ImageVector,
) {
    HOME("home", Icons.Filled.Home),
    EXPLORE("explore", Icons.Filled.Search),
    PROFILE("profile", Icons.Filled.Person)
}

@Composable
fun MainPageUI() {
    val navController = getNavController()
    val selectedTab = navController.state.entries.lastOrNull()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabUI(modifier = Modifier.weight(1.0f))
        BottomNavigation() {
            MainTab.values().forEach {
                BottomNavigationItem(
                    icon = { Icon(it.icon, it.key) },
                    selected = selectedTab?.pageType == it.key,
                    onClick = {
                        navController.navigateTo(it.key)
                    }
                )
            }
        }
    }

}

@Composable
fun TabUI(modifier: Modifier = Modifier) {
    TabNavHost(
        modifier = modifier
    ) {
        tab(
            type = MainTab.HOME.key,
            isDefault = true,
        ) {
            HomeFeed()
        }

        tab(type = MainTab.EXPLORE.key) {
            WIPTab("Explore")
        }

        tab(type = MainTab.PROFILE.key) {
            WIPTab("Profile")
        }
    }
}

@Composable
fun WIPTab(text: String) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text,
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                    shadow = Shadow(Color.Black, blurRadius = 16f)
                )
            )
        }
    }
}