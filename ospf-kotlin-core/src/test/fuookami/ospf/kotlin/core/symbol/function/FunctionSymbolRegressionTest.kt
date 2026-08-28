package fuookami.ospf.kotlin.core.symbol.function

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * MathFunctionSymbol 行为回归测试。
 * Regression tests for MathFunctionSymbol behavior.
 */
class FunctionSymbolRegressionTest {

    @Test
    fun `LinearFunctionSymbolAdapter has correct default properties`() {
        val x = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("x"))),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = x, y = LinearPolynomial(emptyList(), Flt64.zero),
            converter = flt64TestConverter,
            name = "test_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)

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
            converter = flt64TestConverter,
            name = "my_func", displayName = "My Function"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)

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
            converter = flt64TestConverter,
            name = "slack_test"
        )

        assertEquals(2, slack.helperVariables.size, "SlackFunction should have 2 helper variables (pos + neg)")
    }

    @Test
    fun `SlackFunction registerAuxiliaryTokens succeeds`() {
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
            converter = flt64TestConverter,
            name = "aux_slack"
        )

        val tokens = fuookami.ospf.kotlin.core.token.AutoTokenTable<Flt64>(Linear, false)
        tokens.add(listOf(x))
        val result = slack.registerAuxiliaryTokens(tokens)
        assertTrue(result is Ok, "registerAuxiliaryTokens should succeed")
    }

    @Test
    fun `LinearFunctionSymbolAdapter registerAuxiliaryTokens delegates to inner function`() {
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
            converter = flt64TestConverter,
            name = "adapter_aux"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)

        val tokens = fuookami.ospf.kotlin.core.token.AutoTokenTable<Flt64>(Linear, false)
        tokens.add(listOf(x))
        val result = adapter.registerAuxiliaryTokens(tokens)
        assertTrue(result is Ok, "adapter.registerAuxiliaryTokens should succeed")
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
            converter = flt64TestConverter,
            name = "eval_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)

        val values = mapOf<Symbol, Flt64>(x to Flt64(5.0))
        val result = adapter.evaluate(values)
        assertNotNull(result, "evaluate should return a value when all symbols are provided")
    }

    @Test
    fun `LinearFunctionSymbolAdapter flattenedMonomials expose delegate result polynomial`() {
        val x = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, RealVar("x"))),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = x, y = LinearPolynomial(emptyList(), Flt64.zero),
            converter = flt64TestConverter,
            name = "flatten_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)

        assertEquals(2, adapter.flattenedMonomials.monomials.size)
        assertEquals(setOf(slack.negVar, slack.posVar), adapter.flattenedMonomials.monomials.map { it.symbol }.toSet())
        assertEquals(Flt64.zero, adapter.flattenedMonomials.constant)
    }

    @Test
    fun `LinearFunctionSymbolAdapter resolves legacy function result variables`() {
        val x = RealVar("legacy_result_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val sameAs = SameAsFunction(
            inequalities = listOf(
                LinearInequality(
                    lhs = xPoly,
                    rhs = LinearPolynomial(emptyList(), Flt64.zero),
                    comparison = Comparison.GE,
                    name = "legacy_same_as_input"
                )
            ),
            constraint = false,
            epsilon = Flt64(1e-6),
            m = Flt64(10.0),
            converter = flt64TestConverter,
            name = "legacy_same_as"
        )
        val floor = FloorFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_floor"
        )
        val ceiling = CeilingFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_ceiling"
        )
        val rounding = RoundingFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_rounding"
        )
        val binaryzation = BinaryzationFunction(
            polynomial = xPoly,
            converter = flt64TestConverter,
            name = "legacy_binaryzation"
        )

        val cases: List<Pair<MathFunctionSymbol<Flt64>, AbstractVariableItem<*, *>>> = listOf(
            floor to floor.resultVar,
            ceiling to ceiling.resultVar,
            rounding to rounding.resultVar,
            binaryzation to binaryzation.resultVar,
            sameAs to sameAs.resultVar
        )

        for ((function, resultVariable) in cases) {
            val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
            assertEquals(
                listOf(resultVariable),
                adapter.polynomial.monomials.map { it.symbol },
                "${function.name} should expose its actual result variable"
            )
            assertEquals(Flt64.one, adapter.polynomial.monomials.single().coefficient)
            assertEquals(Flt64.zero, adapter.polynomial.constant)
        }
    }

    @Test
    fun `LinearFunctionSymbolAdapter reads legacy result from solver tokens`() {
        val x = RealVar("legacy_solver_result_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val floor = FloorFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_solver_floor"
        )
        val ceiling = CeilingFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_solver_ceiling"
        )
        val rounding = RoundingFunction(
            x = xPoly,
            converter = flt64TestConverter,
            name = "legacy_solver_rounding"
        )
        val binaryzation = BinaryzationFunction(
            polynomial = xPoly,
            converter = flt64TestConverter,
            name = "legacy_solver_binaryzation"
        )
        val sameAs = SameAsFunction(
            inequalities = listOf(
                LinearInequality(
                    lhs = xPoly,
                    rhs = LinearPolynomial(emptyList(), Flt64.zero),
                    comparison = Comparison.GE,
                    name = "legacy_solver_same_as_input"
                )
            ),
            constraint = false,
            epsilon = Flt64(1e-6),
            m = Flt64(10.0),
            converter = flt64TestConverter,
            name = "legacy_solver_same_as"
        )
        val cases: List<Pair<MathFunctionSymbol<Flt64>, AbstractVariableItem<*, *>>> = listOf(
            floor to floor.resultVar,
            ceiling to ceiling.resultVar,
            rounding to rounding.resultVar,
            binaryzation to binaryzation.resultVar,
            sameAs to sameAs.resultVar
        )
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        try {
            for ((index, case) in cases.withIndex()) {
                val (function, resultVariable) = case
                assertTrue(function is HasResultVariable, "${function.name} should implement HasResultVariable")
                assertSame(
                    resultVariable,
                    (function as HasResultVariable).resultVar,
                    "${function.name} should expose its actual result variable through HasResultVariable"
                )
                val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
                assertTrue(adapter.registerAuxiliaryTokens(tokenTable) is Ok)

                val expected = Flt64((index + 1).toDouble())
                tokenTable.setSolverSolution(mapOf(resultVariable to expected))
                assertEquals(
                    expected,
                    adapter.evaluate(tokenTable, flt64TestConverter, zeroIfNone = false),
                    "${function.name} should read the solver value of its result variable"
                )
                tokenTable.clearSolution()
            }
        } finally {
            tokenTable.close()
        }
    }

    @Test
    fun `MathFunctionSymbol two-phase register lifecycle works`() {
        val tokens = fuookami.ospf.kotlin.core.token.AutoTokenTable<Flt64>(Linear, false)

        val testFunc = object : MathFunctionSymbol<Flt64> {
            override var name: String = "independent_test"
            override var displayName: String? = null
            override val helperVariables: List<AbstractVariableItem<*, *>> = emptyList()
            override fun evaluate(values: Map<Symbol, Flt64>): Flt64? = null
            override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<Flt64>): Try = ok
            override fun registerConstraints(model: fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel<Flt64>): Try = ok
        }

        val result = testFunc.registerAuxiliaryTokens(tokens)
        assertTrue(result is Ok, "registerAuxiliaryTokens should succeed")
    }

    @Test
    fun `token register accepts function adapter symbol`() {
        val x = RealVar("register_adapter_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            converter = flt64TestConverter,
            name = "register_adapter_slack"
        )
        val adapter = LinearFunctionSymbolAdapter(slack, flt64TestConverter)
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        tokenTable.add(listOf(x))

        val result = listOf<IntermediateSymbol<*>>(adapter).register(tokenTable)
        assertTrue(result is Ok, "register should support non-expression linear symbols")
    }

    @Test
    fun `token register keeps function adapters out of empty symbol fast path`() {
        val x = RealVar("register_pwl_x")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val function = UnivariateLinearPiecewiseFunction(
            x = xPoly,
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, -Flt64.one),
            converter = flt64TestConverter,
            name = "register_pwl"
        )
        val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)
        tokenTable.add(listOf(x))
        tokenTable.add(adapter)

        val result = listOf<IntermediateSymbol<*>>(adapter).register(tokenTable)

        assertTrue(result is Ok, "register should support function adapters with zero fallback value")
        assertEquals(
            listOf("register_pwl"),
            tokenTable.copy().symbols.map { it.name },
            "mutable token table copy should keep registered function symbols for mechanism lifecycle"
        )
        for (helperVar in function.helperVariables) {
            assertNotNull(
                tokenTable.find(helperVar),
                "helper variable ${helperVar.name} should be registered by function lifecycle"
            )
        }
    }
}
