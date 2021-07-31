package compass.core

import java.util.*

internal class StackInternal<T>() {
    var currentEntryIndex = -1
        private set
    private val entries = LinkedList<T>()

    fun add(entry: T) {
        entries.add(currentEntryIndex + 1, entry)
        currentEntryIndex += 1
    }

    fun replace(entry: T) {
        check(currentEntryIndex >= 0) { "${currentEntryIndex + 1} items in the stack. Can't replace" }

        entries.add(currentEntryIndex, entry)
    }

    fun canPop(): Boolean {
        return currentEntryIndex > 0
    }

    fun pop() {
        check(currentEntryIndex > 0) { "${currentEntryIndex + 1} items in the stack. Can't pop" }

        currentEntryIndex -= 1
    }

    fun asList(): List<T> {
        return entries
    }

    fun removePoppedEntries() {
        while (currentEntryIndex + 1 < entries.size) {
            entries.removeLast()
        }
    }
}