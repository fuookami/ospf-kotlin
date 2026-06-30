package fuookami.ospf.kotlin.math.algebra.value_range

import kotlin.test.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.concept.CompanionConstantProviderResolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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

        val gr = ValueRange.gr(Flt64.one, Flt64).value!!
        assertTrue(!gr.contains(Flt64.one))
        assertTrue(gr.contains(Flt64.two))

        val leq = ValueRange.leq(Flt64.three, Interval.Closed, Flt64).value!!
        assertTrue(leq.contains(Flt64.two))

        val ls = ValueRange.ls(Flt64.three, Flt64).value!!
        assertTrue(!ls.contains(Flt64.three))
        assertTrue(ls.contains(Flt64.two))

        val upperInfinite = ValueRange(Flt64.one, Infinity, Interval.Closed, Flt64).value!!
        assertTrue(upperInfinite.contains(Flt64.ten))

        val lowerInfinite = ValueRange(NegativeInfinity, Flt64.three, Interval.Closed, Flt64).value!!
        assertTrue(lowerInfinite.contains(-Flt64.ten))
    }

    @Test
    /** 验证禁用回退时具态构造器失败 / Verify reified constructors fail when fallback disabled */
    fun reifiedConstructorsShouldFailWhenFallbackDisabled() {
        assertTrue(ValueRange<Flt64>().failed)
        assertTrue(ValueRange(Flt64.one).failed)
        assertTrue(ValueRange(Flt64.one, Flt64.three).failed)
        assertTrue(ValueRange.geq(Flt64.one).failed)
        assertTrue(ValueRange.gr(Flt64.one).failed)
        assertTrue(ValueRange.leq(Flt64.three).failed)
        assertTrue(ValueRange.ls(Flt64.three).failed)
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

    @Test
    fun boundToFlt64ShouldWorkWithoutReflectionFallback() {
        System.clearProperty(CompanionConstantProviderResolver.reflectionFallbackEnabledProperty)

        val bound = Bound(
            value = ValueWrapper(Flt64(200.0), Flt64).value!!,
            interval = Interval.Closed
        )

        val converted = bound.toFlt64()

        assertEquals(Flt64(200.0), converted.value.unwrap())
        assertEquals(Interval.Closed, converted.interval)
    }

    @Test
    fun infiniteBoundToFlt64ShouldKeepInfinityAndOpenIntervalWithoutReflectionFallback() {
        System.clearProperty(CompanionConstantProviderResolver.reflectionFallbackEnabledProperty)

        val upper = Bound(ValueWrapper.Infinity(Flt64), Interval.Closed)
        val converted = upper.toFlt64()

        assertTrue(converted.value.isInfinity)
        assertEquals(Interval.Open, converted.interval)
    }

    @Test
    fun negativeInfinityCopyAndCloneShouldPreserveSign() {
        val negInf = ValueWrapper(NegativeInfinity, Flt64)
        assertTrue(negInf is ValueWrapper.NegativeInfinity)

        val copied = negInf.copy()
        val cloned = negInf.clone()
        assertEquals("-inf", copied.toString())
        assertEquals("-inf", cloned.toString())
    }

    @Test
    fun containsRangeShouldRespectInfiniteBoundsAndIntervals() {
        val geqOne = ValueRange.geq(Flt64.one, Interval.Closed, Flt64).value!!
        val leftSegment = ValueRange(NegativeInfinity, Flt64.zero, Interval.Closed, Flt64).value!!
        assertTrue(!(geqOne contains leftSegment))

        val rightTail = ValueRange(Flt64.two, Infinity, Interval.Open, Flt64).value!!
        assertTrue(geqOne.contains(rightTail))

        val leqThree = ValueRange.leq(Flt64.three, Interval.Closed, Flt64).value!!
        val beyondUpper = ValueRange(Flt64(4.0), Infinity, Interval.Open, Flt64).value!!
        assertTrue(!(leqThree contains beyondUpper))

        val openRange = ValueRange(Flt64.one, Flt64.three, Interval.Open, Interval.Open, Flt64).value!!
        val closedLeft = ValueRange(Flt64.one, Flt64.two, Interval.Closed, Interval.Closed, Flt64).value!!
        val openLeft = ValueRange(Flt64.one, Flt64.two, Interval.Open, Interval.Closed, Flt64).value!!
        assertTrue(!(openRange contains closedLeft))
        assertTrue(openRange.contains(openLeft))
    }
}
