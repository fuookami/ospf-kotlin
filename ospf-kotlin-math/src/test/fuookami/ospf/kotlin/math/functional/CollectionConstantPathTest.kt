package fuookami.ospf.kotlin.math.functional

import fuookami.ospf.kotlin.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.operator.withPrecision
import fuookami.ospf.kotlin.utils.functional.average
import fuookami.ospf.kotlin.utils.functional.averageOrNull
import fuookami.ospf.kotlin.utils.functional.sum
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.utils.functional.sumOfOrNull
import fuookami.ospf.kotlin.utils.functional.sumOrNull
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
        val nullableNumbers = listOf<Int64?>(Int64.one, Int64.two, Int64.three)
        assertEquals(Int64(6), nullableNumbers.sumOrNull(Int64))
        assertEquals(Int64(4), listOf<Int64?>(Int64.one, null, Int64.three).sumOfOrNull(Int64, { it }, { Int64.zero }))
        assertEquals(null, listOf<Int64?>(Int64.one, null).sumOrNull(Int64))
        assertEquals(Int64(6), numbers.asSequence().sum(Int64))
        assertEquals(Int64(6), numbers.asSequence().sumOf(Int64) { it })
        assertEquals(Int64(6), numbers.map { it as Int64? }.asSequence().sumOrNull(Int64))
        assertEquals(
            Int64(4),
            listOf<Int64?>(Int64.one, null, Int64.three).asSequence().sumOfOrNull(Int64, { it }, { Int64.zero })
        )
        val floats = listOf(Flt64.one, Flt64.two, Flt64.three)
        val iterableAverage: Flt64 = floats.average(Flt64)
        val sequenceAverage: Flt64 = floats.asSequence().average(Flt64)
        assertEquals(Flt64(2.0), iterableAverage)
        assertEquals(Flt64(2.0), sequenceAverage)

        val nullableFloatNumbers = listOf(Flt64.one, Flt64.two, Flt64.three)
        val nullableIterableAverage: Flt64? = nullableFloatNumbers.averageOrNull(Flt64)
        val nullableSequenceAverage: Flt64? = nullableFloatNumbers.asSequence().averageOrNull(Flt64)
        assertEquals(Flt64(2.0), nullableIterableAverage)
        assertEquals(Flt64(2.0), nullableSequenceAverage)
        assertEquals(null, listOf(Flt64.one, null).averageOrNull(Flt64))

        val map = mapOf("a" to Int64.one, "b" to Int64.two, "c" to Int64.three)
        assertEquals(Int64(6), map.sum(Int64))
        assertEquals(Int64(6), map.sumOf(Int64) { it.value })
        val nullableMap = mapOf("a" to Int64.one, "b" to Int64.two, "c" to Int64.three).mapValues { it.value as Int64? }
        assertEquals(Int64(6), nullableMap.sumOrNull(Int64))
        assertEquals(
            Int64(4),
            mapOf("a" to Int64.one, "b" to null, "c" to Int64.three).sumOfOrNull(Int64, { it.value }, { Int64.zero })
        )
        assertEquals(null, mapOf("a" to Int64.one, "b" to null).sumOrNull(Int64))
        val floatMap = mapOf("a" to Flt64.one, "b" to Flt64.two, "c" to Flt64.three)
        val mapAverage: Flt64 = floatMap.average(Flt64)
        val mapNullableAverage: Flt64? = floatMap.mapValues { it.value as Flt64? }.averageOrNull(Flt64)
        assertEquals(Flt64(2.0), mapAverage)
        assertEquals(Flt64(2.0), mapNullableAverage)
    }

    @Test
    fun reifiedDefaultPathsShouldThrowWhenFallbackDisabled() {
        val numbers = listOf(Int64.one, Int64.two, Int64.three)
        val map: Map<String, Int64> = mapOf("a" to Int64.one, "b" to Int64.two)
        val nullableMap: Map<String, Int64?> = mapOf("a" to Int64.one, "b" to Int64.two)
        assertFailsWith<IllegalStateException> { numbers.sum<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.map { it as Int64? }.sumOrNull<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.sumOf<Int64, Int64> { it } }
        assertFailsWith<IllegalStateException> { numbers.map { it as Int64? }.sumOfOrNull<Int64?, Int64> { it } }
        assertFailsWith<IllegalStateException> { numbers.asSequence().sum<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.asSequence().sumOf<Int64, Int64> { it } }
        assertFailsWith<IllegalStateException> { numbers.asSequence().map { it as Int64? }.sumOrNull<Int64>() }
        assertFailsWith<IllegalStateException> {
            numbers.asSequence().map { it as Int64? }.sumOfOrNull<Int64?, Int64> { it }
        }
        assertFailsWith<IllegalStateException> { map.sum() }
        assertFailsWith<IllegalStateException> { map.sumOf { it.value } }
        assertFailsWith<IllegalStateException> { nullableMap.sumOrNull() }
        assertFailsWith<IllegalStateException> { numbers.average<Int64>() }
        assertFailsWith<IllegalStateException> { numbers.map { it as Int64? }.averageOrNull<Int64>() }
        assertFailsWith<IllegalStateException> { withPrecision<Flt64>() }
    }

    @Test
    fun explicitPrecisionShouldWorkWhenFallbackDisabled() {
        val explicitConstantsPrecision = withPrecision(Flt64, Flt64(0.01))
        with(explicitConstantsPrecision) {
            assertTrue(Flt64(1.0) equal Flt64(1.005))
            assertTrue(Flt64(1.0) unequal Flt64(1.02))
        }

        val explicitReifiedPrecision = withPrecision<Flt64>(Flt64(0.01))
        with(explicitReifiedPrecision) {
            assertTrue(Flt64(1.0) equal Flt64(1.005))
            assertTrue(Flt64(1.0) unequal Flt64(1.02))
        }
    }
}