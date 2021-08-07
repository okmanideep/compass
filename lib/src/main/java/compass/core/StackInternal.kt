package compass.core

import java.util.*

internal class StackInternal<T>() {
    private val entries = LinkedList<T>()

    fun add(entry: T) {
        entries.add(entry)
    }

    fun replace(entry: T) {
        check(entries.isNotEmpty()) { "No items in the stack. Can't replace" }

        entries.removeLast()
        entries.add(entry)
    }

    fun canPop(): Boolean {
        return entries.size > 1
    }

    fun pop() {
        check(canPop()) { "${entries.size} items in the stack. Can't pop" }

        entries.removeLast()
    }

    fun clear() {
        entries.clear()
    }

    fun peek(): T? {
        return entries.lastOrNull()
    }

    fun contains(entry: T): Boolean {
        return entries.contains(entry)
    }

    fun bringToFront(entry: T) {
        require(entries.contains(entry))

        entries.remove(entry)
        entries.add(entry)
    }

    fun toList(): List<T> {
        return entries.toList()
    }
}