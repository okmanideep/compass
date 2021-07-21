package compass.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.LocalNavContext
import compass.NavContext
import compass.NavController
import compass.Page
import java.util.NoSuchElementException

internal class BackStackEntryScope(private val baseController: NavController) : NavContext,
    ViewModelStoreOwner {
    private val controller by lazy {
        NavController(this)
    }

    private val vmStore by lazy { ViewModelStore() }

    override fun owner(): NavController {
        return baseController
    }

    override fun controller(): NavController {
        return controller
    }

    override fun getViewModelStore(): ViewModelStore {
        return vmStore
    }
}

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