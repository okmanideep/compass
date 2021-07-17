package compass

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.common.firstIndex
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
    var state by mutableStateOf<NavState?>(null)
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
    fun setStateChangedListener(listener: (NavState) -> Unit)
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

//    var isClosing: Boolean = !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun setLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    fun isClosing(): Boolean {
        return !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)
    }

    fun isResumed(): Boolean {
        return lifecycleRegistry.currentState == Lifecycle.State.RESUMED
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

interface NavState {
    val backStack: List<NavEntry>
    fun activeEntries(): List<NavEntry>
}

internal class StackNavState(
    override val backStack: List<NavEntry> = emptyList()
) : NavState{
    /**
     * Active entry and the entry which is probably exiting or present below the
     * active entry which is animating in
     *
     * All the entries which are supposed to be drawn on the screen fully / partially
     */
    override fun activeEntries(): List<NavEntry> {
        if (backStack.isEmpty()) return emptyList()

        val activeEntryIndex = backStack.indexOfLast { !it.isClosing() }

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
}

internal class BottomNavState(
    override val backStack: List<NavEntry> = emptyList()
): NavState {
    override fun activeEntries(): List<NavEntry> {
        if (backStack.isEmpty()) return emptyList()

        val topIndex = backStack.indexOfLast { it.lifecycle.currentState == Lifecycle.State.RESUMED }
        val bottomIndex = topIndex - 1

        return backStack.filterIndexed { index, _ -> index == topIndex || index == bottomIndex }
    }
}

fun NavState.debugLog(): String {
    var log = ""
    backStack.forEach {
            entry -> log = log.plus(" -> ${entry.page.type} [${entry.isClosing()}]")
    }
    return log
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
        return currentEntryIndex > 0
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

    fun toNavStackState(): NavState {
        return StackNavState(this.entries.map { it })
    }

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
        throw IllegalStateException("addOrPopUpTo() Not Implemented Yet")
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
            entry -> log = log.plus(" -> ${entry.page.type} [${entry.isClosing()}]")
        }
        return log
    }

    fun clearBackStack() {
        currentEntryIndex = -1
        updateEntries()
        removeClosedEntries()
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

internal class BottomNavStack {
    private var headIndex = 0
    private var tailIndex = -1
    private val entries = LinkedList<NavEntry>()
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED

    fun updateHostLifecycleState(state: Lifecycle.State) {
        hostLifecycleState = state
    }

    fun add(entry: NavEntry) {
        entries.addLast(entry)
        tailIndex = entries.size - 1
        updateEntries()
    }

    fun canGoBack(): Boolean {
        return tailIndex > headIndex
    }

    fun goBackWithPersist() {
        check(tailIndex > headIndex) { "${tailIndex + 1} items in the stack. Can't pop" }
        tailIndex -= 1
        updateEntries()
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }

    fun toNavStackState(): NavState {
        return BottomNavState(this.entries.map { it })
    }

    fun addOrBringForward(navEntry: NavEntry) {
        val existingPageIndex = entries.firstIndex { it.page.type == navEntry.page.type }
        if (existingPageIndex < 0) {
            add(navEntry)
            return
        }

        tailIndex = existingPageIndex
        updateEntries()
    }

    fun addOrPopUpTo(navEntry: NavEntry) {
        throw IllegalStateException("addOrPopUpTo() Not Implemented Yet")
    }

    private fun updateEntries() {
        for (i in 0 until entries.size) {
            when (i) {
                in (headIndex) until tailIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.STARTED))
                }
                tailIndex -> {
                    entries[i].setLifecycleState(capToHostLifecycle(Lifecycle.State.RESUMED))
                }
                else -> { // persisted entries
                    entries[i].setLifecycleState(Lifecycle.State.CREATED)
                }
            }
        }
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
                entry -> log = log.plus(" -> ${entry.page.type} [${entry.isClosing()}]")
        }
        return log
    }

    /**
     * entries for Bottom Nav must never be cleared
     * */
    fun clearBackStack() {

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
