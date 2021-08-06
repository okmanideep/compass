package compass.stack

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.*
import compass.core.StackInternal

internal class Graph(
    private val destinationByType: Map<String, Destination>
) {
    fun destinationFor(pageType: String): Destination {
        return destinationByType[pageType]
            ?: throw IllegalArgumentException("No destination for $pageType")
    }

    fun hasPageType(pageType: String): Boolean {
        return destinationByType.containsKey(pageType)
    }
}

internal class Destination(
    val content: @Composable (NavEntry) -> Unit,
    val isTransparent: Boolean,
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
)

class GraphBuilder
internal constructor(
    private val destinationByType: HashMap<String, Destination> = HashMap(),
) {
    fun page(
        type: String,
        isTransparent: Boolean = false,
        enterTransition: EnterTransition = slideInHorizontally({ it }),
        exitTransition: ExitTransition = slideOutHorizontally({ it }),
        content: @Composable (NavEntry) -> Unit,
    ) {
        destinationByType[type] = Destination(
            content, isTransparent, enterTransition, exitTransition
        )
    }

    internal fun build() = Graph(destinationByType)
}

@ExperimentalAnimationApi
@Composable
fun StackNavHost(
    navController: NavController,
    startDestination: Page,
    modifier: Modifier = Modifier,
    builder: GraphBuilder.() -> Unit
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
    builder: GraphBuilder.() -> Unit
) {
    val graph = GraphBuilder().apply(builder).build()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "StackNavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }

    val stackNavViewModel = remember(graph, navController, viewModelStoreOwner) {
        stackNavViewModel(
            graph = graph,
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
            stackNavViewModel.clearStack()
            navController.detachNavHostController()
        }
    }

    BackHandler(enabled = navController.canGoBack, onBack = { stackNavViewModel.goBack() })

    PageStack(
        backStack = navController.state,
        graph = graph,
        modifier = modifier
    )
}

@Composable
private fun PageStack(
    backStack: NavBackStack,
    graph: Graph,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(backStack, label = "Page Stack Transition")

    Box(modifier = modifier) {
        // bottom entry
        transition.AnimatedContent(
            transitionSpec = { EnterTransition.None with ExitTransition.None }
        ) {
            val topEntry = it.entries.lastOrNull()
            val topDestination = if (topEntry != null) {
                graph.destinationFor(topEntry.pageType)
            } else {
                null
            }

            if (topDestination?.isTransparent == true) {
                check(it.entries.size > 1) { "First item in the stack can't be transparent" }

                val bottomEntry = it.entries[it.entries.size - 2]
                bottomEntry.Render(graph)
            }
        }

        // top entry
        transition.AnimatedContent(
            transitionSpec = {
                val initialTop = initialState.entries.lastOrNull()
                val targetTop = targetState.entries.lastOrNull()

                val enter = if (targetTop != null && !initialState.hasEntry(targetTop)) {// entering
                    graph.destinationFor(targetTop.pageType).enterTransition
                } else {
                    // EnterTransition.None basically doesn't draw the content
                    fadeIn(0.5f)
                }

                val exit = if (initialTop != null && !targetState.hasEntry(initialTop)) {
                    graph.destinationFor(initialTop.pageType).exitTransition
                } else {
                    // ExitTransition.None basically doesn't draw the content
                    fadeOut(0.5f)
                }

                enter with exit
            }
        ) {
            it.entries.lastOrNull()?.Render(graph)
        }
    }
}

private fun NavBackStack.hasEntry(entry: NavEntry): Boolean {
    return entries.contains(entry)
}

@Composable
private fun NavEntry.Render(graph: Graph) {
    LocalOwnersProvider {
        graph.destinationFor(pageType).content(this)
    }
}

private fun stackNavViewModel(
    graph: Graph,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): StackNavViewModel {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return StackNavViewModel(graph, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(StackNavViewModel::class.java)
}

internal class StackNavViewModel(
    private val graph: Graph,
    private val navController: NavController
) : ViewModel(), NavHostController, LifecycleEventObserver {
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    private val stack = StackInternal<NavEntry>()
    private var listener: ((NavBackStack) -> Unit)? = null

    /**
     * On Host Lifecycle Changed
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        hostLifecycleState = source.lifecycle.currentState

        updateState()
    }

    override fun setStateChangedListener(listener: (NavBackStack) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(pageType: String): Boolean {
        return graph.hasPageType(pageType)
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
        clearStack()

        super.onCleared()
    }

    fun clearStack() {
        for (entry in stack.toList()) {
            entry.viewModelStore.clear()
            entry.setLifecycleState(Lifecycle.State.DESTROYED)
        }

        stack.clear()
    }

    private fun onStateUpdated() {
        listener?.invoke(NavBackStack(stack.toList()))
    }

    private fun updateState() {
        val entries = stack.toList()
        for (i in entries.indices) {
            when {
                i < entries.size - 1 -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.STARTED))
                }
                i == entries.size - 1 -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.RESUMED))
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