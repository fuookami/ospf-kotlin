package fuookami.ospf.kotlin.core.expression.monomial

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.AutoTokenList
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for coefficient preservation in LinearMonomialCell and QuadraticMonomialCell evaluate methods.
 * These tests verify fixes for bugs where coefficients were lost during evaluation.
 */
class MonomialCoefficientPreservationTest {

    // ========== LinearMonomialCell Tests ==========

    @Test
    fun linearMonomialCell_evaluateWithValues_shouldPreserveCoefficient() {
        // Create a linear monomial cell with coefficient 3.0
        val variable = RealVar("x")
        val cell = LinearMonomialCell(Flt64(3.0), variable)

        // Evaluate with values map containing variable value 2.0
        val values = mapOf<Symbol, Flt64>(variable to Flt64(2.0))

        val result = cell.evaluate(values, null, false)

        // Expected: 3.0 * 2.0 = 6.0
        assertEquals(Flt64(6.0), result)
    }

    @Test
    fun linearMonomialCell_evaluateWithNegativeCoefficient_shouldWorkCorrectly() {
        val variable = RealVar("y")
        val cell = LinearMonomialCell(Flt64(-2.0), variable)

        val values = mapOf<Symbol, Flt64>(variable to Flt64(3.0))

        val result = cell.evaluate(values, null, false)

        // Expected: -2.0 * 3.0 = -6.0
        assertEquals(Flt64(-6.0), result)
    }

    // ========== QuadraticMonomialCell Tests ==========

    @Test
    fun quadraticMonomialCell_evaluateWithValues_linearTerm_shouldPreserveCoefficient() {
        // Linear term (variable2 = null)
        val variable = RealVar("z")
        val cell = QuadraticMonomialCell(Flt64(7.0), variable, null)

        val values = mapOf<Symbol, Flt64>(variable to Flt64(2.0))

        val result = cell.evaluate(values, null, false)

        // Expected: 7.0 * 2.0 = 14.0
        assertEquals(Flt64(14.0), result)
    }

    @Test
    fun quadraticMonomialCell_evaluateWithValues_quadraticTerm_shouldPreserveCoefficient() {
        val x = RealVar("x")
        val y = RealVar("y")
        val cell = QuadraticMonomialCell(Flt64(3.0), x, y)

        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(5.0)
        )

        val result = cell.evaluate(values, null, false)

        // Expected: 3.0 * 2.0 * 5.0 = 30.0
        assertEquals(Flt64(30.0), result)
    }

    @Test
    fun quadraticMonomialCell_evaluateWithNegativeCoefficient_shouldWorkCorrectly() {
        val x = RealVar("x")
        val y = RealVar("y")
        val cell = QuadraticMonomialCell(Flt64(-1.5), x, y)

        val values = mapOf<Symbol, Flt64>(
            x to Flt64(2.0),
            y to Flt64(4.0)
        )

        val result = cell.evaluate(values, null, false)

        // Expected: -1.5 * 2.0 * 4.0 = -12.0
        assertEquals(Flt64(-12.0), result)
    }

    @Test
    fun quadraticMonomialCell_evaluateSquaredTerm_shouldPreserveCoefficient() {
        // x^2 term (same variable twice)
        val x = RealVar("x")
        val cell = QuadraticMonomialCell(Flt64(4.0), x, x)

        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))

        val result = cell.evaluate(values, null, false)

        // Expected: 4.0 * 3.0 * 3.0 = 36.0
        assertEquals(Flt64(36.0), result)
    }

    // ========== QuadraticMonomialCell equals Tests ==========

    @Test
    fun quadraticMonomialCell_equals_shouldReturnTrueForSameType() {
        val x = RealVar("x")
        val cell1 = QuadraticMonomialCell(Flt64(2.0), x, null)
        val cell2 = QuadraticMonomialCell(Flt64(2.0), x, null)

        assertEquals(cell1, cell2)
    }

    @Test
    fun quadraticMonomialCell_equals_shouldReturnFalseForLinearMonomialCell() {
        val x = RealVar("x")
        val quadraticCell = QuadraticMonomialCell(Flt64(2.0), x, null)
        val linearCell = LinearMonomialCell(Flt64(2.0), x)

        // Should not be equal because they are different types
        assertFalse(quadraticCell == linearCell)
        assertFalse(linearCell == quadraticCell)
    }

    @Test
    fun quadraticMonomialCell_equals_shouldReturnFalseForDifferentCoefficients() {
        val x = RealVar("x")
        val cell1 = QuadraticMonomialCell(Flt64(2.0), x, null)
        val cell2 = QuadraticMonomialCell(Flt64(3.0), x, null)

        assertNotEquals(cell1, cell2)
    }

    @Test
    fun quadraticMonomialCell_hashCode_shouldBeConsistentWithEquals() {
        val x = RealVar("x")
        val y = RealVar("y")
        val cell1 = QuadraticMonomialCell(Flt64(2.0), x, y)
        val cell2 = QuadraticMonomialCell(Flt64(2.0), x, y)

        assertEquals(cell1.hashCode(), cell2.hashCode())
        assertTrue(cell1 == cell2)
    }

    // ========== Integration Tests with Monomial (higher-level API) ==========

    @Test
    fun linearMonomial_evaluate_shouldPreserveCoefficient() {
        val x = RealVar("x")
        val monomial = LinearMonomial(Flt64(5.0), LinearMonomialSymbol(x))

        val values = mapOf<Symbol, Flt64>(x to Flt64(3.0))

        val result = monomial.evaluate(values, null as AbstractTokenList?, false)

        // Expected: 5.0 * 3.0 = 15.0
        assertEquals(Flt64(15.0), result)
    }

    @Test
    fun quadraticMonomial_evaluate_shouldPreserveCoefficient() {
        val x = RealVar("x")
        val y = RealVar("y")
        val monomial = QuadraticMonomial(
            coefficient = Flt64(2.0),
            symbol = QuadraticMonomialSymbol(x, y)
        )

        val values = mapOf<Symbol, Flt64>(
            x to Flt64(4.0),
            y to Flt64(3.0)
        )

        val result = monomial.evaluate(values, null as AbstractTokenList?, false)

        // Expected: 2.0 * 4.0 * 3.0 = 24.0
        assertEquals(Flt64(24.0), result)
    }
}