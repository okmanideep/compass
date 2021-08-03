package compass.core

import android.os.Parcelable
import compass.NavOptions
import compass.Page

interface NavHostController {
    fun setStateChangedListener(listener: (NavState) -> Unit)
    fun canNavigateTo(page: Page): Boolean
    fun navigateTo(navEntry: NavEntry, popUpTo: Boolean)
    fun navigateTo(navEntry: NavEntry, pageExtras: Parcelable?, navOptions: NavOptions)
    fun canGoBack(): Boolean
    fun goBack(): Boolean
}