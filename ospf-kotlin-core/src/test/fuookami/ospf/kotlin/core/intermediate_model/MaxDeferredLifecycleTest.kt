package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.MaxFallbackMaterializer
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.model.intermediate.generateMaxConstraints
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearPolynomialBounds
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.symbol.function.MinMaxFunction
import fuookami.ospf.kotlin.core.symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar

class MaxDeferredLifecycleTest {
    @Test
    fun affineMaxUsesTheSameExactRowsForEagerAndDeferredPaths() {
        val x = RealVar("max_lifecycle_x")
        val y = RealVar("max_lifecycle_y")
        x.range.geq(Flt64(-2.0))
        x.range.leq(Flt64(3.0))
        y.range.geq(Flt64.zero)
        y.range.leq(Flt64(4.0))
        val inputs = listOf(
            LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, x)),
                constant = Flt64.one
            ),
            LinearPolynomial(
                monomials = listOf(LinearMonomial(-Flt64.two, y)),
                constant = Flt64(3.0)
            ),
            LinearPolynomial(emptyList(), Flt64.zero)
        )
        val function = MaxFunction(
            polynomials = inputs,
            bigM = Flt64(20.0),
            converter = IntoValue.Identity,
            name = "max_lifecycle"
        )
        val structure = assertIs<MaxStructure<Flt64>>(function.deferredStructure())
        val eager = requireOk(
            generateMaxConstraints(
                inputs = structure.inputs,
                resultVariable = structure.resultVariable,
                selectorVariables = structure.selectorVariables,
                bigMValues = structure.bigMValues,
                converter = structure.converter,
                name = structure.name
            )
        )
        val deferred = requireOk(MaxFallbackMaterializer.materialize(structure)).constraints

        assertEquals(7, eager.size)
        assertEquals(eager, deferred)
        assertEquals(
            listOf(
                Comparison.GE,
                Comparison.GE,
                Comparison.GE,
                Comparison.LE,
                Comparison.LE,
                Comparison.LE,
                Comparison.EQ
            ),
            eager.map { it.comparison }
        )
    }

    @Test
    fun automaticProofRejectsExpandedInputButExplicitBigMKeepsFallbackRows() {
        val inputVariable = RealVar("max_proof_x")
        inputVariable.range.geq(Flt64(-3.0))
        inputVariable.range.leq(Flt64(3.0))
        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
            constant = Flt64.zero
        )
        val result = RealVar("max_proof_result")
        val selectors = listOf(BinVar("max_proof_sel0"), BinVar("max_proof_sel1"))
        val captured = MaxStructure(
            inputs = listOf(input, LinearPolynomial(emptyList(), Flt64.zero)),
            resultVariable = result,
            selectorVariables = selectors,
            bigMValues = listOf(Flt64(2.0), Flt64(2.0)),
            converter = IntoValue.Identity,
            name = "max_proof",
            capturedInputBounds = listOf(
                LinearPolynomialBounds(Flt64(-1.0), Flt64.one),
                LinearPolynomialBounds(Flt64.zero, Flt64.zero)
            )
        )
        assertTrue(MaxFallbackMaterializer.materialize(captured) is Failed)

        val explicit = captured.copy(capturedInputBounds = null)
        assertTrue(MaxFallbackMaterializer.materialize(explicit) is Ok)
    }

    @Test
    fun automaticMaxRejectsRangeExpansionAfterSnapshot() {
        val inputVariable = RealVar("max_lifecycle_expanding_x")
        inputVariable.range.set(closedRange(-1.0, 1.0))
        val function = MaxFunction(
            polynomials = listOf(
                LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
                    constant = Flt64.zero
                ),
                LinearPolynomial(emptyList(), Flt64.zero)
            ),
            converter = IntoValue.Identity,
            name = "max_lifecycle_expanding"
        )
        val snapshot = assertIs<MaxStructure<Flt64>>(function.deferredStructure())
        assertTrue(snapshot.capturedInputBounds != null)

        inputVariable.range.set(closedRange(-2.0, 2.0))

        assertTrue(MaxFallbackMaterializer.materialize(snapshot) is Failed)
    }

    @Test
    fun wrappersForwardTheMaxStructure() {
        val input = LinearPolynomial<Flt64>(emptyList(), Flt64(2.0))
        val minMax = MinMaxFunction(
            polynomials = listOf(input, LinearPolynomial(emptyList(), Flt64.one)),
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "wrapped_minmax"
        )
        val slackRange = SlackRangeFunction(
            input = input,
            lower = Flt64.zero,
            upper = Flt64.one,
            bigM = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "wrapped_slack_range"
        )

        assertIs<MaxStructure<*>>(minMax.deferredStructure())
        assertIs<MaxStructure<*>>(slackRange.deferredStructure())
    }

    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
    }

    private fun closedRange(lower: Double, upper: Double): ValueRange<Flt64> {
        return ValueRange(
            lb = Flt64(lower),
            ub = Flt64(upper),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!
    }
}
