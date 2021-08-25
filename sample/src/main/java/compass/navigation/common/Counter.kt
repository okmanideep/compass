package compass.navigation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Counter() {
    var count by rememberSaveable { mutableStateOf(0) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement
        Button(onClick = { count-- }) {
            Text(text = "-")
        }
        // Text
        Text(text = "$count", modifier = Modifier.padding(horizontal = 10.dp))
        // Increment
        Button(onClick = { count++ }) {
            Text(text = "+")
        }
    }
}