package fuookami.ospf.kotlin.core.symbol.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar

class LegacyResultPolynomialContractTest {
    @Test
    fun functionsWithResultVariablesExposeStableResultPolynomials() {
        val x = RealVar("result_contract_x")
        val xPolynomial = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val functions: List<Pair<MathFunctionSymbol<Flt64>, AbstractVariableItem<*, *>>> = listOf(
            FloorFunction(
                x = xPolynomial,
                converter = flt64TestConverter,
                name = "result_contract_floor"
            ).let { it to it.resultVar },
            CeilingFunction(
                x = xPolynomial,
                converter = flt64TestConverter,
                name = "result_contract_ceiling"
            ).let { it to it.resultVar },
            RoundingFunction(
                x = xPolynomial,
                converter = flt64TestConverter,
                name = "result_contract_rounding"
            ).let { it to it.resultVar },
            BinaryzationFunction(
                polynomial = xPolynomial,
                converter = flt64TestConverter,
                name = "result_contract_binaryzation"
            ).let { it to it.resultVar },
            SameAsFunction(
                inequalities = listOf(
                    LinearInequality(
                        lhs = xPolynomial,
                        rhs = LinearPolynomial(emptyList(), Flt64.zero),
                        comparison = Comparison.GE,
                        name = "result_contract_same_as_input"
                    )
                ),
                constraint = false,
                epsilon = Flt64(1e-6),
                m = Flt64(10.0),
                converter = flt64TestConverter,
                name = "result_contract_same_as"
            ).let { it to it.resultVar }
        )

        for ((function, resultVariable) in functions) {
            assertTrue(function is HasResultPolynomial<*>, "${function.name} should expose HasResultPolynomial")
            assertTrue(function.helperVariables.contains(resultVariable))

            val resultPolynomial = resultPolynomialOf(function)
            assertSame(resultVariable, resultPolynomial.monomials.single().symbol)
            assertEquals(Flt64.one, resultPolynomial.monomials.single().coefficient)
            assertEquals(Flt64.zero, resultPolynomial.constant)
            assertSame(resultPolynomial, resultPolynomialOf(function))

            val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
            assertSame(resultPolynomial, adapter.polynomial)
        }
    }

    @Test
    fun adapterReadsLegacyResultContractsWithoutMarkerInterfaces() {
        val resultVarOnlyVariable = RealVar("legacy_result_var_only")
        val resultPropertyVariable = RealVar("legacy_result_property")
        val resultVarOnly = LegacyResultVarFunction(resultVarOnlyVariable)
        val resultProperty = LegacyResultPropertyFunction(resultPropertyVariable)
        val functions = listOf(
            Triple(resultVarOnly, resultVarOnlyVariable, Flt64(17.0)),
            Triple(resultProperty, resultPropertyVariable, Flt64(23.0))
        )
        val adapters = functions.map { (function, _, _) ->
            assertTrue(function !is HasResultPolynomial<*>)
            assertTrue(function !is HasResultVariable)
            LinearFunctionSymbolAdapter(function, flt64TestConverter)
        }
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        try {
            for (adapter in adapters) {
                assertTrue(adapter.registerAuxiliaryTokens(tokenTable) is Ok)
            }

            val solverResults = tokenTable.tokensInSolver.map { token ->
                functions.single { (_, variable, _) -> variable == token.variable }.third
            }
            for ((index, function) in functions.withIndex()) {
                val (_, resultVariable, expected) = function
                val adapter = adapters[index]
                assertEquals(listOf(resultVariable), adapter.polynomial.monomials.map { it.symbol })
                tokenTable.setSolverSolution(mapOf(resultVariable to expected))
                assertEquals(expected, adapter.evaluate(tokenTable, flt64TestConverter, false))
                tokenTable.clearSolution()
                assertEquals(expected, adapter.evaluate(solverResults, tokenTable, flt64TestConverter, false))
                assertEquals(
                    expected,
                    adapter.evaluate(
                        values = mapOf(resultVariable to expected),
                        tokenTable = tokenTable,
                        converter = flt64TestConverter,
                        zeroIfNone = false
                    )
                )
            }
        } finally {
            tokenTable.close()
        }
    }

    @Test
    fun adapterPrefersLegacyResultPolynomialOverResultVariable() {
        val resultVariable = RealVar("legacy_result_polynomial_precedence")
        val resultPolynomial = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), resultVariable)),
            constant = Flt64(3.0)
        )
        val function = LegacyResultVariableAndPolynomialFunction(resultVariable, resultPolynomial)
        val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        try {
            assertTrue(adapter.registerAuxiliaryTokens(tokenTable) is Ok)
            assertSame(resultPolynomial, adapter.polynomial)

            tokenTable.setSolverSolution(mapOf(resultVariable to Flt64(4.0)))
            assertEquals(
                Flt64(11.0),
                adapter.evaluate(tokenTable, flt64TestConverter, zeroIfNone = false),
                "solver evaluation should use the legacy result polynomial"
            )
        } finally {
            tokenTable.close()
        }
    }

    @Test
    fun adapterUsesResultVariableWhenAResultPolynomialGetterFails() {
        val resultVariable = RealVar("legacy_result_getter_failure")
        val function = ThrowingResultPolynomialFunction(resultVariable)
        val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        try {
            assertTrue(adapter.registerAuxiliaryTokens(tokenTable) is Ok)
            assertEquals(
                listOf(resultVariable),
                adapter.polynomial.monomials.map { it.symbol },
                "a broken result polynomial getter must fall back to the real result variable"
            )

            val expected = Flt64(31.0)
            tokenTable.setSolverSolution(mapOf(resultVariable to expected))
            assertEquals(expected, adapter.evaluate(tokenTable, flt64TestConverter, zeroIfNone = false))
        } finally {
            tokenTable.close()
        }
    }

    @Test
    fun adapterKeepsPurePredicateWithoutResultContractAsZero() {
        val helperVariable = RealVar("predicate_helper_only")
        val function = PredicateWithoutResultFunction(helperVariable)
        val adapter = LinearFunctionSymbolAdapter(function, flt64TestConverter)
        val tokenTable = AutoTokenTable<Flt64>(Linear, false)

        try {
            assertTrue(adapter.registerAuxiliaryTokens(tokenTable) is Ok)
            assertTrue(adapter.polynomial.monomials.isEmpty())
            assertEquals(Flt64.zero, adapter.polynomial.constant)

            val helperValue = Flt64(37.0)
            tokenTable.setSolverSolution(mapOf(helperVariable to helperValue))
            assertEquals(
                Flt64.zero,
                adapter.evaluate(tokenTable, flt64TestConverter, zeroIfNone = false),
                "a predicate without a result contract must not use a helper variable as its result"
            )
        } finally {
            tokenTable.close()
        }
    }

    private class LegacyResultVarFunction(
        val resultVar: AbstractVariableItem<*, *>
    ) : MathFunctionSymbol<Flt64> {
        override var name: String = "legacy_result_var"
        override var displayName: String? = null
        override val helperVariables: List<AbstractVariableItem<*, *>> = listOf(resultVar)

        override fun evaluate(values: Map<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>): Flt64? {
            return values[resultVar]
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helperVariables)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try = ok
    }

    private class LegacyResultPropertyFunction(
        resultVariable: AbstractVariableItem<*, *>
    ) : MathFunctionSymbol<Flt64> {
        override var name: String = "legacy_result_property"
        override var displayName: String? = null
        override val helperVariables: List<AbstractVariableItem<*, *>> = listOf(resultVariable)
        val result: LinearPolynomial<Flt64> = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, resultVariable)),
            constant = Flt64.zero
        )

        override fun evaluate(values: Map<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>): Flt64? {
            return result.evaluateWith(values)
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helperVariables)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try = ok
    }

    private class LegacyResultVariableAndPolynomialFunction(
        override val resultVar: AbstractVariableItem<*, *>,
        val resultPolynomial: LinearPolynomial<Flt64>
    ) : MathFunctionSymbol<Flt64>, HasResultVariable {
        override var name: String = "legacy_result_polynomial_precedence"
        override var displayName: String? = null
        override val helperVariables: List<AbstractVariableItem<*, *>> = listOf(resultVar)

        override fun evaluate(values: Map<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>): Flt64? {
            return Flt64(-99.0)
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helperVariables)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try = ok
    }

    private class ThrowingResultPolynomialFunction(
        override val resultVar: AbstractVariableItem<*, *>
    ) : MathFunctionSymbol<Flt64>, HasResultVariable, HasResultPolynomial<Flt64> {
        override var name: String = "legacy_result_getter_failure"
        override var displayName: String? = null
        override val helperVariables: List<AbstractVariableItem<*, *>> = listOf(resultVar)

        override val resultPolynomial: LinearPolynomial<Flt64>
            get() = throw IllegalStateException("test result polynomial getter failure")

        override fun evaluate(values: Map<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>): Flt64? {
            return Flt64(-999.0)
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helperVariables)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try = ok
    }

    private class PredicateWithoutResultFunction(
        private val helperVariable: AbstractVariableItem<*, *>
    ) : MathFunctionSymbol<Flt64> {
        override var name: String = "predicate_without_result"
        override var displayName: String? = null
        override val helperVariables: List<AbstractVariableItem<*, *>> = listOf(helperVariable)

        override fun evaluate(values: Map<fuookami.ospf.kotlin.math.symbol.Symbol, Flt64>): Flt64? {
            return if ((values[helperVariable] ?: Flt64.zero) gr Flt64.zero) {
                Flt64.one
            } else {
                Flt64.zero
            }
        }

        override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<Flt64>): Try {
            return tokens.add(helperVariables)
        }

        override fun registerConstraints(model: AbstractLinearMechanismModel<Flt64>): Try = ok
    }

    @Suppress("UNCHECKED_CAST")
    private fun resultPolynomialOf(function: MathFunctionSymbol<Flt64>): LinearPolynomial<Flt64> {
        return (function as HasResultPolynomial<Flt64>).resultPolynomial
    }
}
