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
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.*
import compass.core.StackInternal

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
) : ViewModel(), NavHostController, LifecycleEventObserver {
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    private val stack = StackInternal<NavEntry>()
    private var listener: ((NavState) -> Unit)? = null
    var canGoBack by mutableStateOf(false)
        private set

    /**
     * On Host Lifecycle Changed
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        hostLifecycleState = source.lifecycle.currentState

        updateState()
    }

    override fun setStateChangedListener(listener: (NavState) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(pageType: String): Boolean {
        return pages.hasPageType(pageType)
    }

    override fun navigateTo(pageType: String, args: Parcelable?, replace: Boolean) {
        val entry = NavEntry(
            pageType = pageType,
            args = args,
            parentNavController = navController
        )
        if (replace) {
            stack.replace(entry)
        } else {
            stack.removePoppedEntries()
            stack.add(entry)
        }
        updateState()
    }

    override fun canGoBack(): Boolean {
        return stack.canPop()
    }

    override fun goBack(): Boolean {
        val canGoBack = stack.canPop()
        if (canGoBack) {
            stack.pop()
            updateState()
        }

        return canGoBack
    }

    override fun onCleared() {
        for (entry in stack.asList()) {
            entry.viewModelStore.clear()
            entry.setLifecycleState(Lifecycle.State.DESTROYED)
        }

        super.onCleared()
    }

    private fun onStateUpdated() {
        canGoBack = stack.canPop()
        listener?.invoke(NavState(stack.asList()))
    }

    private fun updateState() {
        val entries = stack.asList()
        for (i in entries.indices) {
            when {
                i < stack.currentEntryIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.STARTED))
                }
                i == stack.currentEntryIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.RESUMED))
                }
                else -> { // closed entries
                    entries[i].viewModelStore.clear()
                    entries[i].setLifecycleState(Lifecycle.State.DESTROYED)
                }
            }
        }

        onStateUpdated()
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }
}