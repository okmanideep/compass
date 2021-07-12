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
import compass.common.BackStackEntryScope
import compass.common.BackStackEntryScopeProvider
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

    val bottomNavViewModel = remember(pages, navController, viewModelStoreOwner) {
        bottomNavViewModel(pages, navController, viewModelStoreOwner)
    }

    // TODO: 06/07/21 -- On enabling this logic, bottom tab selection messes up ????
//    bottomNavViewModel.setStateChangedListener {
////        Log.e("BottomNavHost: ", it.debugLog())
//    }

    DisposableEffect(bottomNavViewModel, navController) {
//        for (page in initialStack) {
//            bottomNavViewModel.navigateTo(page, false)
//        }
        bottomNavViewModel.setInitialStack(initialStack, false)
        navController.attachNavHostController(bottomNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    val activeEntries = navController.state.activeEntries()
    val canGoBack = navController.canGoBack

    BackHandler(enabled = canGoBack, onBack = { bottomNavViewModel.goBack() })

    Box(modifier = modifier) {
        for (entry in activeEntries) {
            BackStackEntryScopeProvider(
                backStackEntryScope = bottomNavViewModel.scopeForEntryId(entry.id)
            ) {
                AnimatedVisibility(
                    visible = !entry.isClosing,
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
    private var navStack = NavStack(emptyList())
    private var listener: ((NavStackState) -> Unit)? = null
    private val scopeByEntryId = mutableMapOf<String, BackStackEntryScope>()
    var canGoBack by mutableStateOf(false)
        private set

    override fun setStateChangedListener(listener: (NavStackState) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(page: Page): Boolean {
        return pages.hasPageType(page)
    }

    override fun navigateTo(page: Page, popUpTo: Boolean) {
        navStack = if (popUpTo) navStack.addOrPopUpTo(page) else navStack.addOrBringForward(page)

        onStateUpdated()
    }

    override fun canGoBack(): Boolean {
        return navStack.canGoBack()
    }

    override fun goBack(): Boolean {
        Log.e("GoBackBottomNav: ", "Before Back ${navStack.debugLog()} canGoBack[${navStack.canGoBack()}]")
        val canGoBack = navStack.canGoBack()
        if (canGoBack) {
            navStack = navStack.pop()
            onStateUpdated()
        }
        Log.e("GoBackBottomNav: ", "After Back ${navStack.debugLog()} canGoBack[${navStack.canGoBack()}]")
        return canGoBack
    }

    internal fun onExitCompleted() {
        cleanScopes()
    }

    internal fun scopeForEntryId(entryId: String): BackStackEntryScope {
        return scopeByEntryId[entryId]
            ?: createScopeForEntryId(entryId)
    }

    private fun createScopeForEntryId(entryId: String): BackStackEntryScope {
        val scope = BackStackEntryScope(navController)
        scopeByEntryId[entryId] = scope

        return scope
    }

    private fun onStateUpdated() {
        canGoBack = navStack.canGoBack()
        listener?.invoke(navStack.toNavStackState())
    }

    /**
     * Intentionally left bank
     * this method makes no sense for Bottom Navigation
     * as VMs of the Pages loaded directly from BottomNav should persist
     * even when they are removed from the stack
     * */
    private fun cleanScopes() {
        // do nothing for BottomNavHost
    }

    /**
     * this method is called when Page holding the BottomNav is popped
     * hence clear all values to avoid memory leaks
     * */
    override fun onCleared() {
        scopeByEntryId.values.forEach {
            it.viewModelStore.clear()
        }

        super.onCleared()
    }

    fun setInitialStack(initialStack: List<Page>, forceUpdate: Boolean) {
        if (forceUpdate || !navStack.isSameInitialStack(initialStack)) {
            navStack.clearBackStack()
            initialStack.forEach {
                    page -> navigateTo(page = page, false)
            }
        }
    }
}


