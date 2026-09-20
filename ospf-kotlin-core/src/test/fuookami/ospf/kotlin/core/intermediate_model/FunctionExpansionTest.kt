package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackConstraints
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionFallbackTarget
import fuookami.ospf.kotlin.core.model.intermediate.materializeDeferredFunctionFallbacks
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.intermediate.FunctionInputShape
import fuookami.ospf.kotlin.core.model.intermediate.FunctionLoweringDecision
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.FunctionUsageLocation
import fuookami.ospf.kotlin.core.model.intermediate.FunctionUsageSummary
import fuookami.ospf.kotlin.core.model.intermediate.PiecewiseContinuity
import fuookami.ospf.kotlin.core.model.intermediate.FunctionSolverCapabilities
import fuookami.ospf.kotlin.core.model.intermediate.decideFunctionLowering
import fuookami.ospf.kotlin.core.model.intermediate.summarizeFunctionUsage
import fuookami.ospf.kotlin.core.model.intermediate.UnivariateLinearPiecewiseStructure
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class FunctionExpansionTest {
    @Test
    fun objectiveOnlyPiecewiseCanUseObjectiveNativeInterface() {
        val usage = FunctionUsageSummary(
            inObjective = true,
            inputShape = FunctionInputShape.SingleVariable,
            continuity = PiecewiseContinuity.Continuous
        )
        val decision = decideFunctionLowering(
            policy = FunctionExpansionPolicy.AUTO,
            usage = usage,
            capabilities = FunctionSolverCapabilities(
                solver = "gurobi",
                version = "10",
                supported = setOf(FunctionNativeCapability.PiecewiseLinearObjective)
            ),
            nativeCapability = FunctionNativeCapability.PiecewiseLinearObjective
        )

        assertEquals(FunctionUsageLocation.ObjectiveOnly, usage.location)
        assertFalse(usage.requiresResultVariable)
        assertTrue(decision.useNative)
    }

    @Test
    fun objectiveAndConstraintRequiresAComposableRelation() {
        val usage = FunctionUsageSummary(
            inObjective = true,
            inConstraint = true,
            inputShape = FunctionInputShape.SingleVariable
        )
        val decision = decideFunctionLowering(
            policy = FunctionExpansionPolicy.AUTO,
            usage = usage,
            capabilities = FunctionSolverCapabilities(
                solver = "gurobi",
                supported = setOf(FunctionNativeCapability.PiecewiseLinearObjective)
            ),
            nativeCapability = FunctionNativeCapability.PiecewiseLinearObjective
        )

        assertEquals(FunctionUsageLocation.ObjectiveAndConstraint, usage.location)
        assertTrue(usage.requiresResultVariable)
        assertFalse(decision.useNative)
    }

    @Test
    fun eagerPolicyAlwaysSelectsFallback() {
        val decision = decideFunctionLowering(
            policy = FunctionExpansionPolicy.EAGER,
            usage = FunctionUsageSummary(inConstraint = true),
            capabilities = FunctionSolverCapabilities(
                solver = "gurobi",
                supported = setOf(FunctionNativeCapability.PiecewiseLinearConstraint)
            ),
            nativeCapability = FunctionNativeCapability.PiecewiseLinearConstraint
        )

        assertEquals(
            FunctionLoweringDecision(false, reason = "eager policy"),
            decision
        )
    }

    @Test
    fun usageSummaryScansObjectiveAndConstraintReferences() {
        val x = RealVar("function_usage_x")
        val result = RealVar("function_usage_result")
        val resultPolynomial = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, result)),
            Flt64.zero
        )
        val constraint = LinearInequality(
            lhs = resultPolynomial,
            rhs = LinearPolynomial(emptyList(), Flt64(3.0)),
            comparison = Comparison.LE,
            name = "function_usage_constraint"
        )
        val summary = summarizeFunctionUsage(
            resultVariable = result,
            objectivePolynomials = listOf(
                LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, result), LinearMonomial(Flt64.one, x)),
                    Flt64.zero
                )
            ),
            constraints = listOf(constraint),
            inputShape = FunctionInputShape.SingleVariable
        )

        assertEquals(FunctionUsageLocation.ObjectiveAndConstraint, summary.location)
        assertTrue(summary.requiresResultVariable)
    }

    @Test
    fun piecewiseFunctionExposesImmutableDeferredStructure() {
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(emptyList(), Flt64.zero),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64(2.0)),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            m = Flt64(10.0),
            converter = IntoValue.Identity,
            name = "deferred_structure"
        )

        val structure = function.deferredStructure()
        assertTrue(structure is UnivariateLinearPiecewiseStructure<*>)
        assertEquals(function.resultVar, structure.resultVariable)
        assertEquals(function.selectorVars, structure.selectorVariables)
        assertEquals(function.breakpoints, structure.breakpoints)
    }

    @Test
    fun deferredFallbackMaterializationRollsBackOnFailure() {
        val structure = object : fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure {}
        val target = object : DeferredFunctionFallbackTarget {
            private var count = 0
            override val constraintCount: Int get() = count
            override fun append(constraints: List<LinearInequality<Flt64>>): Try {
                count += constraints.size
                return ok
            }
            override fun rollback(constraintCount: Int): Try {
                count = constraintCount
                return ok
            }
        }
        val result = materializeDeferredFunctionFallbacks(
            structures = listOf(structure),
            target = target,
            materializer = {
                Failed(ErrorCode.ApplicationError, "unsupported fallback")
            }
        )

        assertTrue(result is Failed)
        assertEquals(0, target.constraintCount)
    }
}
