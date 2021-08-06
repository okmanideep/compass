package compass.tab

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

internal class TabGraph(
    val defaultTab: String,
    val defaultTabArgs: Parcelable?,
    val startTransition: ContentTransform,
    val endTransition: ContentTransform,
    val tabs: List<String>,
    private val destinationByType: Map<String, TabDestination>,
) {
    fun destinationFor(pageType: String): TabDestination {
        return destinationByType[pageType]
            ?: throw IllegalArgumentException("No destination for $pageType")
    }

    fun hasPageType(pageType: String): Boolean {
        return destinationByType.containsKey(pageType)
    }
}

internal class TabDestination(
    val content: @Composable (NavEntry) -> Unit,
)

class TabGraphBuilder
internal constructor(
    private val destinationByType: HashMap<String, TabDestination> = HashMap(),
    private val tabs: ArrayList<String> = ArrayList(),
) {
    private lateinit var defaultTab: String
    private var defaultTabArgs: Parcelable? = null

    fun tab(
        type: String,
        isDefault: Boolean = false,
        defaultTabArgs: Parcelable? = null,
        content: @Composable (NavEntry) -> Unit,
    ) {
        require (tabs.none { it == type }) { "Tab of type: $type already exists" }

        if (isDefault || !this::defaultTab.isInitialized){
            defaultTab = type
            this.defaultTabArgs = defaultTabArgs
        }
        tabs.add(type)
        destinationByType[type] = TabDestination(content)
    }

    internal fun build(
        startTransition: ContentTransform,
        endTransition: ContentTransform,
    ): TabGraph {
        return TabGraph(
            defaultTab,
            defaultTabArgs,
            startTransition,
            endTransition,
            tabs,
            destinationByType
        )
    }
}

@Composable
fun TabNavHost(
    navController: NavController,
    modifier: Modifier = Modifier,
    startTransition: ContentTransform
        = slideInHorizontally() + fadeIn(0.8f)
            with slideOutHorizontally( {it/2}) + fadeOut(0.8f),
    endTransition: ContentTransform
        = slideInHorizontally({it/2}) + fadeIn(0.8f)
            with slideOutHorizontally( {-it/2}) + fadeOut(0.8f),
    builder: TabGraphBuilder.() -> Unit
) {
    val graph = remember { TabGraphBuilder().apply(builder).build(startTransition, endTransition) }
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "TabNavHost requires a ViewModelStoreOwner to be provided via a LocalViewModelStoreOwner"
    }

    val tabNavViewModel = remember(graph, navController, viewModelStoreOwner) {
        tabNavViewModel(graph, navController, viewModelStoreOwner)
    }

    DisposableEffect(tabNavViewModel, navController) {
        navController.attachNavHostController(tabNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    BackHandler(enabled = navController.canGoBack, onBack = { tabNavViewModel.goBack() } )

    TabStack(
        backStack = navController.state,
        graph = graph,
        modifier = modifier,
    )
}

@Composable
private fun TabStack(
    backStack: NavBackStack,
    graph: TabGraph,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(backStack, label = "Tab Stack Transition")

    Box(modifier = modifier) {
        transition.AnimatedContent(
            transitionSpec = {
                val prevTab = initialState.entries.last().pageType
                val nextTab = targetState.entries.last().pageType

                val tabs = graph.tabs
                val prevTabIndex = tabs.indexOf(prevTab)
                val nextTabIndex = tabs.indexOf(nextTab)

                if (nextTabIndex > prevTabIndex) {
                    graph.endTransition
                } else {
                    graph.startTransition
                }
            }
        ) {
            it.entries.lastOrNull()?.Render(graph)
        }
    }
}

@Composable
private fun NavEntry.Render(graph: TabGraph) {
    LocalOwnersProvider {
        graph.destinationFor(pageType).content(this)
    }
}

private fun tabNavViewModel(
    graph: TabGraph,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): TabNavViewModel {
    val factory = object: ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TabNavViewModel(graph, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(TabNavViewModel::class.java)
}

internal class TabNavViewModel(
    private val graph: TabGraph,
    private val navController: NavController,
): ViewModel(), NavHostController, LifecycleEventObserver {
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    private val stack = StackInternal<NavEntry>()
    private var listener: ((NavBackStack) -> Unit)? = null
    private val tabToEntryMap: HashMap<String, NavEntry> = HashMap()
    private val defaultEntry: NavEntry = getEntry(
        type = graph.defaultTab,
        args = graph.defaultTabArgs
    )

    init {
        stack.add(defaultEntry)
    }

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
        if (tabToEntryMap.containsKey(pageType)) {
            stack.bringToFront(tabToEntryMap[pageType]!!)
        } else {
            stack.add(getEntry(pageType, args))
        }
    }

    override fun canGoBack(): Boolean {
        return stack.canPop() || (graph.defaultTab != stack.peek()?.pageType)
    }

    override fun goBack(): Boolean {
        when {
            stack.canPop() -> {
                stack.pop()
            }
            graph.defaultTab != stack.peek()?.pageType -> {
                stack.replace(defaultEntry)
            }
            else -> {
                return false
            }
        }

        updateState()
        return true
    }

    private fun updateState() {
        val currentPageType = stack.peek()?.pageType ?: return

        for (entry in tabToEntryMap.values) {
            val state = if (entry.pageType == currentPageType) {
                Lifecycle.State.RESUMED
            } else {
                Lifecycle.State.STARTED
            }
            entry.setLifecycleState(capToHostLifecycle(state))
        }

        onStateUpdated()
    }

    private fun onStateUpdated() {
        val stackEntries = stack.toList()
        val entries = if (defaultEntry == stackEntries.firstOrNull()) {
            stackEntries
        } else {
            listOf(defaultEntry) + stackEntries
        }

        listener?.invoke(NavBackStack(entries))
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }

    private fun getEntry(type: String, args: Parcelable?): NavEntry {
        val currentEntry = tabToEntryMap[type]
        if (currentEntry != null) return currentEntry

        val entry = NavEntry(
            pageType = type,
            parentNavController = navController,
            args = args,
        )
        tabToEntryMap[type] = entry
        return entry
    }
}