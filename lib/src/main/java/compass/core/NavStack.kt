package compass.core

import androidx.lifecycle.Lifecycle
import compass.NavEntry
import java.util.*

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

    fun popUpTo(pageType: String) {
        val popToIndex = entries.firstIndex { it.pageType == pageType }
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

    fun asList(): List<NavEntry> {
        return entries
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
