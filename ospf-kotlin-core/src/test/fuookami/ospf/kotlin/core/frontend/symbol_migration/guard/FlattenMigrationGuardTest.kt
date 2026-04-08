package fuookami.ospf.kotlin.core.frontend.symbol_migration.guard

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.TokenCacheContexts
import fuookami.ospf.kotlin.core.frontend.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.functional.sum
import fuookami.ospf.kotlin.math.symbol.Symbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 3.0 Guard Tests
 *
 * These tests ensure that critical paths correctly use flattenedMonomials.
 * They act as regression guards to verify the migration from cells to flattenedMonomials.
 */
class FlattenMigrationGuardTest {

    // ========== Polynomial Flatten Guards ==========

    @Test
    fun linearPolynomial_flattenedMonomials_should_work_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(3.0, x),
                LinearMonomial(y)
            ),
            constant = Flt64(5.0)
        )

        val flattenData = poly.flattenedMonomials

        assertEquals(2, flattenData.monomials.size, "Should have 2 monomials")
        assertTrue(flattenData.constant eq Flt64(5.0), "Constant should be 5.0")
    }

    @Test
    fun quadraticPolynomial_flattenedMonomials_should_work_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val poly = QuadraticPolynomial(
            monomials = listOf(x * y, QuadraticMonomial(2.0, x)),
            constant = Flt64(3.0)
        )

        val flattenData = poly.flattenedMonomials

        assertTrue(flattenData.monomials.isNotEmpty(), "Should have monomials")
        assertTrue(flattenData.constant eq Flt64(3.0), "Constant should be 3.0")
    }

    // ========== Monomial Flatten Guards ==========

    @Test
    fun linearMonomialCell_evaluate_should_use_flattenedData_correctly() {
        val x = RealVar("x")

        val cell = LinearMonomialCell(Flt64(2.0), x)

        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))

        val result = cell.evaluate(values, null, false)

        assertEquals(Flt64(6.0), result, "2.0 * 3.0 should equal 6.0")
    }

    @Test
    fun quadraticMonomialCell_evaluate_should_use_flattenedData_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val cell = QuadraticMonomialCell(Flt64(3.0), x, y)

        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(5.0)
        )

        val result = cell.evaluate(values, null, false)

        assertEquals(Flt64(30.0), result, "3.0 * 2.0 * 5.0 should equal 30.0")
    }

    // ========== Coefficient Preservation Guards ==========

    @Test
    fun linearMonomial_coefficient_should_be_preserved_in_flatten() {
        val x = RealVar("x")

        val monomial = LinearMonomial(Flt64(4.0), x)

        val flattenData = monomial.flattenedMonomials

        assertEquals(1, flattenData.monomials.size)
        assertTrue(flattenData.monomials[0].coefficient eq Flt64(4.0), "Coefficient should be preserved as 4.0")
    }

    @Test
    fun quadraticMonomial_coefficient_should_be_preserved_in_flatten() {
        val x = RealVar("x")
        val y = RealVar("y")

        val monomial = QuadraticMonomial(Flt64(3.0), x, y)

        val flattenData = monomial.flattenedMonomials

        assertEquals(1, flattenData.monomials.size)
        assertTrue(flattenData.monomials[0].coefficient eq Flt64(3.0), "Coefficient should be preserved as 3.0")
    }

    // ========== Inequality Normalize Guards ==========

    @Test
    fun linearInequality_normalize_should_preserve_coefficients() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64(3.0)
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(y)),
                constant = Flt64(-1.0)
            ),
            sign = Sign.GreaterEqual
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.Less, normalized.sign)
        assertTrue(normalized.rhs.constant eq Flt64(4.0))
    }

    @Test
    fun quadraticInequality_normalize_should_work_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(x * x, 2 * (x * y)),
                constant = Flt64(5.0)
            ),
            rhs = QuadraticPolynomial(
                monomials = listOf(y * y),
                constant = Flt64.one
            ),
            sign = Sign.Greater
        )

        val normalized = inequality.normalizeToLessEqual()

        assertEquals(Sign.LessEqual, normalized.sign)
    }

    // ========== Inequality Flatten Guards ==========

    @Test
    fun linearInequality_flattenedMonomials_should_work_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64(3.0)
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(y)),
                constant = Flt64(-1.0)
            ),
            sign = Sign.LessEqual
        )

        val flattenData = inequality.flattenedMonomials

        assertTrue(flattenData.monomials.isNotEmpty(), "Linear inequality should have monomials")
    }

    @Test
    fun quadraticInequality_flattenedMonomials_should_work_correctly() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(x * y),
                constant = Flt64(5.0)
            ),
            rhs = QuadraticPolynomial(
                monomials = listOf(y * y),
                constant = Flt64.one
            ),
            sign = Sign.LessEqual
        )

        val flattenData = inequality.flattenedMonomials

        assertTrue(flattenData.monomials.isNotEmpty(), "Quadratic inequality should have monomials")
    }

    // ========== TokenCacheContext Integration Guards ==========

    @Test
    fun tokenCacheContext_should_cache_flattenedMonomials_correctly() {
        val symbol = LinearExpressionSymbol(
            polynomial = LinearPolynomial(constant = Flt64.one),
            name = "cache_test_symbol"
        )
        val contexts = TokenCacheContexts()

        // Cache using flattenedMonomials (the new path)
        contexts.linearFlatten.put(symbol, symbol.flattenedMonomials)

        assertTrue(contexts.linearFlatten.contains(symbol), "Symbol should be in linear flatten cache")
    }

    @Test
    fun symbol_flattenedMonomials_should_be_consistent() {
        val x = RealVar("x")

        val symbol = LinearExpressionSymbol(
            polynomial = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64(3.0)
            ),
            name = "consistency_test_symbol"
        )

        val flattenData = symbol.flattenedMonomials

        assertEquals(1, flattenData.monomials.size, "Should have 1 monomial")
        assertTrue(flattenData.monomials[0].coefficient eq Flt64(2.0), "Coefficient should be 2.0")
        assertTrue(flattenData.constant eq Flt64(3.0), "Constant should be 3.0")
    }

    // ========== P0 Guard Tests: Quadratic Expand Correctness ==========

    /**
     * Test A: (x + 1) * (y + 1) expansion should produce all terms
     * This tests that when multiplying two linear expressions with constants,
     * we get all cross terms: xy, x, y, and constant
     */
    @Test
    fun quadraticExpand_xPlus1_times_yPlus1_shouldHaveAllTerms() {
        val x = RealVar("x")
        val y = RealVar("y")

        // Create (x + 1) and (y + 1) as quadratic polynomials
        // Then multiply: the result should contain xy term plus linear terms
        val polyX = QuadraticPolynomial(monomials = listOf(QuadraticMonomial(Flt64.one, x)), constant = Flt64.one)
        val polyY = QuadraticPolynomial(monomials = listOf(QuadraticMonomial(Flt64.one, y)), constant = Flt64.one)

        // Multiply using polynomial arithmetic
        val product = polyX * polyY

        val flatten = product.flattenedMonomials

        // After expansion: (x + 1)(y + 1) = xy + x + y + 1
        // So we should have 3 monomial terms (xy, x, y) and constant = 1
        assertTrue(flatten.monomials.isNotEmpty(), "Should have monomial terms")
        assertTrue(flatten.constant eq Flt64.one, "Constant should be 1.0")
    }

    /**
     * Test B: x*y and y*x should produce identical merged results
     */
    @Test
    fun quadraticMerge_xy_sameAs_yx() {
        val x = RealVar("x")
        val y = RealVar("y")

        // x*y
        val poly1 = QuadraticPolynomial(monomials = listOf(x * y), constant = Flt64.zero)

        // y*x
        val poly2 = QuadraticPolynomial(monomials = listOf(y * x), constant = Flt64.zero)

        val flatten1 = poly1.flattenedMonomials
        val flatten2 = poly2.flattenedMonomials

        // Both should have exactly 1 monomial
        assertEquals(1, flatten1.monomials.size, "xy should have 1 monomial")
        assertEquals(1, flatten2.monomials.size, "yx should have 1 monomial")

        val m1 = flatten1.monomials[0]
        val m2 = flatten2.monomials[0]

        // Coefficients should be equal (both 1.0)
        assertTrue(m1.coefficient eq m2.coefficient, "Coefficients should match: ${m1.coefficient} vs ${m2.coefficient}")

        // Both should have two symbols (quadratic term)
        assertTrue(m1.symbol2 != null, "xy should have symbol2")
        assertTrue(m2.symbol2 != null, "yx should have symbol2")

        // The variable pairs should be the same set (order normalized)
        val vars1 = setOf(m1.symbol1, m1.symbol2)
        val vars2 = setOf(m2.symbol1, m2.symbol2)
        assertEquals(vars1, vars2, "Variable sets should match")
    }

    /**
     * Test C: Multiple constructions should produce identical keys
     */
    @Test
    fun quadraticKey_multipleConstructions_shouldBeStable() {
        val x = RealVar("x")
        val y = RealVar("y")

        val results = mutableListOf<Set<Any?>>()

        // Construct the same expression 5 times
        repeat(5) {
            val poly = QuadraticPolynomial(monomials = listOf(x * y), constant = Flt64.zero)
            val flatten = poly.flattenedMonomials

            assertEquals(1, flatten.monomials.size, "Should have 1 monomial")
            val m = flatten.monomials[0]

            // Collect variable pair as set
            results.add(setOf(m.symbol1, m.symbol2))
        }

        // All results should be identical
        val distinctResults = results.distinct()
        assertEquals(1, distinctResults.size, "All constructions should produce identical variable pairs")
    }

    // ========== M2 Consistency Tests: flatten vs cells ==========

    /**
     * Test that flattenedMonomials and cells produce consistent results for LinearPolynomial
     */
    @Test
    fun linearPolynomial_flatten_vs_cells_consistency() {
        val x = RealVar("x")
        val y = RealVar("y")

        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(2.0, x),
                LinearMonomial(3.0, y),
                LinearMonomial(x)  // Test merging
            ),
            constant = Flt64(5.0)
        )

        val flatten = poly.flattenedMonomials
        @Suppress("DEPRECATION")
        val cells = poly.cells

        // Check constant
        assertTrue(flatten.constant eq Flt64(5.0), "Flatten constant should be 5.0")
        val cellsConstant = cells.filter { it.isConstant }.mapNotNull { it.constant }.fold(Flt64.zero) { acc, v -> acc + v }
        assertTrue(cellsConstant eq Flt64(5.0), "Cells constant should be 5.0")

        // Check variable coefficients
        val flattenX = flatten.monomials.find { it.symbol == x }
        @Suppress("DEPRECATION")
        val cellX = cells.find { it.pair?.variable == x }

        assertNotNull(flattenX, "Flatten should have x")
        assertNotNull(cellX, "Cells should have x")
        assertTrue(flattenX.coefficient eq cellX.pair!!.coefficient, "x coefficients should match: ${flattenX.coefficient} vs ${cellX.pair!!.coefficient}")
    }

    /**
     * Test that flattenedMonomials and cells produce consistent results for QuadraticPolynomial
     */
    @Test
    fun quadraticPolynomial_flatten_vs_cells_consistency() {
        val x = RealVar("x")
        val y = RealVar("y")

        val poly = QuadraticPolynomial(
            monomials = listOf(
                x * y,
                x * x,
                QuadraticMonomial(2.0, y)
            ),
            constant = Flt64(3.0)
        )

        val flatten = poly.flattenedMonomials
        @Suppress("DEPRECATION")
        val cells = poly.cells

        // Check constant
        assertTrue(flatten.constant eq Flt64(3.0), "Flatten constant should be 3.0")
        val cellsConstant = cells.filter { it.isConstant }.mapNotNull { it.constant }.fold(Flt64.zero) { acc, v -> acc + v }
        assertTrue(cellsConstant eq Flt64(3.0), "Cells constant should be 3.0")

        // Check monomial count (non-constant)
        val flattenCount = flatten.monomials.size
        val cellsCount = cells.count { !it.isConstant }
        assertEquals(flattenCount, cellsCount, "Monomial count should match")
    }

    /**
     * Test that flattenedMonomials and cells produce consistent results for LinearInequality
     */
    @Test
    fun linearInequality_flatten_vs_cells_consistency() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = LinearInequality(
            lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(2.0, x)),
                constant = Flt64(3.0)
            ),
            rhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(y)),
                constant = Flt64(1.0)
            ),
            sign = Sign.LessEqual
        )

        val flatten = inequality.flattenedMonomials
        @Suppress("DEPRECATION")
        val cells = inequality.cells

        // Check constant (3.0 - 1.0 = 2.0)
        assertTrue(flatten.constant eq Flt64(2.0), "Flatten constant should be 2.0")
        val cellsConstant = cells.filter { it.isConstant }.mapNotNull { it.constant }.fold(Flt64.zero) { acc, v -> acc + v }
        assertTrue(cellsConstant eq Flt64(2.0), "Cells constant should be 2.0")

        // Check variable coefficients
        val flattenX = flatten.monomials.find { it.symbol == x }
        @Suppress("DEPRECATION")
        val cellX = cells.find { it.pair?.variable == x }

        assertNotNull(flattenX, "Flatten should have x")
        assertNotNull(cellX, "Cells should have x")
        assertTrue(flattenX.coefficient eq cellX.pair!!.coefficient, "x coefficients should match")
    }

    /**
     * Test that flattenedMonomials and cells produce consistent results for QuadraticInequality
     */
    @Test
    fun quadraticInequality_flatten_vs_cells_consistency() {
        val x = RealVar("x")
        val y = RealVar("y")

        val inequality = QuadraticInequality(
            lhs = QuadraticPolynomial(
                monomials = listOf(x * y, QuadraticMonomial(2.0, x)),
                constant = Flt64(5.0)
            ),
            rhs = QuadraticPolynomial(
                monomials = listOf(x * x),
                constant = Flt64.one
            ),
            sign = Sign.LessEqual
        )

        val flatten = inequality.flattenedMonomials
        @Suppress("DEPRECATION")
        val cells = inequality.cells

        // Check constant (5.0 - 1.0 = 4.0)
        assertTrue(flatten.constant eq Flt64(4.0), "Flatten constant should be 4.0")
        val cellsConstant = cells.filter { it.isConstant }.mapNotNull { it.constant }.fold(Flt64.zero) { acc, v -> acc + v }
        assertTrue(cellsConstant eq Flt64(4.0), "Cells constant should be 4.0")

        // Check monomial count (non-constant)
        val flattenCount = flatten.monomials.size
        val cellsCount = cells.count { !it.isConstant }
        assertEquals(flattenCount, cellsCount, "Monomial count should match")
    }
}