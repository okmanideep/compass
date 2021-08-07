package compass.navigation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun FullWidthImage(index: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = 2.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                index.toString(),
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.surface,
                    shadow = Shadow(MaterialTheme.colors.background, blurRadius = 8f)
                )
            )
        }
    }
}

@Composable
fun HomeFeedItem(index: Int = (Math.random()*100).roundToInt()) {
    Column {
        FeedTitle()
        FullWidthImage(index)
    }
}

@Composable
private fun FeedTitle() {
    Row() {
        Surface(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .width(32.dp)
                .height(32.dp),
            elevation = 2.dp
        ) {}
        Surface(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .width(128.dp)
                .height(18.dp),
            elevation = 2.dp
        ) {}
    }
}

@Composable
fun HomeFeed() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(100, key = { it }) {
            HomeFeedItem(it + 1)
            Spacer(Modifier.height(16.dp))
        }
    }
}