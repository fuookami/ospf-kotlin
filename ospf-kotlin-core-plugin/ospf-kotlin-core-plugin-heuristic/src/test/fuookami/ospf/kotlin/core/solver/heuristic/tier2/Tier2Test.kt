/**
 * Tier-2 算法最小收敛算例 / Minimal convergence cases for Tier-2 algorithms
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier2

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
import fuookami.ospf.kotlin.core.solver.heuristic.differentialEvolution
import fuookami.ospf.kotlin.core.solver.heuristic.whaleOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.harrisHawkOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.arithmeticOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.teachingLearningOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.salpSwarmOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.gravitationalSearchOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.fireflyOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.cuckooSearchOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.batAlgorithmOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.mothFlameOptimization
import fuookami.ospf.kotlin.core.solver.heuristic.grasshopperOptimization

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

/** 求解器起点对应的目标值 / Objective of the solver's starting point */
private fun startingObjective(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): Double {
    val solution = model.initialSolutions(UInt64.one).firstOrNull() ?: return Double.POSITIVE_INFINITY
    return model.objective(solution)?.toDouble() ?: Double.POSITIVE_INFINITY
}

/**
 * 断言求解器返回可用结果，且不劣于起点。 / Assert the solver returns a usable result no worse than its starting point.
 *
 * @param model 模型 / Model
 * @param solver 求解器 / Solver
 * @param baseline 起点目标值 / Starting objective
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

class Tier2Test {

    @Test
    fun differentialEvolutionShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = differentialEvolution<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(101L)
        )
        assertEquals("de", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun whaleOptimizationShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = whaleOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(103L)
        )
        assertEquals("woa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun harrisHawkShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = harrisHawkOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(107L)
        )
        assertEquals("hho", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun arithmeticOptimizationShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = arithmeticOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(109L)
        )
        assertEquals("aoa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun teachingLearningShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = teachingLearningOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(113L)
        )
        assertEquals("tlbo", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun salpSwarmShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = salpSwarmOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(127L)
        )
        assertEquals("ssa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun gravitationalSearchShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = gravitationalSearchOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(131L)
        )
        assertEquals("gsa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun fireflyShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = fireflyOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(137L)
        )
        assertEquals("fa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun cuckooSearchShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = cuckooSearchOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(139L)
        )
        assertEquals("cs", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun batAlgorithmShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = batAlgorithmOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(149L)
        )
        assertEquals("ba", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun mothFlameShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = mothFlameOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(151L)
        )
        assertEquals("mfo", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }

    @Test
    fun grasshopperShouldConverge(): Unit = runBlocking {
        val model = buildModel()
        val solver = grasshopperOptimization<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(157L)
        )
        assertEquals("goa", solver.name)
        assertUsable(model, solver, startingObjective(model))
    }
}
