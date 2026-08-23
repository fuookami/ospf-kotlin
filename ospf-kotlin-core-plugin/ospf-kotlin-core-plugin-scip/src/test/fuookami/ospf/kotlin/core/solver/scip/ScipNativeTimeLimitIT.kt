package fuookami.ospf.kotlin.core.solver.scip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.config.SCIPSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.BackendParameterValue
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.variable.Binary

/** SCIP 原生时间限制集成测试。 / SCIP native time-limit integration tests. */
class ScipNativeTimeLimitIT {
    @Test
    fun timeLimitWithInitialIncumbentPreservesStructuredSolution() = runBlocking {
        val report = requireAvailable(
            ScipLinearSolver(config = timeLimitConfig()).solveReport(timeLimitModel(withIncumbent = true)),
            "SCIP time limit with incumbent"
        )

        assertEquals(TerminationReason.TimeLimit, report.terminationReason)
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(SolutionPresence.Incumbent, report.solutionPresence)
        val solution = assertNotNull(report.solution)
        assertEquals(Flt64.zero, solution.objective)
        assertEquals(variableCount, solution.values.size)
        assertNotNull(report.statistics.bestBound)
        Unit
    }

    @Test
    fun timeLimitWithoutIncumbentDoesNotInventSolution() = runBlocking {
        val report = requireAvailable(
            ScipLinearSolver(config = timeLimitConfig()).solveReport(timeLimitModel(withIncumbent = false)),
            "SCIP time limit without incumbent"
        )

        assertEquals(TerminationReason.TimeLimit, report.terminationReason)
        assertEquals(ProblemStatus.Unknown, report.problemStatus)
        assertEquals(SolutionPresence.None, report.solutionPresence)
        assertNull(report.solution)
    }

    private fun timeLimitConfig(): SolverConfig {
        return SolverConfig(
            time = Duration.ZERO,
            threadNum = UInt64.one,
            backendConfiguration = SCIPSolverConfig(
                presolve = false,
                randomSeed = 1L,
                deterministic = true,
                nativeParameters = mapOf(
                    "display/verblevel" to BackendParameterValue.Integer(0)
                )
            )
        )
    }

    private fun timeLimitModel(withIncumbent: Boolean): LinearTriadModel {
        val lhs = SparseMatrix<Flt64>().also { matrix ->
            matrix.addRow(
                SparseVector<Flt64>().also { row ->
                    repeat(variableCount) { index -> row.add(index, Flt64.one) }
                }
            )
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
                    signs = listOf(ConstraintRelation.LessEqual),
                    rhs = listOf(Flt64(variableCount.toDouble() / 2.0)),
                    names = listOf("time-limit-cardinality"),
                    sources = listOf(ConstraintSource.Origin)
                ),
                name = "scip-native-time-limit-it"
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
        const val variableCount = 120
        val unavailableCodes = setOf(
            ErrorCode.SolverNotFound,
            ErrorCode.OREngineEnvironmentLost,
            ErrorCode.OREngineConnectionOvertime
        )
    }
}
