/**
 * 搜索骨架契约测试 / Search skeleton contract tests
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
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SinglePointCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.GaussianMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.HistoryBestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance.BetterOnlyAcceptance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

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

/** 构造初始个体 / Build a seed individual */
private fun seedIndividual(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): SolutionWithFitness<Flt64, Flt64> {
    val solution = model.initialSolutions(UInt64.one).first()
    return SolutionWithFitness(
        solution = solution,
        fitness = model.objective(solution) ?: model.defaultObjective
    )
}

class RunnerTest {

    @Test
    fun populationRunnerShouldNotWorsenTheArchiveBest() = runBlocking {
        val model = buildModel()

        val zeroRunner = PopulationRunner<Flt64, Flt64>(
            size = 8,
            initializer = ModelInitializer,
            update = SinglePointCross,
            mutation = GaussianMutation(Flt64(0.3)),
            survivor = ElitistSurvivor,
            mutationRate = Flt64(0.5),
            entropy = SeededEntropy(),
            archive = BestArchive(4),
            stopping = IterationLimit(UInt64.zero),
            converter = IntoValue.Identity
        )
        val initialBest = zeroRunner
            .run(model = model).also { assertTrue(it is Ok) }.value!!
            .first()
            .fitness
            .toDouble()

        val runner = PopulationRunner<Flt64, Flt64>(
            size = 8,
            initializer = ModelInitializer,
            update = SinglePointCross,
            mutation = GaussianMutation(Flt64(0.3)),
            survivor = ElitistSurvivor,
            mutationRate = Flt64(0.5),
            entropy = SeededEntropy(),
            archive = BestArchive(4),
            stopping = IterationLimit(UInt64(20)),
            converter = IntoValue.Identity
        )
        val individuals = runner.run(model = model).also { assertTrue(it is Ok) }.value!!

        assertTrue(individuals.isNotEmpty(), "population runner produced no individual")
        assertTrue(individuals.all { it.solution.size == 2 }, "population runner produced malformed solutions")
        assertTrue(individuals.all { it.fitness.toDouble().isFinite() }, "population runner produced non-finite fitness")
        assertTrue(
            individuals.first().fitness.toDouble() <= initialBest + 1.0e-9,
            "population runner worsened the best objective: ${individuals.first().fitness} > $initialBest"
        )
    }

    @Test
    fun trajectoryRunnerShouldNeverWorsenTheCurrentSolution() = runBlocking {
        val model = buildModel()
        val seed = seedIndividual(model)

        val runner = TrajectoryRunner<Flt64, Flt64>(
            initializer = ModelInitializer,
            neighborhood = RandomPerturbation(1),
            acceptance = BetterOnlyAcceptance,
            entropy = SeededEntropy(),
            archive = HistoryBestArchive(),
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity
        )
        val individuals = runner.run(model = model).also { assertTrue(it is Ok) }.value!!

        assertTrue(individuals.isNotEmpty(), "trajectory runner produced no individual")
        assertTrue(
            individuals.first().fitness.toDouble() <= seed.fitness.toDouble() + 1.0e-9,
            "trajectory runner moved to a worse solution"
        )
    }

    @Test
    fun runnerSolverShouldWrapTheRunnerResult() = runBlocking {
        val model = buildModel()
        val runner = PopulationRunner<Flt64, Flt64>(
            size = 6,
            initializer = ModelInitializer,
            update = SinglePointCross,
            mutation = GaussianMutation(Flt64(0.3)),
            survivor = ElitistSurvivor,
            mutationRate = Flt64(0.5),
            entropy = SeededEntropy(),
            archive = BestArchive(2),
            stopping = IterationLimit(UInt64(5)),
            converter = IntoValue.Identity
        )
        val solver = RunnerSolver<Flt64, Flt64>(name = "generic-ga", runner = runner)

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("generic-ga", solver.name)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
                assertNotNull(result.value.bestSolution)
                assertNotNull(result.value.bestObjective)
                assertEquals(2, result.value.bestSolution!!.size)
            }

            else -> fail("runner solver failed: $result")
        }
    }

    @Test
    fun composedSolversShouldBeAssembledFromOperators(): Unit = runBlocking {
        val model = buildModel()

        val genetic = ComposedSolvers.genetic<Flt64, Flt64>(
            size = 6,
            stopping = IterationLimit(UInt64(10)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy()
        )
        when (val result = genetic(model)) {
            is Ok -> {
                assertEquals("genetic", genetic.name)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
                assertNotNull(result.value.bestObjective)
            }

            else -> fail("composed genetic solver failed: $result")
        }

        val climbing = ComposedSolvers.hillClimbing<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(20)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(31L)
        )
        when (val result = climbing(model)) {
            is Ok -> {
                assertEquals("hill-climbing", climbing.name)
                assertNotNull(result.value.bestObjective)
            }

            else -> fail("composed hill-climbing solver failed: $result")
        }

        val walk = ComposedSolvers.randomWalk<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(20)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(37L)
        )
        when (val result = walk(model)) {
            is Ok -> {
                assertEquals("random-walk", walk.name)
                assertNotNull(result.value.bestObjective)
            }

            else -> fail("composed random-walk solver failed: $result")
        }
    }

    @Test
    fun greyWolfShouldBeComposedFromAPositionUpdate(): Unit = runBlocking {
        val model = buildModel()
        val solver = ComposedSolvers.greyWolf<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(41L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("gwo", solver.name)
                assertNotNull(result.value.bestObjective)
                assertTrue(result.value.bestObjective!!.toDouble().isFinite())
            }

            else -> fail("composed GWO failed: $result")
        }
    }

    @Test
    fun simulatedAnnealingShouldBeComposedFromTrajectoryAndMetropolis(): Unit = runBlocking {
        val model = buildModel()
        val seed = seedIndividual(model)
        val solver = ComposedSolvers.simulatedAnnealing<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(43L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("simulated-annealing", solver.name)
                assertNotNull(result.value.bestObjective)
                assertTrue(
                    result.value.bestObjective!!.toDouble() <= seed.fitness.toDouble() + 1.0e-9,
                    "simulated annealing returned a worse objective than the seed"
                )
            }

            else -> fail("composed simulated annealing failed: $result")
        }
    }

    @Test
    fun sineCosineShouldBeComposedFromAPositionUpdate(): Unit = runBlocking {
        val model = buildModel()
        val solver = ComposedSolvers.sineCosine<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(47L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("sca", solver.name)
                assertNotNull(result.value.bestObjective)
                assertTrue(result.value.bestObjective!!.toDouble().isFinite())
            }

            else -> fail("composed SCA failed: $result")
        }
    }

    @Test
    fun multiVerseShouldBeComposedFromAPositionUpdate(): Unit = runBlocking {
        val model = buildModel()
        val solver = ComposedSolvers.multiVerse<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(53L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("mvo", solver.name)
                assertNotNull(result.value.bestObjective)
                assertTrue(result.value.bestObjective!!.toDouble().isFinite())
            }

            else -> fail("composed MVO failed: $result")
        }
    }

    @Test
    fun particleSwarmShouldCarryVelocityAcrossIterations(): Unit = runBlocking {
        val model = buildModel()
        val solver = ComposedSolvers.particleSwarm<Flt64, Flt64>(
            size = 12,
            stopping = IterationLimit(UInt64(40)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(59L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("pso", solver.name)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
                assertNotNull(result.value.bestObjective)
                assertTrue(result.value.bestObjective!!.toDouble().isFinite())
                assertEquals(2, result.value.bestSolution!!.size)
            }

            else -> fail("composed PSO failed: $result")
        }
    }
}
