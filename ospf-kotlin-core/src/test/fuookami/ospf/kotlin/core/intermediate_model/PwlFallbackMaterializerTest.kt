package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlin.test.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.intermediate.PwlFallbackMaterializer
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackTarget
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackMaterializer
import fuookami.ospf.kotlin.core.model.intermediate.materializeDeferredFunctionFallbacks
import fuookami.ospf.kotlin.core.model.intermediate.generateUnivariateLinearPiecewiseConstraints
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.evaluateWith
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar

class PwlFallbackMaterializerTest {
    @Test
    fun materializerMatchesSharedEagerGeneratorAndPreservesAffineConstant() {
        val inputVariable = RealVar("pwl_materializer_x")
        val input = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), inputVariable)),
            constant = Flt64(3.0)
        )
        val function = UnivariateLinearPiecewiseFunction(
            x = input,
            breakpoints = listOf(Flt64.zero, Flt64(5.0), Flt64(10.0)),
            slopes = listOf(Flt64.one, Flt64(2.0)),
            intercepts = listOf(Flt64.zero, Flt64(-5.0)),
            m = Flt64(20.0),
            converter = IntoValue.Identity,
            name = "pwl_materializer"
        )
        val structure = function.deferredStructure() as UnivariateLinearPiecewiseStructure<Flt64>
        val eager = requireOk(
            generateUnivariateLinearPiecewiseConstraints(
                input = structure.input,
                breakpoints = structure.breakpoints,
                slopes = structure.slopes,
                intercepts = structure.intercepts,
                explicitM = structure.explicitM,
                converter = structure.converter!!,
                resultVariable = structure.resultVariable,
                selectorVariables = structure.selectorVariables,
                name = structure.name
            )
        )
        val fallback = requireOk(PwlFallbackMaterializer.materialize(structure)).constraints

        assertEquals(eager, fallback)
        assertEquals(9, fallback.size)
        assertEquals(
            Flt64.zero,
            fallback.first { it.name == "pwl_materializer_seg_0_lb" }.rhs.constant
        )
        assertEquals(
            Flt64(5.0),
            fallback.first { it.name == "pwl_materializer_seg_1_lb" }.rhs.constant
        )
        assertEquals(
            Flt64(25.0),
            fallback.first { it.name == "pwl_materializer_seg_0_ub" }.rhs.constant
        )
        assertEquals(
            Flt64(30.0),
            fallback.first { it.name == "pwl_materializer_seg_1_ub" }.rhs.constant
        )
        val equationUpper = fallback.first { it.name == "pwl_materializer_seg_0_eq_ub" }
        assertEquals(Flt64(-3.0), equationUpper.lhs.constant)

        val actualValues: Map<Symbol, Flt64> = mapOf(
            inputVariable to Flt64.zero,
            structure.resultVariable to Flt64(3.0),
            structure.selectorVariables[0] to Flt64.one,
            structure.selectorVariables[1] to Flt64.zero
        )
        assertTrue(fallback.all { satisfies(it, actualValues) })

        val legacyWrongValues = actualValues + (structure.resultVariable to Flt64.zero)
        assertTrue(fallback.any { !satisfies(it, legacyWrongValues) })
    }

    @Test
    fun deferredStructureCopiesInputPolynomialAndRetainsMaterializerData() {
        val monomials = mutableListOf(LinearMonomial(Flt64.one, RealVar("pwl_snapshot_x")))
        val input = LinearPolynomial(monomials = monomials, constant = Flt64(4.0))
        val function = UnivariateLinearPiecewiseFunction(
            x = input,
            breakpoints = listOf(Flt64.zero, Flt64.one),
            slopes = listOf(Flt64.one),
            intercepts = listOf(Flt64.zero),
            m = Flt64(8.0),
            converter = IntoValue.Identity,
            name = "pwl_snapshot"
        )

        val structure = function.deferredStructure() as UnivariateLinearPiecewiseStructure<Flt64>
        monomials += LinearMonomial(Flt64.one, RealVar("pwl_snapshot_mutation"))

        assertEquals(1, structure.input.monomials.size)
        assertEquals(Flt64(8.0), structure.explicitM)
        assertNotSame(input.monomials, structure.input.monomials)
        assertEquals("pwl_snapshot", structure.name)
    }

    @Test
    fun automaticBigMUsesInputBoundsCapturedBeforeVariableMutation() {
        val inputVariable = RealVar("pwl_bounds_snapshot_x")
        inputVariable.range.set(testRange(-1.0, 1.0))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64(2.0)),
            slopes = listOf(Flt64.one),
            intercepts = listOf(Flt64.zero),
            converter = IntoValue.Identity,
            name = "pwl_bounds_snapshot"
        )

        val structure = function.deferredStructure() as UnivariateLinearPiecewiseStructure<Flt64>
        val capturedBounds = structure.capturedInputBounds ?: error("automatic Big-M bounds were not captured")
        val captured = requireOk(capturedBounds)
        assertEquals(Flt64(-1.0), captured.lower)
        assertEquals(Flt64.one, captured.upper)

        inputVariable.range.set(testRange(-100.0, 100.0))

        val fallback = requireOk(PwlFallbackMaterializer.materialize(structure)).constraints
        val expected = requireOk(
            generateUnivariateLinearPiecewiseConstraints(
                input = structure.input,
                breakpoints = structure.breakpoints,
                slopes = structure.slopes,
                intercepts = structure.intercepts,
                explicitM = structure.explicitM,
                converter = structure.converter!!,
                resultVariable = structure.resultVariable,
                selectorVariables = structure.selectorVariables,
                name = structure.name,
                inputBounds = capturedBounds
            )
        )

        assertEquals(expected, fallback)
        assertEquals(
            Flt64(3.0),
            fallback.first { it.name == "pwl_bounds_snapshot_seg_0_eq_ub" }.rhs.constant
        )
    }

    @Test
    fun automaticBigMSnapshotFailureIsNotRepairedByLaterFiniteRange() {
        val inputVariable = RealVar("pwl_bounds_failure_snapshot_x")
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, inputVariable)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64.one),
            slopes = listOf(Flt64.one),
            intercepts = listOf(Flt64.zero),
            converter = IntoValue.Identity,
            name = "pwl_bounds_failure_snapshot"
        )

        val structure = function.deferredStructure() as UnivariateLinearPiecewiseStructure<Flt64>
        assertTrue(structure.capturedInputBounds is Failed)

        inputVariable.range.set(testRange(-1.0, 1.0))

        val result = PwlFallbackMaterializer.materialize(structure)
        assertTrue(result is Failed)
    }

    @Test
    fun invalidPiecewiseSnapshotReturnsFailedWithoutMaterializing() {
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(emptyList(), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64(2.0), Flt64.one),
            slopes = listOf(Flt64.one, Flt64.one),
            intercepts = listOf(Flt64.zero, Flt64.zero),
            m = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "pwl_invalid"
        )

        val result = PwlFallbackMaterializer.materialize(function.deferredStructure())

        assertTrue(result is Failed)
        assertEquals(ErrorCode.IllegalArgument, (result as Failed).error.code)
    }

    @Test
    fun materializationFailureRollsBackPreviouslyAppendedRows() {
        val valid = piecewiseStructure("pwl_valid")
        val invalid = UnivariateLinearPiecewiseStructure(
            input = LinearPolynomial(emptyList(), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one),
            slopes = listOf(Flt64.one),
            intercepts = listOf(Flt64.zero),
            resultVariable = RealVar("pwl_invalid_result"),
            selectorVariables = emptyList(),
            explicitM = Flt64.one,
            converter = IntoValue.Identity,
            name = "pwl_invalid_dimensions"
        )
        val target = RecordingTarget()

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(valid, invalid),
            target = target,
            materializer = PwlFallbackMaterializer
        )

        assertTrue(result is Failed)
        assertEquals(0, target.constraintCount)
    }

    @Test
    fun appendFailureIsRolledBackByTransactionCaller() {
        val target = RecordingTarget(failAppend = true)

        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(piecewiseStructure("pwl_append_failure")),
            target = target,
            materializer = PwlFallbackMaterializer
        )

        assertTrue(result is Failed)
        assertEquals(0, target.constraintCount)
    }

    @Test
    fun unsupportedStructureReturnsFailed() {
        val materializer: DeferredFunctionFallbackMaterializer = PwlFallbackMaterializer
        val result = materializer.materialize(object : DeferredFunctionStructure {})

        assertTrue(result is Failed)
    }

    private fun piecewiseStructure(name: String): DeferredFunctionStructure {
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(emptyList(), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64(2.0)),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            m = Flt64(10.0),
            converter = IntoValue.Identity,
            name = name
        )
        return function.deferredStructure()
    }

    private fun testRange(lower: Double, upper: Double): ValueRange<Flt64> {
        return ValueRange(
            lb = Flt64(lower),
            ub = Flt64(upper),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!
    }

    private fun <T> requireOk(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> error(result.error.message)
            else -> error("unexpected fatal result")
        }
    }

    private fun satisfies(
        constraint: LinearInequality<Flt64>,
        values: Map<Symbol, Flt64>
    ): Boolean {
        val lhs = constraint.lhs.evaluateWith(values) ?: return false
        val rhs = constraint.rhs.evaluateWith(values) ?: return false
        return when (constraint.comparison) {
            Comparison.EQ -> lhs.compareTo(rhs) == 0
            Comparison.LE -> lhs.compareTo(rhs) <= 0
            Comparison.GE -> lhs.compareTo(rhs) >= 0
            else -> false
        }
    }

    private class RecordingTarget(
        private val failAppend: Boolean = false
    ) : DeferredFunctionFallbackTarget {
    private val constraints = mutableListOf<LinearInequality<Flt64>>()

        override val constraintCount: Int
            get() = constraints.size

    override fun append(constraints: List<LinearInequality<Flt64>>): Try {
            this.constraints += constraints
            return if (failAppend) {
                Failed(ErrorCode.ApplicationError, "append failed")
            } else {
                ok
            }
        }

        override fun rollback(constraintCount: Int): Try {
            if (constraintCount < 0 || constraintCount > constraints.size) {
                return Failed(ErrorCode.IllegalArgument, "invalid rollback position")
            }
            constraints.subList(constraintCount, constraints.size).clear()
            return ok
        }
    }
}
