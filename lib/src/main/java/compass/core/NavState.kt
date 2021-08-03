package compass.core

interface NavState {
    val backStack: List<NavEntry>
    fun activeEntries(): List<NavEntry>
}