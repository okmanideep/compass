package compass

import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import kotlinx.parcelize.Parcelize

@Parcelize
data class Page(
    val type: String,
    val args: Parcelable? = null
) : Parcelable

class NavBackStack(
    val entries: List<NavEntry> = emptyList()
) {
    fun activeEntries(): List<NavEntry> {
        if (entries.isEmpty()) return emptyList()

        val activeEntryIndex =
            entries.indexOfLast { it.lifecycle.currentState == Lifecycle.State.RESUMED }

        val topIndex = if (activeEntryIndex + 1 == entries.size) { // active item at top
            activeEntryIndex
        } else {
            activeEntryIndex + 1
        }

        val bottomIndex = if (activeEntryIndex + 1 == entries.size) { // active item at top
            activeEntryIndex - 1
        } else {
            activeEntryIndex
        }

        return entries
            .filterIndexed { index, _ -> index == topIndex || index == bottomIndex }
    }
}

