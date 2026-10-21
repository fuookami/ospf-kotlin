/**
 * 岛模型骨架与迁移算子契约测试 / Island-model skeleton and migration operator contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.heuristic.HeuristicSolutionStatus
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.BestToWorstMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.NoMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.RingMigration
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class IslandEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun islandModel() = CallBackModel(
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
 * 构造一个只带目标值的个体 / Build an individual carrying only an objective value
 *
 * @param fitness 目标值 / Objective value
 * @return 个体 / Individual
*/
private fun individual(fitness: Double) = SolutionWithFitness(
    solution = listOf(Flt64(fitness)),
    fitness = Flt64(fitness)
)

class IslandMigrationTest {

    @Test
    fun noMigrationShouldLeaveIslandsUntouched() {
        val model = islandModel()
        val islands = listOf(
            listOf(individual(1.0), individual(2.0)),
            listOf(individual(3.0), individual(4.0))
        )
        val result = NoMigration<Flt64, Flt64>()(
            iteration = Iteration(),
            islands = islands,
            model = model,
            entropy = IslandEntropy(229L)
        )
        assertEquals(islands, result)
    }

    @Test
    fun ringMigrationShouldMoveTheBestToTheNeighbour() {
        val model = islandModel()
        // 岛 0 明显更好，环形迁移后岛 1 应当收到岛 0 的最优个体。
        // Island 0 is clearly better, so after ring migration island 1 should receive island 0's best.
        val islands = listOf(
            listOf(individual(1.0), individual(2.0), individual(3.0)),
            listOf(individual(7.0), individual(8.0), individual(9.0))
        )
        val result = RingMigration<Flt64, Flt64>(rate = 0.34)(
            iteration = Iteration(),
            islands = islands,
            model = model,
            entropy = IslandEntropy(233L)
        )

        assertEquals(2, result.size)
        assertEquals(3, result[0].size, "island size must be preserved")
        assertEquals(3, result[1].size, "island size must be preserved")
        assertTrue(
            result[1].any { it.fitness.toDouble() == 1.0 },
            "island 1 did not receive island 0's best individual"
        )
        // 最差的 9.0 被 1.0 取代后，岛 1 的最大值应当回落到 8.0。
        // Once the worst 9.0 is replaced by 1.0, the maximum of island 1 must drop to 8.0.
        assertEquals(
            8.0,
            result[1].maxOf { it.fitness.toDouble() },
            1.0e-9,
            "the worst individual of island 1 should have been replaced"
        )
        assertFalse(
            result[1].any { it.fitness.toDouble() == 9.0 },
            "the worst individual 9.0 should have been removed from island 1"
        )
    }

    @Test
    fun bestToWorstMigrationShouldInjectIntoThePoorIsland() {
        val model = islandModel()
        val islands = listOf(
            listOf(individual(1.0), individual(2.0)),
            listOf(individual(5.0), individual(6.0)),
            listOf(individual(9.0), individual(10.0))
        )
        val result = BestToWorstMigration<Flt64, Flt64>(amount = 1)(
            iteration = Iteration(),
            islands = islands,
            model = model,
            entropy = IslandEntropy(239L)
        )

        assertEquals(3, result.size)
        assertTrue(
            result[2].any { it.fitness.toDouble() == 1.0 },
            "the worst island did not receive the best individual"
        )
        assertEquals(2, result[2].size, "island size must be preserved")
        assertEquals(islands[0], result[0], "the source island must not change")
    }

    @Test
    fun singleIslandShouldBeLeftAlone(): Unit = runBlocking {
        val model = islandModel()
        val islands = listOf(listOf(individual(1.0), individual(2.0)))
        assertEquals(
            islands,
            RingMigration<Flt64, Flt64>()(Iteration(), islands, model, IslandEntropy(241L))
        )
        assertEquals(
            islands,
            BestToWorstMigration<Flt64, Flt64>()(Iteration(), islands, model, IslandEntropy(241L))
        )
    }
}

class IslandRunnerTest {

    @Test
    fun islandGeneticShouldConverge(): Unit = runBlocking {
        val model = islandModel()
        val solver = ComposedSolvers.islandGenetic<Flt64, Flt64>(
            islandCount = 3,
            islandSize = 6,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = IslandEntropy(251L),
            migrationPeriod = UInt64(4),
            migration = RingMigration(rate = 0.2),
            archiveAmount = 1
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("island-ga", solver.name)
                val best = result.value.bestObjective
                assertNotNull(best)
                assertTrue(best.toDouble().isFinite(), "island GA returned a non-finite objective")
                assertEquals(2, result.value.bestSolution!!.size)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
            }

            else -> fail("island GA failed: $result")
        }
    }

    @Test
    fun islandGeneticShouldWorkWithoutMigration(): Unit = runBlocking {
        val model = islandModel()
        val solver = ComposedSolvers.islandGenetic<Flt64, Flt64>(
            islandCount = 2,
            islandSize = 5,
            stopping = IterationLimit(UInt64(20)),
            converter = IntoValue.Identity,
            entropy = IslandEntropy(257L),
            migrationPeriod = UInt64.zero,
            migration = NoMigration(),
            archiveAmount = 1
        )

        when (val result = solver(model)) {
            is Ok -> assertTrue(result.value.bestObjective!!.toDouble().isFinite())
            else -> fail("island GA (no migration) failed: $result")
        }
    }
}
