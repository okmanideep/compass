package compass.navigation

import android.os.Parcelable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import compass.NavController
import compass.NavEntry
import compass.Page
import compass.getNavController
import compass.stack.StackNavHost
import kotlinx.parcelize.Parcelize

sealed class AppPage(
    val pageType: PageType,
) {
    fun toPage() = Page(pageType.key, args())

    abstract fun toBreadCrumbLabel(): String
    abstract fun args(): Parcelable?
}

enum class PageType(val key: String) {
    HOME("home"), DETAIL("detail"), WATCH("watch")
}

object HomePage : AppPage(
    pageType = PageType.HOME,
) {
    override fun toBreadCrumbLabel(): String {
        return "home"
    }

    override fun args(): Parcelable? {
        return null
    }
}

@Parcelize
data class DetailPage(val contentId: String) : AppPage(
    pageType = PageType.DETAIL
), Parcelable {
    companion object {
        fun from(entry: NavEntry): DetailPage {
            return entry.args as DetailPage
        }
    }

    override fun toBreadCrumbLabel(): String {
        return "detail($contentId)"
    }

    override fun args(): Parcelable {
        return this
    }
}

@Parcelize
data class WatchPage(val contentId: String) : AppPage(
    pageType = PageType.WATCH
), Parcelable {

    override fun toBreadCrumbLabel(): String {
        return "watch($contentId)"
    }

    override fun args(): Parcelable {
        return this
    }
}

@ExperimentalAnimationApi
@Composable
fun App() {
    val navController = getNavController()
    val initialStack = listOf(
        HomePage.toPage(), DetailPage("Content Id 1").toPage()
    )
    StackNavHost(navController = navController, initialStack = initialStack) {
        page(PageType.HOME.key) {
            HomePageUI()
        }

        page(PageType.DETAIL.key) {
            DetailPageUI(it.args as DetailPage, navController)
        }

        page(PageType.WATCH.key) {
            WatchPageUI(page = it.args as WatchPage)
        }
    }
}

@Composable
fun HomePageUI() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Text(text = "HOME", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun DetailPageUI(
    page: DetailPage,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = "DETAIL - ${page.contentId}",
                )

                Button(onClick = {
                    val watchPage = WatchPage(page.contentId)
                    navController.navigateTo(
                        watchPage.pageType.key,
                        watchPage,
                        false
                    )
                }) {
                    Text("WATCH")
                }
            }
        }
    }

}

@Composable
fun WatchPageUI(page: WatchPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Text(text = "WATCH - ${page.contentId}", modifier = Modifier.align(Alignment.Center))
    }
}
