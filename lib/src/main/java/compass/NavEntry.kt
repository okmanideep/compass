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
