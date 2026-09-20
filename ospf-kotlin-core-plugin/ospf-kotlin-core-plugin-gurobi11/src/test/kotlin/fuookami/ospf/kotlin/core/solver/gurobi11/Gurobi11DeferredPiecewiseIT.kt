package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import com.gurobi.gurobi.GRB
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/** Gurobi 11 连续一元 PWL 原生单解最小 IT。 / Minimal Gurobi 11 single-solution IT for continuous univariate native PWL. */
class Gurobi11DeferredPiecewiseIT {
    @Test
    fun nativePiecewiseSingleSolveKeepsShapeAndResult() = runBlocking {
        val input = RealVar("gurobi11_it_input")
        assertTrue(input.range.geq(Flt64.zero))
        assertTrue(input.range.leq(Flt64.two))
        val function = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, input)),
                constant = Flt64.zero
            ),
            breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
            slopes = listOf(Flt64.one, Flt64.two),
            intercepts = listOf(Flt64.zero, Flt64(-1.0)),
            converter = IntoValue.Identity,
            name = "gurobi11-it-pwl"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-deferred-pwl-it",
            configuration = MetaModelConfiguration(
                functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
            ),
            converter = IntoValue.Identity
        )
        assertTrue(metaModel.add(input) is Ok)
        assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
        val pointConstraint = LinearInequality(
            lhs = function.x,
            rhs = LinearPolynomial(emptyList(), Flt64(1.5)),
            comparison = Comparison.EQ,
            name = "gurobi11-it-input"
        )
        assertTrue(metaModel.addConstraint(relation = pointConstraint, name = pointConstraint.name) is Ok)
        assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)

        val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> fail(result.error.message ?: "failed to build Gurobi 11 IT model")
            is Fatal -> fail(result.errors.joinToString { it.message ?: "" })
        }
        try {
            val shapes = ArrayList<NativeModelShape>()
            val report = requireAvailable(
                GurobiLinearSolver(
                    config = SolverConfig(
                        threadNum = UInt64.one,
                        dumpIntermediateModelBounds = false,
                        functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST
                    ),
                    callBack = afterModelingCallback(shapes)
                ).solve(mechanism, IntoValue.Identity),
                "Gurobi 11 native PWL single solve"
            )

            assertEquals(1, shapes.size)
            assertEquals(2, shapes.single().numVars)
            assertEquals(1, shapes.single().numConstrs)
            assertEquals(1, shapes.single().numGenConstrs)
            assertEquals(Flt64(1.5), valueAt(report, mechanism, input))
            assertEquals(Flt64(2.0), valueAt(report, mechanism, function.resultVar))
        } finally {
            mechanism.close()
            metaModel.close()
        }
    }

    private fun afterModelingCallback(shapes: MutableList<NativeModelShape>): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack().afterModeling { _, model, _, _ ->
            model.update()
            shapes += NativeModelShape(
                numVars = model.get(GRB.IntAttr.NumVars),
                numConstrs = model.get(GRB.IntAttr.NumConstrs),
                numGenConstrs = model.get(GRB.IntAttr.NumGenConstrs)
            )
            ok
        }
    }

    private fun valueAt(
        report: SolveReport<Flt64>,
        model: LinearMechanismModel<Flt64>,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        val index = model.tokens.tokensInSolver.indexOfFirst { it.key == variable.key }
        assertTrue(index >= 0, "Missing ${variable.name} in solver token order")
        return report.values[index]
    }

    private fun <T> requireAvailable(result: Ret<T>, label: String): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> {
                if (result.error.code in unavailableCodes) {
                    println("$label skipped (${result.error.code}): ${result.error.message}")
                    assumeTrue(false, "$label skipped: ${result.error.message}")
                }
                error("$label failed: ${result.error.code}: ${result.error.message}")
            }
            is Fatal -> {
                if (result.errors.isNotEmpty() && result.errors.all { it.code in unavailableCodes }) {
                    println("$label skipped: ${result.errors.joinToString { "${it.code}: ${it.message}" }}")
                    assumeTrue(false, "$label skipped: ${result.errors.joinToString { it.message ?: "" }}")
                }
                error("$label failed fatally: ${result.errors}")
            }
        }
    }

    private companion object {
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }

    private data class NativeModelShape(
        val numVars: Int,
        val numConstrs: Int,
        val numGenConstrs: Int
    )
}
