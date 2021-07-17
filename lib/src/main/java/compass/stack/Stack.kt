package compass.stack

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.*
import compass.common.BackStackEntryScope
import compass.common.BackStackEntryScopeProvider
import compass.common.Pages
import compass.common.PagesBuilder

@ExperimentalAnimationApi
@Composable
fun StackNavHost(
    navController: NavController,
    startDestination: Page,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) = StackNavHost(
    navController = navController,
    initialStack = listOf(startDestination),
    modifier,
    builder = builder
)

@ExperimentalAnimationApi
@Composable
fun StackNavHost(
    navController: NavController,
    initialStack: List<Page>,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) {
    val pages = PagesBuilder().apply(builder).build()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "StackNavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }

    val stackNavViewModel = remember(pages, navController, viewModelStoreOwner) {
        stackNavViewModel(
            pages = pages,
            navController = navController,
            viewModelStoreOwner = viewModelStoreOwner
        )
    }

    DisposableEffect(stackNavViewModel, navController) {
//        for (page in initialStack) {
//            stackNavViewModel.navigateTo(page, false)
//        }
        stackNavViewModel.setInitialStack(initialStack, false)

        navController.attachNavHostController(stackNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    val activeEntries = navController.state?.activeEntries() ?: emptyList()
    val canGoBack = navController.canGoBack

    BackHandler(enabled = canGoBack, onBack = { stackNavViewModel.goBack() })

    Box(modifier = modifier) {
        for (entry in activeEntries) {
            BackStackEntryScopeProvider(
                backStackEntryScope = stackNavViewModel.scopeForEntryId(entry.id)
            ) {
                AnimatedVisibility(
                    visible = !entry.isClosing(),
                    enter = slideInHorizontally({ it }),
                    exit = fadeOut()
                ) {
                    val pageContentComposable = pages.pageContentByType[entry.page.type]!!
                    pageContentComposable(entry.page)
                }
            }
        }
    }
}

private fun stackNavViewModel(
    pages: Pages,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): StackNavViewModel {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return StackNavViewModel(pages, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(StackNavViewModel::class.java)
}

internal class StackNavViewModel(
    private val pages: Pages,
    private val navController: NavController
) : ViewModel(), NavHostController {
    private var navStack = NavStack()
    private var listener: ((NavState) -> Unit)? = null
    private val scopeByEntryId = mutableMapOf<String, BackStackEntryScope>()
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
        if (popUpTo) navStack.addOrPopUpTo(navEntry) else navStack.add(navEntry)
        onStateUpdated()
    }

    override fun canGoBack(): Boolean {
        return navStack.canPop()
    }

    override fun goBack(): Boolean {
        Log.e("GoBackStackNav: ", "Before Back ${navStack.debugLog()} canGoBack[${navStack.canPop()}]")
        val canGoBack = navStack.canPop()
        if (canGoBack) {
            navStack.pop()
            onStateUpdated()
        }
        Log.e("GoBackStackNav: ", "After Back ${navStack.debugLog()} canGoBack[${navStack.canPop()}]")
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
        canGoBack = navStack.canPop()
        listener?.invoke(navStack.toNavStackState())
    }

    /**
     * clean the scopes for items which are removed from stack
     * but present in @param [scopeByEntryId]
     * */
    private fun cleanScopes() {
        val list = mutableListOf<String>()
        for (mutableEntry in scopeByEntryId) {
            if (!navStack.contains(mutableEntry.key))
                list.add(mutableEntry.key)
        }

        for (id in list) {
            val scope = scopeByEntryId.remove(id)
            scope?.viewModelStore?.clear()
        }
    }

    override fun onCleared() {
        scopeByEntryId.values.forEach {
            it.viewModelStore.clear()
        }
        scopeByEntryId.clear()

        super.onCleared()
    }

    fun setInitialStack(initialStack: List<Page>, forceUpdate: Boolean) {
        if (forceUpdate || !navStack.isSameInitialStack(initialStack)) {
            navStack.clearBackStack()
            cleanScopes()
            initialStack.forEach {
                page -> navigateTo(NavEntry(page = page, navContext = navController.navContext), false)
            }
        }
    }
}

