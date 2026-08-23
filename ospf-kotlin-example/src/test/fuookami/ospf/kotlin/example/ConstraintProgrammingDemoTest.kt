package fuookami.ospf.kotlin.example

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.example.constraint_programming_demo.CpDemoContext
import fuookami.ospf.kotlin.example.constraint_programming_demo.DirectConstraintProgrammingDemo
import fuookami.ospf.kotlin.example.constraint_programming_demo.LogicBasedBendersDemo
import fuookami.ospf.kotlin.framework.solver.LogicBasedBendersReport

class ConstraintProgrammingDemoTest {
    @Test
    fun directCoreDemoReachesOptimalProof() = runBlocking {
        val result = DirectConstraintProgrammingDemo()
        val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(result.value)
        assertEquals(SolverStatus.Optimal, feasible.status)
        assertEquals(ProofStatus.Verified, feasible.proofStatus)
    }

    @Test
    fun frameworkContextRegistersAggregationAndPipeline() {
        val model = assertIs<ConstraintProgrammingModel>(CpDemoContext().buildModel().value)
        assertEquals(2, model.variableCount)
        assertEquals(1, model.constraintCount)
    }

    @Test
    fun bendersDemoConvergesWithConflictCut() = runBlocking {
        val result = LogicBasedBendersDemo()
        val report = assertIs<LogicBasedBendersReport>(result.value)
        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(ProofStatus.Verified, report.proof.status)
        assertEquals(1, report.cuts.size)
        assertEquals(2, report.iterations.size)
    }
}
