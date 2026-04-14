package fuookami.ospf.kotlin.core.expression.flatten

import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenData
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenData
import fuookami.ospf.kotlin.core.intermediate_symbol.flatten.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for FlattenUtility functions
 */
class FlattenUtilityTest {

    // ========== Linear Merge Tests ==========

    @Test
    fun mergeLinearMonomials_shouldCombineSameVariables() {
        val x = RealVar("x")
        val y = RealVar("y")

        val monomials = listOf(
            UtilsLinearMonomial(Flt64(2.0), x),
            UtilsLinearMonomial(Flt64(3.0), x),  // Same variable as first
            UtilsLinearMonomial(Flt64(1.0), y)
        )

        val result = mergeLinearMonomials(monomials, Flt64(5.0))

        assertEquals(2, result.monomials.size, "Should have 2 unique variables")
        assertTrue(result.constant eq Flt64(5.0), "Constant should be 5.0")

        // Check x coefficient (2 + 3 = 5)
        val xMonomial = result.monomials.find { it.symbol == x }
        assertNotNull(xMonomial, "x monomial should exist")
        assertTrue(xMonomial.coefficient eq Flt64(5.0), "x coefficient should be 5.0")

        // Check y coefficient
        val yMonomial = result.monomials.find { it.symbol == y }
        assertNotNull(yMonomial, "y monomial should exist")
        assertTrue(yMonomial.coefficient eq Flt64(1.0), "y coefficient should be 1.0")
    }

    @Test
    fun mergeLinearMonomials_shouldFilterZeroCoefficients() {
        val x = RealVar("x")
        val y = RealVar("y")

        val monomials = listOf(
            UtilsLinearMonomial(Flt64(2.0), x),
            UtilsLinearMonomial(Flt64.zero, y)  // Zero coefficient
        )

        val result = mergeLinearMonomials(monomials, Flt64.zero)

        assertEquals(1, result.monomials.size, "Should filter out zero coefficients")
    }

    @Test
    fun mergeLinearFlattenData_shouldCombineMultipleData() {
        val x = RealVar("x")
        val y = RealVar("y")

        val data1 = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64(2.0), x)),
            constant = Flt64(1.0)
        )
        val data2 = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64(3.0), x), UtilsLinearMonomial(Flt64(1.0), y)),
            constant = Flt64(2.0)
        )

        val result = mergeLinearFlattenData(listOf(data1, data2))

        assertEquals(2, result.monomials.size, "Should have 2 unique variables")
        assertTrue(result.constant eq Flt64(3.0), "Constant should be 1.0 + 2.0 = 3.0")

        val xMonomial = result.monomials.find { it.symbol == x }
        assertNotNull(xMonomial, "x monomial should exist")
        assertTrue(xMonomial.coefficient eq Flt64(5.0), "x coefficient should be 2.0 + 3.0 = 5.0")
    }

    // ========== Quadratic Merge Tests ==========

    @Test
    fun mergeQuadraticMonomials_shouldCombineSymmetricTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        // x*y and y*x should be merged
        val monomials = listOf(
            UtilsQuadraticMonomial(Flt64(2.0), x, y),
            UtilsQuadraticMonomial(Flt64(3.0), y, x)  // Symmetric to first
        )

        val result = mergeQuadraticMonomials(monomials, Flt64.zero)

        assertEquals(1, result.monomials.size, "Symmetric terms should be merged")
        assertTrue(result.monomials[0].coefficient eq Flt64(5.0), "Coefficient should be 2.0 + 3.0 = 5.0")
    }

    @Test
    fun mergeQuadraticMonomials_shouldHandleLinearTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        val monomials = listOf(
            UtilsQuadraticMonomial(Flt64(2.0), x, null),  // Linear term
            UtilsQuadraticMonomial(Flt64(3.0), x, y)     // Quadratic term
        )

        val result = mergeQuadraticMonomials(monomials, Flt64.zero)

        assertEquals(2, result.monomials.size, "Should have 1 linear and 1 quadratic term")
    }

    @Test
    fun mergeQuadraticFlattenData_shouldCombineMultipleData() {
        val x = RealVar("x")
        val y = RealVar("y")

        val data1 = QuadraticFlattenData(
            monomials = listOf(UtilsQuadraticMonomial(Flt64(2.0), x, y)),
            constant = Flt64(1.0)
        )
        val data2 = QuadraticFlattenData(
            monomials = listOf(UtilsQuadraticMonomial(Flt64(3.0), y, x)),  // Symmetric to data1
            constant = Flt64(2.0)
        )

        val result = mergeQuadraticFlattenData(listOf(data1, data2))

        assertEquals(1, result.monomials.size, "Symmetric terms should be merged")
        assertTrue(result.constant eq Flt64(3.0), "Constant should be 1.0 + 2.0 = 3.0")
    }

    // ========== Multiply Tests ==========

    @Test
    fun multiplyLinear_shouldProduceAllTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        // (x + 1) * (y + 1) = xy + x + y + 1
        val lhs = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64.one, x)),
            constant = Flt64.one
        )
        val rhs = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64.one, y)),
            constant = Flt64.one
        )

        val result = multiplyLinear(lhs, rhs)

        assertEquals(3, result.monomials.size, "Should have xy, x, y terms")
        assertTrue(result.constant eq Flt64.one, "Constant should be 1.0")
    }

    @Test
    fun multiplyLinear_withZeroConstant_shouldOnlyHaveProductTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        // x * y = xy (no linear terms)
        val lhs = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val rhs = LinearFlattenData(
            monomials = listOf(UtilsLinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val result = multiplyLinear(lhs, rhs)

        assertEquals(1, result.monomials.size, "Should only have xy term")
        assertTrue(result.constant eq Flt64.zero, "Constant should be 0.0")

        val xy = result.monomials[0]
        assertTrue(xy.symbol2 != null, "Should be quadratic term")
    }

    // ========== Normalize Tests ==========

    @Test
    fun normalizeLinear_shouldFilterZeroCoefficients() {
        val x = RealVar("x")
        val y = RealVar("y")

        val data = LinearFlattenData(
            monomials = listOf(
                UtilsLinearMonomial(Flt64(2.0), x),
                UtilsLinearMonomial(Flt64.zero, y)  // Zero coefficient
            ),
            constant = Flt64(5.0)
        )

        val result = normalizeLinear(data)

        assertEquals(1, result.monomials.size, "Should filter zero coefficients")
        assertTrue(result.constant eq Flt64(5.0), "Constant should be preserved")
    }

    @Test
    fun normalizeQuadratic_shouldFilterAndCanonicalize() {
        val x = RealVar("x")
        val y = RealVar("y")

        val data = QuadraticFlattenData(
            monomials = listOf(
                UtilsQuadraticMonomial(Flt64(2.0), y, x),  // Different order
                UtilsQuadraticMonomial(Flt64.zero, x, null)  // Zero coefficient
            ),
            constant = Flt64(5.0)
        )

        val result = normalizeQuadratic(data)

        assertEquals(1, result.monomials.size, "Should filter zero and normalize")
        assertTrue(result.constant eq Flt64(5.0), "Constant should be preserved")
    }
}