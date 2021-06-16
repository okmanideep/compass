package compass.navigation

import android.os.Parcelable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

enum class PageType(val key: String){
    HOME("home"), DETAIL("detail"), WATCH("watch")
}

object HomePage: AppPage(
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
data class DetailPage(val contentId: String): AppPage(
    pageType = PageType.DETAIL
), Parcelable {
    companion object {
        fun from(page: Page): DetailPage {
            return page.args as DetailPage
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
data class WatchPage(val contentId: String): AppPage(
    pageType = PageType.WATCH
), Parcelable {
    companion object {
        fun from(page: Page): WatchPage {
            return page.args as WatchPage
        }
    }

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
    StackNavHost(navController = navController, startDestination = HomePage.toPage()) {
        page(PageType.HOME.key) {
            HomePageUI()
        }

        page(PageType.DETAIL.key) {
            DetailPageUI(DetailPage.from(it))
        }

        page(PageType.WATCH.key) {
            WatchPageUI(WatchPage.from(it))
        }
    }
}

@Composable
fun HomePageUI() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "HOME", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun DetailPageUI(page: DetailPage) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "DETAIL - ${page.contentId}", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun WatchPageUI(page: WatchPage) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "WATCH - ${page.contentId}", modifier = Modifier.align(Alignment.Center))
    }
}
