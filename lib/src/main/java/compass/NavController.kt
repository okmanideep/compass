package compass

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.*
import compass.core.NavContext
import compass.core.NavEntry
import compass.core.NavHostController
import compass.core.NavState

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

    /**
     * @param pageExtras are for passing extra info required for page business logic eg. ContentId
     * @param navOptions are for extras like clearTop, replace etc flags
     * */

    fun navigateTo(page: Page, pageExtras: Parcelable?, navOptions: NavOptions) {
        hostController?.let {
            if (it.canNavigateTo(page = page)) {
                val navEntry = createNavEntry(page, this)
                it.navigateTo(navEntry = navEntry, null, navOptions)
            } else navContext.owner()?.navigateTo(page, pageExtras, navOptions)
        } ?: navContext.owner()?.navigateTo(page, pageExtras, navOptions)
    }

    private fun createNavEntry(page: Page, navController: NavController): NavEntry {
        return NavEntry(page = page, baseNavController = navController)
    }
}

val LocalNavContext = compositionLocalOf<NavContext?> { null }
val LocalParentViewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner?> { null }
