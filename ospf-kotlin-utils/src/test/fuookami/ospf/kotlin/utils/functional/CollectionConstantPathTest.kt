package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.operator.withPrecision
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CollectionConstantPathTest {
    companion object {
        private val propertyKey = CompanionConstantProviderResolver.reflectionFallbackEnabledProperty
        private var previousValue: String? = null

        @JvmStatic
        @BeforeAll
        fun disableReflectionFallback() {
            previousValue = System.getProperty(propertyKey)
            System.setProperty(propertyKey, "false")
        }

        @JvmStatic
        @AfterAll
        fun restoreReflectionFallback() {
            if (previousValue == null) {
                System.clearProperty(propertyKey)
            } else {
                System.setProperty(propertyKey, previousValue)
            }
        }
    }

    @Test
    fun explicitConstantsPathsShouldWorkWhenFallbackDisabled() {
        val numbers = listOf(Int64.one, Int64.two, Int64.three)
        assertEquals(Int64(6), numbers.sum(Int64))
        assertEquals(Int64(6), numbers.sumOf(Int64) { it })
        assertEquals(Int64(6), numbers.asSequence().sum(Int64))
        assertEquals(Int64(6), numbers.asSequence().sumOf(Int64) { it })
        val floats = listOf(Flt64.one, Flt64.two, Flt64.three)
        val iterableAverage: Flt64 = floats.average(Flt64)
        val sequenceAverage: Flt64 = floats.asSequence().average(Flt64)
        assertEquals(Flt64(2.0), iterableAverage)
        assertEquals(Flt64(2.0), sequenceAverage)

        val nullableNumbers = listOf(Flt64.one, Flt64.two, Flt64.three)
        val nullableIterableAverage: Flt64? = nullableNumbers.averageOrNull(Flt64)
        val nullableSequenceAverage: Flt64? = nullableNumbers.asSequence().averageOrNull(Flt64)
        assertEquals(Flt64(2.0), nullableIterableAverage)
        assertEquals(Flt64(2.0), nullableSequenceAverage)
        assertEquals(null, listOf(Flt64.one, null).averageOrNull(Flt64))

        val map = mapOf("a" to Int64.one, "b" to Int64.two, "c" to Int64.three)
        assertEquals(Int64(6), map.sum(Int64))
        assertEquals(Int64(6), map.sumOf(Int64) { it.value })
        val floatMap = mapOf("a" to Flt64.one, "b" to Flt64.two, "c" to Flt64.three)
        val mapAverage: Flt64 = floatMap.average(Flt64)
        val mapNullableAverage: Flt64? = floatMap.mapValues { it.value as Flt64? }.averageOrNull(Flt64)
        assertEquals(Flt64(2.0), mapAverage)
        assertEquals(Flt64(2.0), mapNullableAverage)
    }

    @Test
    fun reifiedDefaultPathsShouldThrowWhenFallbackDisabled() {
        val numbers = listOf(Int64.one, Int64.two, Int64.three)
        assertFailsWith<IllegalStateException> { numbers.sum<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.sumOf<Int64, Int64> { it } }
        assertFailsWith<IllegalStateException> { numbers.average<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.map { it as Int64? }.averageOrNull<Int64>() }
        assertFailsWith<IllegalStateException> { withPrecision<Flt64>() }
    }

    @Test
    fun explicitPrecisionShouldWorkWhenFallbackDisabled() {
        val precision = withPrecision(Flt64, Flt64(0.01))
        with(precision) {
            assertTrue(Flt64(1.0) equal Flt64(1.005))
            assertTrue(Flt64(1.0) unequal Flt64(1.02))
        }
    }
}
