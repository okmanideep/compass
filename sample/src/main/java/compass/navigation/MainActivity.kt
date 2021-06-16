package compass.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import compass.NavController
import compass.NavControllerRegistry
import compass.RootNavContextProvider
import compass.navigation.ui.theme.CompassTheme

class MainActivity : ComponentActivity(), NavControllerRegistry {
    private var rootNavController: NavController? = null

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootNavContextProvider {
                CompassTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        App()
                    }
                }
            }
        }
    }

    override fun registerNavController(navController: NavController) {
        rootNavController = navController
    }

    override fun unregisterNavController(navController: NavController) {
        check(rootNavController === navController)
        rootNavController = null
    }

    override fun onBackPressed() {
        if (rootNavController?.goBack() == false) {
            super.onBackPressed()
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompassTheme {
        Greeting("Android")
    }
}