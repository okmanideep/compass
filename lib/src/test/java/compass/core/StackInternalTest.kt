package compass.core

import org.junit.Assert.*
import org.junit.Before

import org.junit.Test

class StackInternalTest {
    private lateinit var stack: StackInternal<String>

    @Before
    fun setup() {
        stack = StackInternal()
    }

    @Test
    fun isEmptyInitially() {
        assertEquals(emptyList<String>(), stack.asList())
        assertEquals(false, stack.canPop())
    }

    @Test
    fun add() {
        stack.add("A")
        assertEquals(listOf("A"), stack.asList())
        assertEquals(0, stack.currentEntryIndex)

        stack.add("B")
        assertEquals(listOf("A", "B"), stack.asList())
        assertEquals(1, stack.currentEntryIndex)

        stack.add("C")
        assertEquals(listOf("A", "B", "C"), stack.asList())
        assertEquals(2, stack.currentEntryIndex)
    }

    @Test
    fun replaceAndThenAdd() {
        stack.add("A")
        stack.add("B")
        stack.replace("C")

        assertEquals(listOf("A", "C", "B"), stack.asList())
        assertEquals(1, stack.currentEntryIndex)

        stack.add("D")
        assertEquals(listOf("A", "C", "D", "B"), stack.asList())
        assertEquals(2, stack.currentEntryIndex)
    }

    @Test
    fun popAndThenAdd() {
        stack.add("A")
        stack.add("B")
        stack.add("C")
        stack.pop()

        assertEquals(listOf("A", "B", "C"), stack.asList())
        assertEquals(1, stack.currentEntryIndex)

        stack.add("D")
        assertEquals(listOf("A", "B", "D", "C"), stack.asList())
        assertEquals(2, stack.currentEntryIndex)
    }

    @Test
    fun canPop() {
        stack.add("A")
        assertEquals(false, stack.canPop())

        stack.add("B")
        assertEquals(true, stack.canPop())

        stack.add("C")
        assertEquals(true, stack.canPop())

        stack.pop()
        assertEquals(true, stack.canPop())

        stack.pop()
        assertEquals(false, stack.canPop())
    }

    @Test
    fun removePoppedEntries() {
        stack.add("A")
        stack.add("B")
        stack.add("C")
        stack.pop()
        stack.add("D")
        stack.removePoppedEntries()

        assertEquals(listOf("A", "B", "D"), stack.asList())
        assertEquals(2, stack.currentEntryIndex)
    }
}