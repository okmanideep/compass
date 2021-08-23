package compass

import android.os.Parcelable
import androidx.compose.runtime.*

@Composable
fun getNavController(): NavController {
    return LocalNavController.current
        ?: remember {
            NavController(null)
        }
}

interface NavHostController {
    fun setStateChangedListener(listener: (NavBackStack) -> Unit)
    fun canNavigateTo(pageType: String): Boolean
    fun navigateTo(pageType: String, args: Parcelable? = null, replace: Boolean = false)
    fun canGoBack(): Boolean
    fun goBack(): Boolean
}

class NavController(private val parent: NavController?) {
    private var hostController: NavHostController? = null
    var state by mutableStateOf(NavBackStack())
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

    fun navigateTo(pageType: String, args: Parcelable? = null, replace: Boolean = false) {
        hostController?.let {
            if (it.canNavigateTo(pageType)) {
                it.navigateTo(pageType, args, replace)
                return
            }
        }

        parent?.navigateTo(pageType, args, replace)
    }

    fun goBack() {
        if (hostController?.goBack() != true) {
            parent?.goBack()
        }
    }
}

val LocalNavController = compositionLocalOf<NavController?> { null }

