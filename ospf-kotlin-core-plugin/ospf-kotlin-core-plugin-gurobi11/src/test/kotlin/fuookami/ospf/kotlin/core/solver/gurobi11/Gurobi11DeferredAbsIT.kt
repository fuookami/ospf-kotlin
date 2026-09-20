package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionExpansionPolicy
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar

/** Gurobi 11 负值 ABS 原生集成测试；无 license 时明确跳过。 / Gurobi 11 negative ABS integration test; explicitly skipped without a license. */
class Gurobi11DeferredAbsIT {
    @Test
    fun nativeAbsPreservesNegativeValue() = runBlocking {
        val input = RealVar("gurobi11_abs_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(2.0))
        val function = AbsFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(2.0),
            name = "gurobi11_abs"
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "gurobi11-abs-it",
            configuration = MetaModelConfiguration(functionExpansionPolicy = FunctionExpansionPolicy.DEFERRED_NATIVE_FIRST),
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.add(input) is Ok)
            assertTrue(metaModel.add(LinearFunctionSymbolAdapter(function, IntoValue.Identity)) is Ok)
            assertTrue(metaModel.addConstraint(
                relation = LinearInequality(
                    lhs = function.polynomial,
                    rhs = LinearPolynomial(emptyList(), Flt64(-1.5)),
                    comparison = Comparison.EQ,
                    name = "gurobi11_abs_point"
                ),
                name = "gurobi11_abs_point"
            ) is Ok)
            assertTrue(metaModel.minimize(function.resultPolynomial) is Ok)
            val mechanism = when (val result = LinearMechanismModel.invoke<Flt64>(
                metaModel = metaModel,
                concurrent = false,
                blocking = true
            )) {
                is Ok -> result.value
                is Failed -> fail(result.error.message ?: "failed to build Gurobi 11 ABS model")
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
                    "Gurobi 11 native ABS negative value"
                )

                assertEquals(1, shapes.single().numGenConstrs)
                assertEquals(Flt64(1.5), valueAt(report, mechanism, function.resultVar))
            } finally {
                mechanism.close()
            }
        } finally {
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
                    assumeTrue(false, "$label skipped: ${result.error.message}")
                }
                error("$label failed: ${result.error.code}: ${result.error.message}")
            }
            is Fatal -> {
                if (result.errors.all { it.code in unavailableCodes }) {
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
