package compass

import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable

@Composable
fun getNavController(): NavController {
    return LocalNavController.current
        ?: remember {
            NavController(null)
        }
}

class NavController(private val parent: NavController?) {
    private var hostController: NavHostController? = null
    var state by mutableStateOf(NavState())
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
            } else {
                parent?.navigateTo(pageType, args, replace)
            } ?: parent?.navigateTo(pageType, args, replace)
        }
    }

    fun goBack() {
        if (hostController?.goBack() != true) {
            parent?.goBack()
        }
    }
}

val LocalNavController = compositionLocalOf<NavController?> { null }

interface NavHostController {
    fun setStateChangedListener(listener: (NavState) -> Unit)
    fun canNavigateTo(pageType: String): Boolean
    fun navigateTo(pageType: String, args: Parcelable? = null, replace: Boolean = false)
    fun canGoBack(): Boolean
    fun goBack(): Boolean
}

class NavEntry(
    val id: String = UUID.randomUUID().toString(),
    val pageType: String,
    val args: Parcelable?,
    private val parentNavController: NavController,
    private val viewModelStore: ViewModelStore = ViewModelStore(),
) : LifecycleOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    val controller by lazy { NavController(parentNavController) }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun setLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }
}

@Composable
fun NavEntry.LocalOwnersProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavController provides this.controller,
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
    ) {
        content()
    }
}

class NavState(
    val backStack: List<NavEntry> = emptyList()
) {
    fun activeEntries(): List<NavEntry> {
        if (backStack.isEmpty()) return emptyList()

        val activeEntryIndex =
            backStack.indexOfLast { it.lifecycle.currentState == Lifecycle.State.RESUMED }

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

