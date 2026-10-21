/**
 * PSO 变体契约测试 / PSO variant contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.heuristic.HeuristicSolver
import fuookami.ospf.kotlin.core.solver.heuristic.HeuristicSolutionStatus
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.adaptiveParticleSwarm
import fuookami.ospf.kotlin.core.solver.heuristic.comprehensiveLearningParticleSwarm

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class PsoVariantEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun psoVariantModel() = CallBackModel(
    objectCategory = ObjectCategory.Minimum,
    initialSolutionGenerator = { (solution, variable) ->
        Flt64(((solution.toInt() * 3 + variable.toInt() * 2) % 7).toDouble())
    },
    converter = IntoValue.Identity
).also { model ->
    model.add(RealVar("x"))
    model.add(RealVar("y"))
    model.addObject(
        ObjectCategory.Minimum,
        { solution: List<Flt64> ->
            (solution[0] - Flt64.one).pow(2) + (solution[1] - Flt64(5.0)).pow(2)
        }
    )
}

/** 求解器起点对应的目标值 / Objective of the solver's starting point */
private fun psoVariantStartingObjective(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): Double {
    val solution = model.initialSolutions(UInt64.one).firstOrNull() ?: return Double.POSITIVE_INFINITY
    return model.objective(solution)?.toDouble() ?: Double.POSITIVE_INFINITY
}

/**
 * 断言求解器返回可用结果 / Assert the solver returns a usable result
 *
 * @param model 模型 / Model
 * @param solver 求解器 / Solver
 * @param baseline 起点目标值 / Starting objective
*/
private suspend fun assertPsoVariantUsable(
    model: AbstractCallBackModelInterface<*, Flt64, Flt64>,
    solver: HeuristicSolver<Flt64, Flt64>,
    baseline: Double
) {
    when (val result = solver(model)) {
        is Ok -> {
            val best = result.value.bestObjective
            assertNotNull(best, "${solver.name} returned no objective")
            assertTrue(best.toDouble().isFinite(), "${solver.name} returned a non-finite objective")
            assertTrue(
                best.toDouble() <= baseline + 1.0e-9,
                "${solver.name} worsened the objective: ${best.toDouble()} > $baseline"
            )
            assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
            assertEquals(2, result.value.bestSolution!!.size)
        }

        else -> fail("${solver.name} failed: $result")
    }
}

class PsoVariantTest {

    @Test
    fun adaptiveParticleSwarmShouldConverge(): Unit = runBlocking {
        val model = psoVariantModel()
        val solver = adaptiveParticleSwarm<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = PsoVariantEntropy(193L)
        )
        assertEquals("apso", solver.name)
        assertPsoVariantUsable(model, solver, psoVariantStartingObjective(model))
    }

    @Test
    fun comprehensiveLearningParticleSwarmShouldConverge(): Unit = runBlocking {
        val model = psoVariantModel()
        val solver = comprehensiveLearningParticleSwarm<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = PsoVariantEntropy(197L)
        )
        assertEquals("clpso", solver.name)
        assertPsoVariantUsable(model, solver, psoVariantStartingObjective(model))
    }
}
