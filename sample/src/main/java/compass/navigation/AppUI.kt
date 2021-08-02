package compass.navigation

import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compass.*
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
    HOME("home"), DETAIL("detail"), WATCH("watch"),
    TAB_ONE("Tab1"), TAB_TWO("Tab2"), TAB_THREE("Tab3"), TAB_FOUR("Tab4")
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
data class WatchPage(val contentId: String) : AppPage(
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
    val initialStack = listOf(
        HomePage.toPage(), DetailPage("Content Id 1").toPage()
    )
    StackNavHost(navController = navController, initialStack = initialStack) {
        page(PageType.HOME.key) {
            HomePageUI()
        }

        page(PageType.DETAIL.key) {
            DetailPageUI(Modifier, DetailPage.from(it))
        }
    }
}

@Composable
fun HomePageUI() {

    val bottomNavigationItems = listOf(
        BottomNavigationScreens.Tab1,
        BottomNavigationScreens.Tab2,
        BottomNavigationScreens.Tab3,
        BottomNavigationScreens.Tab4
    )

    val bottomNavController = getNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            XBottomNavBar(bottomNavController, bottomNavigationItems)
        }
    ) { innerPadding ->
        BottomNavHost(
            navController = bottomNavController,
            startDestination = BottomNavigationScreens.Tab1.toPage()
        ) {
            page(PageType.TAB_ONE.key) {

                Log.e("HomePageUI: ", "${LocalParentViewModelStoreOwner.current.hashCode()}")
                val detailNavController = getNavController()
                StackNavHost(
                    navController = detailNavController,
                    startDestination = BottomNavigationScreens.Tab1.toPage()
                ) {
                    page(PageType.TAB_ONE.key) {
                        TabOneUI(Modifier.padding(innerPadding))
                    }

                    page(PageType.DETAIL.key) {
                        DetailPageUI(Modifier.padding(innerPadding), DetailPage.from(it))
                    }
                }
            }


            /**
             * adding page here means telling BottomNav what all pages can be navigated by it
             * */
//            page(PageType.DETAIL.key) {
//                DetailPageUI(DetailPage.from(it))
//            }

            page(PageType.TAB_TWO.key) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colors.background)
                ) {
                    val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
                    Text(text = "Tab2 $parentVMSO", modifier = Modifier.align(Alignment.Center))
                }
            }

            page(PageType.TAB_THREE.key) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colors.background)
                ) {
                    val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
                    Text(text = "Tab3 $parentVMSO", modifier = Modifier.align(Alignment.Center))
                }
            }

            page(PageType.TAB_FOUR.key) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colors.background)
                ) {
                    val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
                    Text(text = "Tab4 $parentVMSO", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}


// TODO: 08/07/21 Why Align.Centre not accessible when extracted out as a method???
@Composable
private fun TabOneUI(modifier: Modifier) {
    val navController = getNavController()
    Column(
        modifier = modifier
            .fillMaxSize()
            .border(5.dp, Color.Red, RoundedCornerShape(5.dp))
    ) {
        val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
        Text(
            text = "Tab1 $parentVMSO", modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(15.dp))
        Button(onClick = {
            navController.navigateTo(DetailPage("27").toPage(), null, NavOptions(false))
        }, modifier = Modifier.padding(10.dp)) {
            Text(text = "Click to goto DETAILS Page")
        }
    }
}

@Composable
fun DetailPageUI(modifier: Modifier, page: DetailPage) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(5.dp, Color.Blue)
            .background(MaterialTheme.colors.background)
    ) {
        val detailNavController = getNavController()
        StackNavHost(
            navController = detailNavController, startDestination = page.toPage()
        ) {
            page(PageType.DETAIL.key) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    val detailPage = DetailPage.from(it)
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
                        Text(
                            text = "DETAIL - ${page.contentId} $parentVMSO",
                        )

                        Button(onClick = {
                            detailNavController.navigateTo(WatchPage(detailPage.contentId).toPage(), null, NavOptions(false))
                        }) {
                            Text("WATCH")
                        }
                    }
                }
            }

            page(PageType.WATCH.key) {
                WatchPageUI(page = WatchPage.from(it))
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
        val parentVMSO = LocalParentViewModelStoreOwner.current.hashCode()
        Text(text = "WATCH - ${page.contentId} $parentVMSO", modifier = Modifier.align(Alignment.Center))
    }
}

sealed class BottomNavigationScreens(val route: String) {
    fun toPage() = Page(route)

    object Tab1 : BottomNavigationScreens("Tab1")
    object Tab2 : BottomNavigationScreens("Tab2")
    object Tab3 : BottomNavigationScreens("Tab3")
    object Tab4 : BottomNavigationScreens("Tab4")
}

@Composable
private fun XBottomNavBar(
    navController: NavController,
    items: List<BottomNavigationScreens>
) {
    BottomNavigation(modifier = Modifier) {
        val currentRoute = currentRoute(navController = navController)
        items.forEach { screen ->
            BottomNavigationItem(
                label = { Text(screen.route) },
                selected = currentRoute == screen.route,
                alwaysShowLabel = true,
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(0.4f),
                icon = {},
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigateTo(
                            page = screen.toPage(), null, NavOptions(false)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun currentRoute(navController: NavController): String {
    return navController.state?.backStack?.lastOrNull { it.isResumed() }?.page?.type ?: ""
}
