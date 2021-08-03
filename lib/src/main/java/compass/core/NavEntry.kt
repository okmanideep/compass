package compass.core

import androidx.lifecycle.*
import compass.NavController
import compass.Page
import java.util.*

class NavEntry(
    val id: String = UUID.randomUUID().toString(),
    val page: Page,
    private val baseNavController: NavController,
    private val viewModelStore: ViewModelStore = ViewModelStore()
) : LifecycleOwner, ViewModelStoreOwner, NavContext {
    private val lifecycleRegistry = LifecycleRegistry(this)

//    var isClosing: Boolean = !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private val controller by lazy {
        NavController(this)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    fun setLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    fun isClosing(): Boolean {
        return !lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)
    }

    fun isResumed(): Boolean {
        return lifecycleRegistry.currentState == Lifecycle.State.RESUMED
    }

    override fun owner(): NavController {
        return baseNavController
    }

    override fun controller(): NavController {
        return controller
    }
}