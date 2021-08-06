package compass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable

data class NavBackStack(
    val entries: List<NavEntry> = emptyList()
)

