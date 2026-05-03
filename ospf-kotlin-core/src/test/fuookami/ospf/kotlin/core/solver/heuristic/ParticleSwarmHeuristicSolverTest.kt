package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ParticleSwarmHeuristicSolverTest {
    @Test
    fun psoSolverShouldConsumeCallbackModelInitialSolutions() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")
        val model = CallBackModel(
            objectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator = { (solution, _) ->
                when (solution.toInt()) {
                    0 -> Flt64(5)
                    1 -> Flt64.one
                    else -> Flt64(3)
                }
            }
        )
        assertTrue(model.add(listOf(x, y)) is Ok)
        model.minimize(
            func = { solution -> solution.fold(Flt64.zero) { acc, value -> acc + value } },
            name = "sum",
            displayName = null
        )

        val solver = ParticleSwarmHeuristicSolver(
            particleAmount = UInt64(3),
            solutionAmount = UInt64(2),
            w = Flt64.zero,
            c1 = Flt64.zero,
            c2 = Flt64.zero,
            maxVelocity = Flt64(10)
        ).withSolveOnObjectiveMiss(false)
        val result = solver(
            model = model,
            policy = BasicHeuristicPolicy(iterationLimit = UInt64.zero)
        )

        when (result) {
            is Ok -> {
                assertEquals("pso", solver.name)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
                assertEquals(Flt64(2), result.value.bestObjective)
                assertEquals(listOf(Flt64.one, Flt64.one), result.value.bestSolution)
            }

            else -> {
                fail("expected pso solve to succeed, got $result")
            }
        }
    }
}
