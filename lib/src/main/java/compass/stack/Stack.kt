package compass.stack

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import compass.*
import compass.NavStack

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

@ExperimentalAnimationApi
@Composable
fun StackNavHost(
    navController: NavController,
    startDestination: Page,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) = StackNavHost(
    navController = navController,
    initialStack = listOf(startDestination),
    modifier,
    builder = builder
)

@ExperimentalAnimationApi
@Composable
fun StackNavHost(
    navController: NavController,
    initialStack: List<Page>,
    modifier: Modifier = Modifier,
    builder: PagesBuilder.() -> Unit
) {
    val pages = PagesBuilder().apply(builder).build()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "StackNavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }

    val stackNavViewModel = remember (pages, navController, viewModelStoreOwner) {
        stackNavViewModel(
            pages = pages,
            navController = navController,
            viewModelStoreOwner = viewModelStoreOwner
        )
    }

    DisposableEffect(stackNavViewModel, navController) {
        for (page in initialStack) {
            stackNavViewModel.navigateTo(page, false)
        }

        navController.attachNavHostController(stackNavViewModel)

        onDispose {
            navController.detachNavHostController()
        }
    }

    val activeEntries = navController.state.activeEntries()

    Box(modifier = modifier) {
        for (entry in activeEntries) {
            BackStackEntryScopeProvider(
                backStackEntryScope = stackNavViewModel.scopeForEntryId(entry.id)
            ) {
                AnimatedVisibility(
                    visible = !entry.isClosing,
                    enter = slideInHorizontally({ it }),
                    exit = fadeOut()
                ) {
                    val pageContentComposable = pages.pageContentByType[entry.page.type]!!
                    pageContentComposable(entry.page)
                }
            }
        }
    }
}

@Composable
private fun BackStackEntryScopeProvider(
    backStackEntryScope: BackStackEntryScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNavContext provides backStackEntryScope,
        LocalViewModelStoreOwner provides backStackEntryScope
    ) {
        content()
    }
}

private fun stackNavViewModel(
    pages: Pages,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): StackNavViewModel {
    val factory = object: ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return StackNavViewModel(pages, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(StackNavViewModel::class.java)
}

internal class StackNavViewModel(
    private val pages: Pages,
    private val navController: NavController
): ViewModel(), NavHostController {
    private var navStack = NavStack(emptyList())
    private var listener: ((NavStackState) -> Unit)? = null
    private val scopeByEntryId = mutableMapOf<String, BackStackEntryScope>()

    override fun setStateChangedListener(listener: (NavStackState) -> Unit) {
        this.listener = listener
        invokeListener()
    }

    override fun canNavigateTo(page: Page): Boolean {
        return pages.hasPageType(page)
    }

    override fun navigateTo(page: Page, popUpTo: Boolean) {
        navStack = if (popUpTo) navStack.addOrPopUpTo(page) else navStack.add(page)

        invokeListener()
    }

    override fun goBack(): Boolean {
        val canGoBack = navStack.canGoBack()
        if (canGoBack) {
            navStack = navStack.pop()
            invokeListener()
        }

        return canGoBack
    }

    internal fun onExitCompleted() {
        cleanScopes()
    }

    internal fun scopeForEntryId(entryId: String): BackStackEntryScope {
        return scopeByEntryId[entryId]
            ?: createScopeForEntryId(entryId)
    }

    private fun createScopeForEntryId(entryId: String): BackStackEntryScope {
        val scope = BackStackEntryScope(navController)
        scopeByEntryId[entryId] = scope

        return scope
    }

    private fun invokeListener() {
        listener?.invoke(navStack.toNavStackState())
    }

    private fun cleanScopes() {
        //TODO: clean and clear scopes in scopeByEntryId based on back stack
    }

    override fun onCleared() {
        scopeByEntryId.values.forEach {
            it.viewModelStore.clear()
        }

        super.onCleared()
    }
}

internal class BackStackEntryScope(private val baseController: NavController)
    : NavContext, ViewModelStoreOwner {
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