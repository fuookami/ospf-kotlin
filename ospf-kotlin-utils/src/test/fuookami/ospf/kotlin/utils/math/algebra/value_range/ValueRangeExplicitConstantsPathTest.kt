package fuookami.ospf.kotlin.utils.math.algebra.value_range

import fuookami.ospf.kotlin.utils.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValueRangeExplicitConstantsPathTest {
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
    fun explicitConstantsConstructorsShouldWorkWhenFallbackDisabled() {
        val full = ValueRange(Flt64)
        assertTrue(full.contains(Flt64.zero))

        val single = ValueRange(Flt64.one, Flt64).value!!
        assertTrue(single.fixed)
        assertEquals(Flt64.one, single.fixedValue)

        val bounded = ValueRange(Flt64.one, Flt64.three, Interval.Closed, Interval.Closed, Flt64).value!!
        assertTrue(bounded.contains(Flt64.two))

        val geq = ValueRange.geq(Flt64.one, Interval.Closed, Flt64).value!!
        assertTrue(geq.contains(Flt64.one))

        val leq = ValueRange.leq(Flt64.three, Interval.Closed, Flt64).value!!
        assertTrue(leq.contains(Flt64.two))

        val upperInfinite = ValueRange(Flt64.one, Infinity, Interval.Closed, Flt64).value!!
        assertTrue(upperInfinite.contains(Flt64.ten))

        val lowerInfinite = ValueRange(NegativeInfinity, Flt64.three, Interval.Closed, Flt64).value!!
        assertTrue(lowerInfinite.contains(-Flt64.ten))
    }

    @Test
    fun reifiedConstructorsShouldThrowWhenFallbackDisabled() {
        assertFailsWith<IllegalStateException> { ValueRange<Flt64>() }
        assertFailsWith<IllegalStateException> { ValueRange(Flt64.one) }
        assertFailsWith<IllegalStateException> { ValueRange(Flt64.one, Flt64.three) }
        assertFailsWith<IllegalStateException> { ValueRange.geq(Flt64.one) }
        assertFailsWith<IllegalStateException> { ValueRange.leq(Flt64.three) }
    }

    @Test
    fun explicitSerializerPathsShouldWorkWhenFallbackDisabled() {
        val wrapperSerializer = ValueWrapperSerializer(Flt64)
        val wrapper = ValueWrapper(Flt64.two, Flt64).value!!
        val wrapperJson = Json.encodeToString(wrapperSerializer, wrapper)
        val decodedWrapper = Json.decodeFromString(wrapperSerializer, wrapperJson)
        assertTrue(decodedWrapper eq Flt64.two)

        val range = ValueRange(Flt64.one, Flt64.three, Interval.Closed, Interval.Closed, Flt64).value!!
        val rangeJson = Json.encodeToString(ValueRangeFlt64Serializer, range)
        val decodedRange = Json.decodeFromString(ValueRangeFlt64Serializer, rangeJson)
        assertTrue(decodedRange.contains(Flt64.two))
    }
}
