package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Regression tests for P4-1 Phase A2: MathFunctionSymbol migration.
 */
class FunctionSymbolMigrationTest {

    @Test
    fun `LinearFunctionSymbolAdapter has correct default properties`() {
        val x = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("x"))),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = x, y = LinearPolynomial(emptyList(), Flt64.zero),
            name = "test_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack)

        assertEquals(Linear, adapter.category)
        assertFalse(adapter.cached)
        assertFalse(adapter.discrete)
        assertEquals(0, adapter.index)
        assertEquals("test_slack", adapter.name)
    }

    @Test
    fun `LinearFunctionSymbolAdapter delegates name and displayName`() {
        val x = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("x"))),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = x, y = LinearPolynomial(emptyList(), Flt64.zero),
            name = "my_func", displayName = "My Function"
        )
        val adapter = LinearFunctionSymbolAdapter(slack)

        assertEquals("my_func", adapter.name)
        assertEquals("My Function", adapter.displayName)

        adapter.name = "new_name"
        adapter.displayName = "New Display"
        assertEquals("new_name", slack.name)
        assertEquals("New Display", slack.displayName)
    }

    @Test
    fun `SlackFunction creates helper variables for pos and neg`() {
        val x = RealVar("slack_test_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            withNegative = true,
            withPositive = true,
            name = "slack_test"
        )

        assertEquals(2, slack.helperVariables.size, "SlackFunction should have 2 helper variables (pos + neg)")
    }

    @Test
    fun `SlackFunction register adds helper variables to model`() {
        val x = RealVar("aux_test_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            withNegative = true,
            withPositive = true,
            name = "aux_slack"
        )

        val model = LinearMetaModel<Flt64>(name = "stub_model")
        val result = slack.register(model)
        assertTrue(result is Ok, "register should succeed")
    }

    @Test
    fun `LinearFunctionSymbolAdapter register delegates to inner function`() {
        val x = RealVar("adapter_aux_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            withNegative = true,
            withPositive = true,
            name = "adapter_aux"
        )
        val adapter = LinearFunctionSymbolAdapter(slack)

        val model = LinearMetaModel<Flt64>(name = "stub_model")
        val result = adapter.register(model)
        assertTrue(result is Ok, "adapter.register should succeed")
    }

    @Test
    fun `LinearFunctionSymbolAdapter evaluate delegates to inner function`() {
        val x = RealVar("eval_test_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            name = "eval_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack)

        val values = mapOf<Symbol, Flt64>(x to Flt64(5.0))
        val result = adapter.evaluate(values)
        assertNotNull(result, "evaluate should return a value when all symbols are provided")
    }

    @Test
    fun `LinearFunctionSymbolAdapter flattenedMonomials is empty by design`() {
        val x = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("x"))),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = x, y = LinearPolynomial(emptyList(), Flt64.zero),
            name = "flatten_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack)

        assertTrue(adapter.flattenedMonomials.monomials.isEmpty())
        assertEquals(Flt64.zero, adapter.flattenedMonomials.constant)
    }

    @Test
    fun `MathFunctionSymbol register adds helper variables`() {
        val model = LinearMetaModel<Flt64>(name = "independent_test_model")

        val testFunc = object : MathFunctionSymbol<Flt64> {
            override var name: String = "independent_test"
            override var displayName: String? = null
            override val helperVariables: List<AbstractVariableItem<*, *>> = emptyList()
            override fun evaluate(values: Map<Symbol, Flt64>): Flt64? = null
            override fun register(model: fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel<Flt64>): Try {
                return ok
            }
        }

        val result = testFunc.register(model)
        assertTrue(result is Ok, "register should succeed")
    }
}
