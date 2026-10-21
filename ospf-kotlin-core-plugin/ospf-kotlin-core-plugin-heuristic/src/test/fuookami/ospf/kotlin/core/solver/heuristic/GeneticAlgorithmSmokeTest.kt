/**
 * 遗传算法端到端冒烟测试 / Genetic algorithm end-to-end smoke test
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.callback.CallBackModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.NoMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.GaussianMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SinglePointCross

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class GaEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

class GeneticAlgorithmSmokeTest {

    @Test
    fun gaShouldRunEndToEndWithTheNewOperators(): Unit = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")
        val model = CallBackModel(
            objectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator = { (solution, variable) ->
                Flt64(((solution.toInt() * 2 + variable.toInt()) % 5).toDouble())
            },
            converter = IntoValue.Identity
        )
        assertTrue(model.add(listOf(x, y)) is Ok)
        model.minimize(
            func = { solution ->
                (solution[0] - Flt64.one).pow(2) + (solution[1] - Flt64(2.0)).pow(2)
            },
            name = "distance",
            displayName = null
        )

        // 两个规模与变异率都不同的岛，等价于旧 GeneAlgorithm 的异构岛屿能力。
        // Two islands with different sizes and mutation rates, equivalent to the old heterogeneous GeneAlgorithm.
        val solver = islandGenetic<Flt64, Flt64>(
            islands = listOf(
                IslandSpec(size = 4, mutationRate = Flt64(0.1)),
                IslandSpec(size = 5, mutationRate = Flt64(0.2))
            ),
            stopping = IterationLimit(UInt64(5)),
            converter = IntoValue.Identity,
            entropy = GaEntropy(13L),
            migrationPeriod = UInt64.zero,
            migration = NoMigration(),
            update = SinglePointCross,
            mutation = GaussianMutation(),
            archiveAmount = 2
        )

        when (val result = solver(model)) {
            is Ok -> {
                val solution = result.value.bestSolution
                assertNotNull(solution, "GA produced no solution")
                assertEquals(2, solution.size, "GA produced a malformed solution")
                val objective = result.value.bestObjective
                assertNotNull(objective, "GA produced no objective")
                assertTrue(objective.toDouble().isFinite(), "GA produced a non-finite objective")
            }

            else -> fail("GA failed: $result")
        }
    }
}
