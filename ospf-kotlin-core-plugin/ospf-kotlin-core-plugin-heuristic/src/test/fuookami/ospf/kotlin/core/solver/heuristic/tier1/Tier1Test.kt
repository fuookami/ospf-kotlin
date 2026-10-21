/**
 * Tier-1 算法最小收敛算例 / Minimal convergence cases for Tier-1 algorithms
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier1

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
import fuookami.ospf.kotlin.core.solver.heuristic.harmonySearch
import fuookami.ospf.kotlin.core.solver.heuristic.waterCycleAlgorithm
import fuookami.ospf.kotlin.core.solver.heuristic.seagullOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.neighborhoodSearch
import fuookami.ospf.kotlin.core.solver.heuristic.hillClimbing
import fuookami.ospf.kotlin.core.solver.heuristic.warStrategyOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.energyValleyOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.germinalCenterOptimization

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class SeededEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun buildModel() = CallBackModel(
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

/**
 * 求解器的起点目标值 / Objective of the solver's starting point
 *
 * 轨迹类算法只从第一个初始解出发，种群类算法的初始种群也包含它，因此统一以「第一个初始解」为基线才可比。 /
 * Trajectory algorithms start from the first initial solution only, and a population solver's initial population also contains
 * it, so "the first initial solution" is the only comparable baseline.
 *
 * @param model 模型 / Model
 * @return 起点目标值 / Starting objective
*/
private fun startingObjective(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): Double {
    val solution = model.initialSolutions(UInt64.one).firstOrNull() ?: return Double.POSITIVE_INFINITY
    return model.objective(solution)?.toDouble() ?: Double.POSITIVE_INFINITY
}

/**
 * 断言求解器返回可用结果，且不劣于初始种群的最优值。 / Assert the solver returns a usable result no worse than the best initial solution.
 *
 * @param model 模型 / Model
 * @param solver 求解器 / Solver
 * @param baseline 初始最优值 / Baseline best objective
*/
private suspend fun assertUsable(
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
            assertNotNull(result.value.bestSolution)
            assertEquals(2, result.value.bestSolution!!.size)
        }

        else -> fail("${solver.name} failed: $result")
    }
}

class Tier1Test {

    @Test
    fun harmonySearchShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = harmonySearch<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(61L)
        )
        assertEquals("hs", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun waterCycleShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = waterCycleAlgorithm<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(67L)
        )
        assertEquals("wca", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun seagullShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = seagullOptimization<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(71L)
        )
        assertEquals("soa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun neighborhoodSearchShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = neighborhoodSearch<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(73L)
        )
        assertEquals("ns", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun hillClimbingShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = hillClimbing<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(79L)
        )
        assertEquals("hill-climbing", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun warStrategyShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = warStrategyOptimization<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(83L)
        )
        assertEquals("warso", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun energyValleyShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = energyValleyOptimization<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(89L)
        )
        assertEquals("evo", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun germinalCenterShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = germinalCenterOptimization<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(97L)
        )
        assertEquals("gco", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }
}
