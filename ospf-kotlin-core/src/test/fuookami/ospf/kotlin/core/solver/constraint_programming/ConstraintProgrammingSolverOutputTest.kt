package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64

class ConstraintProgrammingSolverOutputTest {
    @Test
    fun solutionShouldProvideStableIdLookupAndTypedOutput() {
        val id = VariableId("amount")
        val solution = ConstraintProgrammingSolution(values = mapOf(id to Int64(9)))
        assertEquals(Int64(9), solution.value(id).value)
        val output = ConstraintProgrammingFeasibleOutput(solution = solution)
        assertEquals(solution, output.solution)
    }

    @Test
    fun typedReportKeepsInt64SolutionSeparateFromFlt64Statistics() {
        val id = VariableId("amount")
        val exact = Int64(9_007_199_254_740_993L)
        val report = fuookami.ospf.kotlin.core.solver.report.SolveReport<Int64>(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.Incumbent,
            solution = fuookami.ospf.kotlin.core.solver.report.SolveSolution(
                values = listOf(exact),
                objective = exact
            ),
            statistics = fuookami.ospf.kotlin.core.solver.report.SolveStatistics(
                bestBound = Flt64(0.5),
                gap = Flt64(0.25)
            )
        )
        assertEquals(exact, report.solution!!.values.single())
        assertEquals(Flt64(0.5), report.statistics.bestBound)
    }
}
