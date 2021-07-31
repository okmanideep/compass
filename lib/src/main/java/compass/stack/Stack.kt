package compass.stack

import android.os.Parcelable
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
import compass.core.NavStack

internal class Pages(
    val pageContentByType: Map<String, @Composable (NavEntry) -> Unit>
) {

    fun hasPageType(pageType: String): Boolean {
        return pageContentByType.containsKey(pageType)
    }
}

class PagesBuilder(
    private val pageContentByType: HashMap<String, @Composable (NavEntry) -> Unit> = HashMap()
) {
    fun page(type: String, content: @Composable (NavEntry) -> Unit) {
        pageContentByType[type] = content
    }

    internal fun build() = Pages(pageContentByType)
}

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
        for (page in initialStack) {
            stackNavViewModel.navigateTo(page.type, page.args, false)
        }

        navController.attachNavHostController(stackNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    val activeEntries = navController.state.activeEntries()
    val canGoBack = navController.canGoBack

    BackHandler(enabled = canGoBack, onBack = { stackNavViewModel.goBack() })

    Box(modifier = modifier) {
        for (entry in activeEntries) {
            entry.LocalOwnersProvider {
                AnimatedVisibility(
                    visible = entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
                    enter = slideInHorizontally({ it }),
                    exit = fadeOut()
                ) {
                    val pageContentComposable = pages.pageContentByType[entry.pageType]!!
                    pageContentComposable(entry)
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

    override fun canNavigateTo(pageType: String): Boolean {
        return pages.hasPageType(pageType)
    }

    override fun navigateTo(pageType: String, args: Parcelable?, replace: Boolean) {
        if (replace) navStack.pop()
        val entry = NavEntry(
            pageType = pageType,
            args = args,
            parentNavController = navController
        )
        navStack.add(entry)

        onStateUpdated()
    }

    override fun canGoBack(): Boolean {
        return navStack.canPop()
    }

    override fun goBack(): Boolean {
        val canGoBack = navStack.canPop()
        if (canGoBack) {
            navStack.pop()
            onStateUpdated()
        }

        return canGoBack
    }

    private fun onStateUpdated() {
        canGoBack = navStack.canPop()
        listener?.invoke(NavState(navStack.asList()))
    }

    private fun cleanScopes() {
        //TODO: clean and clear scopes in scopeByEntryId based on back stack
    }

    override fun onCleared() {
        //TODO: clear all view models

        super.onCleared()
    }
}