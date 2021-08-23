package compass.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@ExperimentalFoundationApi
@Composable
fun ExploreUI() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchBar()
        LazyVerticalGrid(
            cells = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(100) {
                GridCell(it + 1)
            }
        }
    }

}

@Composable
private fun GridCell(it: Int) {
    Surface(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                it.toString(),
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.surface,
                    shadow = Shadow(MaterialTheme.colors.background, blurRadius = 8f)
                )
            )
        }
    }
}

@Composable
fun SearchBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp)),
        elevation = 2.dp
    ) {}
}

