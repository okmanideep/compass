package compass.core

import androidx.lifecycle.Lifecycle
import compass.Page
import compass.firstIndex
import java.util.*

internal class NavStack {
    var currentEntryIndex = -1
    val entries = LinkedList<NavEntry>()
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED


    fun updateHostLifecycleState(state: Lifecycle.State) {
        hostLifecycleState = state
    }

    fun add(navEntry: NavEntry) {
        entries.addLast(navEntry)
        currentEntryIndex += 1
    }

    fun addOrBringForward(navEntry: NavEntry) {
        val pageIndex = entries.indexOfFirst { it.page.type == navEntry.page.type }
        if (pageIndex < 0 || pageIndex == entries.size - 1) {
            add(navEntry)
            return
        }
        val existingPage = entries.removeAt(pageIndex)
        entries.add(existingPage)
        currentEntryIndex = entries.lastIndex
    }

    fun replace(entry: NavEntry) {
        check(currentEntryIndex >= 0) { "${currentEntryIndex + 1} items in the stack. Can't replace" }
        entries.add(currentEntryIndex, entry)
    }

    fun canPop(): Boolean {
        return currentEntryIndex > 0
    }

    fun pop() {
        check(currentEntryIndex > 0) { "${currentEntryIndex + 1} items in the stack. Can't pop" }
        currentEntryIndex -= 1
    }

    fun popUpTo(navEntry: NavEntry) {
        val popToIndex = entries.firstIndex { it.page.type == navEntry.page.type }
        if (popToIndex < 0 || popToIndex > currentEntryIndex) {
            pop()
        } else {
            currentEntryIndex = popToIndex
        }
    }

    fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
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