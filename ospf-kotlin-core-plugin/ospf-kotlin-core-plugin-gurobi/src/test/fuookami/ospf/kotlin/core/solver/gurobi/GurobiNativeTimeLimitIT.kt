package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
import gurobi.GRB

/** Gurobi 原生时间限制集成测试。 / Gurobi native time-limit integration tests. */
class GurobiNativeTimeLimitIT {
    @Test
    fun timeLimitWithInitialIncumbentPreservesStructuredSolution() = runBlocking {
        val report = requireAvailable(
            GurobiLinearSolver(
                config = timeLimitConfig(2.seconds),
                callBack = timeLimitCallback()
            ).solveReport(timeLimitModel(withIncumbent = true)),
            "Gurobi time limit with incumbent"
        )

        assertEquals(TerminationReason.TimeLimit, report.terminationReason)
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(SolutionPresence.Incumbent, report.solutionPresence)
        val solution = assertNotNull(report.solution)
        assertNotNull(solution.objective)
        assertEquals(variableCount, solution.values.size)
        assertNotNull(report.statistics.bestBound)
        Unit
    }

    @Test
    fun timeLimitWithoutIncumbentDoesNotInventSolution() = runBlocking {
        val report = requireAvailable(
            GurobiLinearSolver(
                config = timeLimitConfig(1.milliseconds),
                callBack = timeLimitCallback()
            ).solveReport(timeLimitModel(withIncumbent = false)),
            "Gurobi time limit without incumbent"
        )

        assertEquals(TerminationReason.TimeLimit, report.terminationReason)
        assertEquals(ProblemStatus.Unknown, report.problemStatus)
        assertEquals(SolutionPresence.None, report.solutionPresence)
        assertNull(report.solution)
    }

    private fun timeLimitCallback(): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack().configuration { _, model, _, _ ->
            model.set(GRB.IntParam.Presolve, 0)
            model.set(GRB.DoubleParam.Heuristics, 0.0)
            model.set(GRB.IntParam.Cuts, 0)
            ok
        }
    }

    private fun timeLimitConfig(time: Duration): SolverConfig {
        return SolverConfig(
            time = time,
            threadNum = UInt64.one
        )
    }

    private fun timeLimitModel(withIncumbent: Boolean): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            repeat(constraintCount) { constraintIndex ->
                matrix.addRow(
                    SparseVector<Flt64>().also { row ->
                        for (index in 0 until variableCount) {
                            if ((index * 37 + constraintIndex * 101 + index * constraintIndex * 13) % 1009 < 60) {
                                row.add(index, Flt64.one)
                            }
                        }
                    }
                )
            }
        }
        val variables = (0 until variableCount).map { index ->
            Variable(
                index = index,
                lowerBound = Flt64.zero,
                upperBound = Flt64.one,
                type = Binary,
                origin = null,
                name = "time-limit-$index",
                initialResult = if (withIncumbent) Flt64.zero else null
            )
        }
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = variables,
                constraints = LinearConstraintBatch(
                    sparseLhs = lhs,
                    signs = List(constraintCount) { ConstraintRelation.LessEqual },
                    rhs = List(constraintCount) { Flt64(18.0) },
                    names = (0 until constraintCount).map { "time-limit-packing-$it" },
                    sources = List(constraintCount) { ConstraintSource.Origin }
                ),
                name = "gurobi-native-time-limit-it"
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
        const val variableCount = 1000
        const val constraintCount = 120
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }
}
