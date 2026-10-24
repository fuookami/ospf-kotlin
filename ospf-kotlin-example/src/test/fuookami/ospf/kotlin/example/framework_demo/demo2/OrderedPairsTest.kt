package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class OrderedPairsTest {
    @Test
    fun `suffix enumeration preserves stable sorted strict pairs`() {
        val random = Random(61024)
        repeat(50) {
            val values = List(random.nextInt(35)) { random.nextInt(7) }.sorted()
            val expected = values.flatMapIndexed { i, value ->
                values.drop(i + 1).mapNotNull { other -> (value to other).takeIf { value < other } }
            }
            assertEquals(expected, orderedPairs(values) { a, b -> a < b })
        }
    }

    @Test
    fun `indices preserve first equality and are rebuilt for mutable input`() {
        val values = mutableListOf("b", "a", "b", "c", "a")
        val initial = firstOccurrenceIndices(values)
        for (value in values) {
            assertEquals(values.indexOf(value), initial[value])
        }
        values.removeAt(0)
        val updated = firstOccurrenceIndices(values)
        assertEquals(1, updated["b"])
        assertEquals(0, updated["a"])
        assertEquals(0, initial["b"])
    }
}
