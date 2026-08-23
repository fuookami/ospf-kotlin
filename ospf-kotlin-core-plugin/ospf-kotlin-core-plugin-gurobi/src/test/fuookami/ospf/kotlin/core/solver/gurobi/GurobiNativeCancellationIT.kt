/** Gurobi 原生取消集成测试。 / Gurobi native cancellation integration tests. */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.Binary

/** Gurobi 原生终止必须转换为结构化 Cancelled 报告。 / Gurobi native termination must become a structured Cancelled report. */
class GurobiNativeCancellationIT {
    @Test
    fun nativeMipCancellationReturnsCancelledReport() = runBlocking {
        val handle = SolveHandle.create()
        val nativeCallback = GurobiLinearSolverCallBack()
        val cancellationCallback: NativeCallBack = {
            handle.cancel(
                source = CancellationSource.Callback,
                reason = "native cancellation integration test"
            )
        }
        nativeCallback.set(cancellationCallback)
        val result = GurobiLinearSolver(
            config = SolverConfig(
                time = 10.seconds,
                threadNum = UInt64.one
            ),
            callBack = nativeCallback
        ).invoke(
            model = cancellationModel(),
            solvingStatusCallBack = null,
            cancellationToken = handle.token
        )
        val report = requireAvailable(result, "Gurobi native cancellation")

        assertEquals(TerminationReason.Cancelled, report.terminationReason)
        assertEquals(CancellationSource.Callback, handle.token.record?.source)
    }

    private fun cancellationModel(): LinearTriadModel {
        val variableCount = 120
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(
                SparseVector<Flt64>().also { row ->
                    repeat(variableCount) { index -> row.add(index, Flt64.one) }
                }
            )
        }
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (0 until variableCount).map { index ->
                    Variable(
                        index = index,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.one,
                        type = Binary,
                        origin = null,
                        name = "cancel-$index"
                    )
                },
                constraints = LinearConstraintBatch(
                    sparseLhs = lhs,
                    signs = listOf(ConstraintRelation.LessEqual),
                    rhs = listOf(Flt64(variableCount.toDouble() / 2.0)),
                    names = listOf("cancel-cardinality"),
                    sources = listOf(ConstraintSource.Origin)
                ),
                name = "gurobi-native-cancellation-it"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Maximum,
                objective = (0 until variableCount).map { index ->
                    LinearObjectiveCell(index, Flt64((index + 1).toDouble()))
                }
            )
        )
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
}
