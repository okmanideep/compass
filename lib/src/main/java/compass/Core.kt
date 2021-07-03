package compass

import android.content.Context
import android.content.ContextWrapper
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.parcelize.Parcelize
import java.util.UUID

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

class NavController(private val navContext: NavContext) {
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
        }
    }

    fun detachNavHostController() {
        hostController = null
    }

    fun navigateTo(page: Page, popUpTo: Boolean) {
        hostController?.let {
            if (it.canNavigateTo(page)) {
                it.navigateTo(page, popUpTo)
            } else {
                navContext.owner()?.navigateTo(page, popUpTo)
            }
        } ?: navContext.owner()?.navigateTo(page, popUpTo)
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
    fun navigateTo(page: Page, popUpTo: Boolean = false)
    fun canGoBack(): Boolean
    fun goBack(): Boolean
}

interface NavContext {
    fun owner(): NavController?
    fun controller(): NavController
}

val LocalNavContext = compositionLocalOf<NavContext?> { null }

data class NavStackEntry(
    val page: Page,
    val isClosing: Boolean,
    val id: String = UUID.randomUUID().toString()
)

internal class MutableNavStackEntry(
    val page: Page,
    var isClosing: Boolean,
    val id: String = UUID.randomUUID().toString(),
) {
    fun toBackStackEntry(): NavStackEntry {
        return NavStackEntry(page, isClosing, id)
    }
}

data class NavStackState(
    val backStack: List<NavStackEntry> = emptyList()
) {
    /**
     * Active entry and the entry which is probably exiting or present below the
     * active entry which is animating in
     *
     * All the entries which are supposed to be drawn on the screen fully / partially
     */
    fun activeEntries(): List<NavStackEntry> {
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

internal data class NavStack(
    private val backStack: List<MutableNavStackEntry>,
) {
    companion object {
        val EMPTY = NavStack(emptyList())
    }

    fun add(page: Page): NavStack {
        val cleanBackStack = backStack.takeWhile { !it.isClosing }
        val entry = MutableNavStackEntry(page, false)
        return copy(backStack = cleanBackStack + entry)
    }

    fun pop(): NavStack {
        val cleanBackStack = backStack.takeWhile { !it.isClosing }
        check(cleanBackStack.isNotEmpty()) { "Cannot pop. Nothing in stack" }
        check(cleanBackStack.size > 1) { "Only one item in the stack. Cannot pop" }

        val current = cleanBackStack.last()
        current.isClosing = true
        return this.copy(backStack = cleanBackStack)
    }

    fun popUpTo(pageType: String): NavStack {
        val cleanBackStack = backStack.takeWhile { !it.isClosing }

        val pageIndex = cleanBackStack.indexOfFirst { it.page.type == pageType }
        if (pageIndex < 0 || pageIndex == cleanBackStack.size - 1) return NavStack(cleanBackStack)

        return copy(
            backStack = cleanBackStack.take(pageIndex) + cleanBackStack[pageIndex] + cleanBackStack.last()
                .apply { isClosing = true })
    }

    fun popUpTo(page: Page): NavStack {
        val cleanBackStack = backStack.takeWhile { !it.isClosing }

        val pageIndex = cleanBackStack.indexOfFirst { it.page.type == page.type }
        if (pageIndex < 0 || pageIndex == cleanBackStack.size - 1) return NavStack(cleanBackStack)

        return copy(
            backStack = cleanBackStack.take(pageIndex) + cleanBackStack[pageIndex] + cleanBackStack.last()
                .apply { isClosing = true })
    }

    fun addOrPopUpTo(page: Page): NavStack {
        val cleanBackStack = backStack.takeWhile { !it.isClosing }

        val pageIndex = cleanBackStack.indexOfFirst { it.page.type == page.type }
        if (pageIndex < 0 || pageIndex == cleanBackStack.size - 1) return add(page)

        return copy(
            backStack = cleanBackStack.take(pageIndex) + cleanBackStack[pageIndex] + cleanBackStack.last()
                .apply { isClosing = true })
    }

    fun canGoBack(): Boolean = cleanBackStack().size > 1

    fun toNavStackState(): NavStackState {
        return NavStackState(this.backStack.map { it.toBackStackEntry() })
    }

    private fun cleanBackStack(): List<MutableNavStackEntry> {
        return backStack.takeWhile { !it.isClosing }
    }
}
