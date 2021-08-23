package compass

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.util.*

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

    internal fun setLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    override fun equals(other: Any?): Boolean {
        return (other is NavEntry && other.id == id)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "NavEntry:$pageType/$id"
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

fun LifecycleOwner.isDestroyed(): Boolean {
    return lifecycle.currentState == Lifecycle.State.DESTROYED
}

fun LifecycleOwner.isAtLeastStarted(): Boolean {
    return lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

fun LifecycleOwner.isStarted(): Boolean {
    return lifecycle.currentState == Lifecycle.State.STARTED
}

fun LifecycleOwner.isResumed(): Boolean {
    return lifecycle.currentState == Lifecycle.State.RESUMED
}