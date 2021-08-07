package compass.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import compass.getNavController
import kotlinx.coroutines.delay

@Composable
fun SplashPageUI() {
    val loadingText = remember { mutableStateOf("...") }
    Surface() {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = loadingText.value,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    if (targetState.length > initialState.length) {
                        slideIntoContainer(AnimatedContentScope.SlideDirection.Start) with
                                slideOutOfContainer(AnimatedContentScope.SlideDirection.Start)
                    } else {
                        slideIntoContainer(AnimatedContentScope.SlideDirection.End) with
                                slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
                    }
                },
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    it,
                    style = TextStyle(
                        fontSize = 128.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                        shadow = Shadow(Color.Black, blurRadius = 16f)
                    )
                )
            }
        }
    }

    val navController = getNavController()

    LaunchedEffect(navController) {
        delay(1000)
        loadingText.value = ".."
        delay(1000)
        loadingText.value = "."
        delay(1000)
        navController.navigateTo(RootPage.MAIN.key, replace = true)
    }
}
