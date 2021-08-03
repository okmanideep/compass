package compass

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.core.NavEntry
import compass.core.NavState
import kotlinx.parcelize.Parcelize
import java.util.NoSuchElementException

/**
* Don't remove this commented code
* */

//@Composable
//internal fun BackStackEntryScopeProvider(
//    backStackEntryScope: BackStackEntryScope,
//    content: @Composable () -> Unit
//) {
//    CompositionLocalProvider(
//        LocalNavContext provides backStackEntryScope,
//        LocalViewModelStoreOwner provides backStackEntryScope
//    ) {
//        content()
//    }
//}

@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable

data class NavOptions(
    val clearTop: Boolean = false
)

internal class Pages(
    val pageContentByType: Map<String, @Composable (Page) -> Unit>
) {

    fun hasPageType(page: Page): Boolean {
        return pageContentByType.containsKey(page.type)
    }
}

class PagesBuilder(
    private val pageContentByType: HashMap<String, @Composable (Page) -> Unit> = HashMap()
) {
    fun page(type: String, content: @Composable (Page) -> Unit) {
        pageContentByType[type] = content
    }

    internal fun build() = Pages(pageContentByType)
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

fun NavState.debugLog(): String {
    var log = ""
    for (item in backStack) {
        log = log.plus(" -> ${item.page.type} [${item.isClosing()}]")
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

@Composable
fun NavEntry.LocalOwnersProvider(
    parentViewModelStoreOwner: ViewModelStoreOwner,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNavContext provides this,
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
        LocalParentViewModelStoreOwner provides parentViewModelStoreOwner
    ) {
        content()
    }
}