package compass.bottom_nav

import androidx.lifecycle.Lifecycle
import compass.core.NavEntry
import compass.core.NavState

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