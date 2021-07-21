package compass

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.common.Pages
import compass.common.PagesBuilder

@ExperimentalAnimationApi
@Composable
fun BottomNavHost(
    navController: NavController,
    startDestination: Page,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) = BottomNavHost(
    navController = navController,
    initialStack = listOf(startDestination),
    modifier,
    builder = builder
)

@ExperimentalAnimationApi
@Composable
fun BottomNavHost(
    navController: NavController,
    initialStack: List<Page>,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) {
    val pages = PagesBuilder().apply(builder).build()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "BottomNavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }

    LocalParentViewModelStoreOwner.provides(viewModelStoreOwner)
    val bottomNavViewModel = remember(pages, navController, viewModelStoreOwner) {
        bottomNavViewModel(pages, navController, viewModelStoreOwner)
    }

    DisposableEffect(bottomNavViewModel, navController) {
        bottomNavViewModel.setInitialStack(initialStack, false, navController)
        navController.attachNavHostController(bottomNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    val activeEntries = navController.state?.activeEntries() ?: emptyList()
    val canGoBack = navController.canGoBack

    BackHandler(enabled = canGoBack, onBack = { bottomNavViewModel.goBack() })

    Box(modifier = modifier) {
        for (entry in activeEntries) {
            entry.LocalOwnersProvider(
                parentViewModelStoreOwner = viewModelStoreOwner
            ) {
                AnimatedVisibility(
                    visible = entry.isResumed(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val pageContentComposable = pages.pageContentByType[entry.page.type]!!
                    pageContentComposable(entry.page)
                }
            }
        }
    }
}

private fun bottomNavViewModel(
    pages: Pages,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): BottomNavViewModel {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return BottomNavViewModel(pages, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(BottomNavViewModel::class.java)
}


internal class BottomNavViewModel(
    private val pages: Pages,
    private val navController: NavController
) : ViewModel(), NavHostController {
    private var navStack = BottomNavStack()
    private var listener: ((NavState) -> Unit)? = null
    var canGoBack by mutableStateOf(false)
        private set

    override fun setStateChangedListener(listener: (NavState) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(page: Page): Boolean {
        return pages.hasPageType(page)
    }

    override fun navigateTo(navEntry: NavEntry, popUpTo: Boolean) {
        navStack.addOrBringForward(navEntry = navEntry)
        onStateUpdated()
    }

    override fun canGoBack(): Boolean {
        return navStack.canGoBack()
    }

    override fun goBack(): Boolean {
        Log.e(
            "GoBackBottomNav: ",
            "Before Back ${navStack.debugLog()} canGoBack[${navStack.canGoBack()}]"
        )
        val canGoBack = navStack.canGoBack()
        if (canGoBack) {
            navStack.goBackWithPersist()
            onStateUpdated()
        }
        Log.e(
            "GoBackBottomNav: ",
            "After Back ${navStack.debugLog()} canGoBack[${navStack.canGoBack()}]"
        )
        return canGoBack
    }

    internal fun onExitCompleted() {
        cleanScopes()
    }

    private fun onStateUpdated() {
        canGoBack = navStack.canGoBack()
        listener?.invoke(navStack.toNavStackState())
    }

    /**
     * Intentionally left bank
     * this method makes no sense for Bottom Navigation
     * as VMs of the Pages loaded directly from BottomNav should persist
     * */
    private fun cleanScopes() {
        // do nothing for BottomNavHost
    }

    fun setInitialStack(initialStack: List<Page>, forceUpdate: Boolean, baseNavController: NavController) {
        if (forceUpdate || !navStack.isSameInitialStack(initialStack)) {
            navStack.clearBackStack()
            initialStack.forEach { page ->
                navigateTo(NavEntry(page = page, baseNavController = baseNavController), false)
            }
        }
    }
}


