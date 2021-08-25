package compass.navigation

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import compass.Page
import compass.getNavController
import compass.navigation.common.BigColumn
import compass.navigation.common.Counter
import compass.stack.StackNavHost

@Composable
fun Sample2() {
    val navController = getNavController()
    StackNavHost(navController = navController, startDestination = Page("A")) {
        page("A") {
            BigColumn(color = Color.Red) {
                Counter()

                Button(onClick = { navController.navigateTo("B") }) {
                    Text(text = "GO TO B")
                }
            }
        }

        page("B") {
            BigColumn(color = Color.Green) {
                Counter()
            }
        }
    }
}