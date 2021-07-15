package compass

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable

@Composable
fun getNavController(): NavController {
    val navContext = LocalNavContext.current
        ?: remember {
            object : NavContext {
                override fun owner(): NavController? {
                    return null
                }

                override fun controller(): NavController {
                    return NavController(this)
                }
            }
        }

    return navContext.controller()
}

class NavController(val navContext: NavContext) {
    private var hostController: NavHostController? = null
    var state by mutableStateOf(NavStackState())
        private set
    var canGoBack by mutableStateOf(false)
        private set

    fun attachNavHostController(navHostController: NavHostController) {
        check(hostController == null) {
            "A NavHostController is already attached. Detach it before attaching another"
        }

        hostController = navHostController
        navHostController.setStateChangedListener { state ->
            this.state = state
            this.canGoBack = navHostController.canGoBack()
            Log.e("StateUpdated: $navHostController", state.debugLog())
        }
    }

    fun detachNavHostController() {
        hostController = null
    }

    fun navigateTo(navEntry: NavEntry, popUpTo: Boolean) {
        hostController?.let {
            if (it.canNavigateTo(navEntry.page)) {
                it.navigateTo(navEntry, popUpTo)
            } else {
                navContext.owner()?.navigateTo(navEntry, popUpTo)
            }
        } ?: navContext.owner()?.navigateTo(navEntry, popUpTo)
    }

    fun goBack() {
        if (hostController?.goBack() != true) {
            navContext.owner()?.goBack()
        }
    }
}

interface NavHostController {
    fun setStateChangedListener(listener: (NavStackState) -> Unit)
    fun canNavigateTo(page: Page): Boolean
    fun navigateTo(navEntry: NavEntry, popUpTo: Boolean = false)
    fun canGoBack(): Boolean
    fun goBack(): Boolean
}

interface NavContext {
    fun owner(): NavController?
    fun controller(): NavController
}

val LocalNavContext = compositionLocalOf<NavContext?> { null }

class NavEntry(
    val id: String = UUID.randomUUID().toString(),
    val page: Page,
    val navContext: NavContext,
    private val viewModelStore: ViewModelStore = ViewModelStore()
) : LifecycleOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    var isClosing: Boolean = false
        private set(value) {
            !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)
            field = value
        }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
    fun setLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }
}

@Composable
fun NavEntry.LocalOwnersProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavContext provides navContext,
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
    ) {
        content()
    }
}

data class NavStackState(
    val backStack: List<NavEntry> = emptyList()
) {
    /**
     * Active entry and the entry which is probably exiting or present below the
     * active entry which is animating in
     *
     * All the entries which are supposed to be drawn on the screen fully / partially
     */
    fun activeEntries(): List<NavEntry> {
        if (backStack.isEmpty()) return emptyList()

        val activeEntryIndex = backStack.indexOfLast { !it.isClosing }

        val topIndex = if (activeEntryIndex + 1 == backStack.size) { // active item at top
            activeEntryIndex
        } else {
            activeEntryIndex + 1
        }

        val bottomIndex = if (activeEntryIndex + 1 == backStack.size) { // active item at top
            activeEntryIndex - 1
        } else {
            activeEntryIndex
        }

        return backStack
            .filterIndexed { index, _ -> index == topIndex || index == bottomIndex }
    }

    fun debugLog(): String {
        var log = ""
        backStack.forEach {
                entry -> log = log.plus(" -> ${entry.page.type} [${entry.isClosing}]")
        }
        return log
    }
}

inline fun <T> List<T>.takeUntil(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (item in this) {
        list.add(item)
        if (predicate(item)) {
            break
        }
    }
    return list
}

internal class NavStack() {

    private var currentEntryIndex = -1
    private val entries = LinkedList<NavEntry>()
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED


    fun updateHostLifecycleState(state: Lifecycle.State) {
        hostLifecycleState = state
        updateEntries()
    }

    fun add(entry: NavEntry) {
        removeClosedEntries()
        entries.addLast(entry)
        currentEntryIndex += 1
        updateEntries()
    }

    fun replace(entry: NavEntry) {
        check(currentEntryIndex >= 0) { "${currentEntryIndex + 1} items in the stack. Can't replace" }
        entries.add(currentEntryIndex, entry)
        updateEntries()
    }

    fun canPop(): Boolean {
        return currentEntryIndex > 1
    }

    fun pop() {
        check(currentEntryIndex > 0) { "${currentEntryIndex + 1} items in the stack. Can't pop" }
        currentEntryIndex -= 1
        updateEntries()
    }

    fun popUpTo(entry: NavEntry) {
        val popToIndex = entries.firstIndex { it.page.type == entry.page.type }
        if (popToIndex < 0 || popToIndex > currentEntryIndex) {
            pop()
        } else {
            currentEntryIndex = popToIndex
        }
        updateEntries()
    }

    private fun updateEntries() {
        for (i in 0 until entries.size) {
            when {
                i < currentEntryIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.STARTED))
                }
                i == currentEntryIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.RESUMED))
                }
                else -> { // closed entries
                    entries[i].viewModelStore.clear()
                    entries[i].setLifecycleState(Lifecycle.State.DESTROYED)
                }
            }
        }
    }

    private fun removeClosedEntries() {
        while (currentEntryIndex + 1 > entries.size) {
            entries.removeLast()
        }
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }

    fun toNavStackState(): NavStackState {
        return NavStackState(this.entries.map { it })
    }
//
//    private fun cleanBackStack(): List<MutableNavEntry> {
//        return backStack.takeWhile { !it.isClosing }
//    }

    fun addOrBringForward(navEntry: NavEntry) {
        val pageIndex = entries.indexOfFirst { it.page.type == navEntry.page.type }
        if (pageIndex < 0 || pageIndex == entries.size - 1) {
            add(navEntry)
            return
        }
        val existingPage = entries.removeAt(pageIndex)
        entries.add(existingPage)
        updateEntries()
    }

    fun addOrPopUpTo(navEntry: NavEntry) {
        IllegalStateException("addOrPopUpTo() Not Implemented Yet")
    }

    fun contains(id: String): Boolean {
        entries.forEach { entry ->
            if (entry.id == id)
                return true
        }
        return false
    }

    fun debugLog(): String {
        var log = ""
        entries.forEach {
            entry -> log = log.plus(" -> ${entry.page.type} [${entry.isClosing}]")
        }
        return log
    }

    fun clearBackStack() {
        return entries.clear()
    }

    fun isSameInitialStack(initialStack: List<Page>): Boolean {
        if (entries.isEmpty())
            return false
        initialStack.forEachIndexed { index, page ->
            if (entries.size > index && entries[index].page != page)
                return false
        }
        return true
    }
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
inline fun <T> Iterable<T>.firstIndex(predicate: (T) -> Boolean): Int {
    for ((i, element) in this.withIndex()) {
        if (predicate(element)) {
            return i
        }
    }
    return -1
}
