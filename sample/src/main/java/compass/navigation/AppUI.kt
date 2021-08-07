package compass.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import compass.Page
import compass.getNavController
import compass.stack.StackNavHost

enum class RootPage(val key: String) {
    SPLASH("splash"),
    MAIN("main"),
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun App() {
    val navController = getNavController()
    val initialStack = listOf(
        Page(RootPage.SPLASH.key)
    )
    StackNavHost(navController = navController, initialStack = initialStack) {
        page(RootPage.SPLASH.key) {
            SplashPageUI()
        }
        page(RootPage.MAIN.key) {
            MainPageUI()
        }
    }
}
