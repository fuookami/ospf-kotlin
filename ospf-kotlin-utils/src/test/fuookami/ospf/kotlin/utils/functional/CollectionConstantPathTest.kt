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
        val list = listOf(Int64.one, Int64.two, Int64.three)
        assertEquals(Int64(6), list.sum(Int64))
        assertEquals(Int64(6), list.sumOf(Int64) { it })
        assertEquals(Int64(6), list.asSequence().sum(Int64))
        assertEquals(Int64(6), list.asSequence().sumOf(Int64) { it })

        val map = mapOf("a" to Int64.one, "b" to Int64.two, "c" to Int64.three)
        assertEquals(Int64(6), map.sum(Int64))
        assertEquals(Int64(6), map.sumOf(Int64) { it.value })
    }

    @Test
    fun reifiedDefaultPathsShouldThrowWhenFallbackDisabled() {
        val list = listOf(Int64.one, Int64.two, Int64.three)
        assertFailsWith<IllegalStateException> { list.sum<Int64>() }
        assertFailsWith<IllegalStateException> { list.sumOf<Int64, Int64> { it } }
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
