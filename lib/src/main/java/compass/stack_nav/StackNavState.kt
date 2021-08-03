package compass.stack_nav

import compass.core.NavEntry
import compass.core.NavState

internal class StackNavState(
    override val backStack: List<NavEntry> = emptyList()
) : NavState {
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