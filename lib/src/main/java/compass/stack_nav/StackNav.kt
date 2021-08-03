package compass.stack_nav

import android.os.Parcelable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.*
import compass.Page
import compass.Pages
import compass.PagesBuilder
import compass.core.NavEntry
import compass.core.NavHostController
import compass.core.NavStack
import compass.core.NavState

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
        stackNavViewModel.setInitialStack(initialStack, false, navController)

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
            entry.LocalOwnersProvider(
                parentViewModelStoreOwner = viewModelStoreOwner
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

    override fun navigateTo(navEntry: NavEntry, pageExtras: Parcelable?, navOptions: NavOptions) {
        if (navOptions.clearTop) {
            navStack.addOrPopUpTo(navEntry = navEntry)
        } else {
            removeClosedEntries()
            navStack.add(navEntry = navEntry)
        }
        updateEntries()
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
            updateEntries()
            onStateUpdated()
        }
        Log.e("GoBackStackNav: ", "After Back ${navStack.debugLog()} canGoBack[${navStack.canPop()}]")
        return canGoBack
    }

    private fun updateEntries() {
        val entries = navStack.entries
        for (i in 0 until entries.size) {
            when {
                i < navStack.currentEntryIndex -> {
                    entries[i].setLifecycleState(navStack.capToHostLifecycle(Lifecycle.State.STARTED))
                }
                i == navStack.currentEntryIndex -> {
                    entries[i].setLifecycleState(navStack.capToHostLifecycle(Lifecycle.State.RESUMED))
                }
                else -> { // closed entries
                    entries[i].viewModelStore.clear()
                    entries[i].setLifecycleState(Lifecycle.State.DESTROYED)
                }
            }
        }
    }

    private fun removeClosedEntries() {
        val entries = navStack.entries
        while (navStack.currentEntryIndex + 1 > entries.size) {
            entries.removeLast()
        }
    }

    internal fun onExitCompleted() {

    }

    private fun onStateUpdated() {
        canGoBack = navStack.canPop()
        listener?.invoke(toNavStackState())
    }

    fun toNavStackState(): NavState {
        return StackNavState(navStack.entries.map { it })
    }

    fun setInitialStack(initialStack: List<Page>, forceUpdate: Boolean, baseNavController: NavController) {
        if (forceUpdate || !navStack.isSameInitialStack(initialStack)) {
            navStack.clearBackStack()
            updateEntries()
            removeClosedEntries()
            initialStack.forEach {
                page -> navigateTo(NavEntry(page = page, baseNavController = baseNavController), null, NavOptions(false))
            }
        }
    }
}

