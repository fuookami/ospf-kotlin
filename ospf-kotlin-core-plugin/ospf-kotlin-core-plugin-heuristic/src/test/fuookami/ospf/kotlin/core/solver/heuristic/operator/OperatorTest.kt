/**
 * 算子契约测试 / Operator contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator

import kotlin.test.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.distribution.Uniform
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.core.solver.heuristic.operator.ranking.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.boundary.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.diversity.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stochastic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.*

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

/** 构建两个实变量、最小化 (x-1)^2 + (y-2)^2 的测试模型 / Build a test model with two real variables minimizing (x-1)^2 + (y-2)^2 */
private fun buildModel() = CallBackModel(
    objectCategory = ObjectCategory.Minimum,
    initialSolutionGenerator = { (solution, _) ->
        when (solution.toInt()) {
            0 -> Flt64.one
            1 -> Flt64(2.0)
            else -> Flt64(3.0)
        }
    },
    converter = IntoValue.Identity
).also { model ->
    val x = RealVar("x")
    val y = RealVar("y")
    model.add(x)
    model.add(y)
    model.addObject(
        ObjectCategory.Minimum,
        { solution: List<Flt64> ->
            (solution[0] - Flt64.one).pow(2) + (solution[1] - Flt64(2.0)).pow(2)
        }
    )
}

/** 构造指定目标值的个体 / Build an individual with the given objective value */
private fun individual(value: Double): SolutionWithFitness<Flt64, Flt64> {
    return SolutionWithFitness(
        solution = listOf(Flt64(value), Flt64(value)),
        fitness = Flt64(value)
    )
}

class OperatorTest {

    @Test
    fun objectiveRankingShouldSortAscending() {
        val model = buildModel()
        val individuals = listOf(individual(3.0), individual(1.0), individual(2.0))
        val ranked = ObjectiveRanking(model, individuals)
        assertEquals(listOf(1.0, 2.0, 3.0), ranked.map { it.fitness.toDouble() })
        assertEquals(listOf(3.0, 2.0, 1.0), ReversedObjectiveRanking(model, individuals).map { it.fitness.toDouble() })
        assertEquals(individuals, IdentityRanking(model, individuals))
    }

    @Test
    fun boundaryHandlersShouldMapOutOfRangeValues() {
        val iteration = Iteration()
        val lower = Flt64.zero
        val upper = Flt64(10.0)

        assertEquals(10.0, ClampBoundary(iteration, 0, Flt64(15.0), lower, upper).toDouble(), 1.0e-9)
        assertEquals(0.0, ClampBoundary(iteration, 0, Flt64(-5.0), lower, upper).toDouble(), 1.0e-9)
        assertEquals(5.0, ReflectBoundary(iteration, 0, Flt64(15.0), lower, upper).toDouble(), 1.0e-9)
        assertEquals(5.0, WrapBoundary(iteration, 0, Flt64(15.0), lower, upper).toDouble(), 1.0e-9)
        assertEquals(7.0, WrapBoundary(iteration, 0, Flt64(-3.0), lower, upper).toDouble(), 1.0e-9)
    }

    @Test
    fun stoppingCriteriaShouldCompose() {
        val iteration = Iteration()
        repeat(5) { iteration.next(true) }

        assertTrue(IterationLimit(UInt64(5))(iteration))
        assertFalse(IterationLimit(UInt64(6))(iteration))
        assertFalse(NotBetterLimit(UInt64(1))(iteration))
        assertTrue((IterationLimit(UInt64(5)) or NotBetterLimit(UInt64(1)))(iteration))
        assertFalse((IterationLimit(UInt64(6)) and NotBetterLimit(UInt64(1)))(iteration))
        assertFalse(NeverStop(iteration))
        assertFalse(TimeLimit(10.seconds)(iteration))
    }

    @Test
    fun topologiesShouldReturnExpectedNeighbours() {
        assertEquals((0 until 5).toSet(), GlobalTopology.neighbours(0, 5))
        assertEquals(setOf(4, 0, 1), RingKTopology(1).neighbours(0, 5))
        assertEquals(setOf(0, 3, 4), RingKTopology(1).neighbours(4, 5))
        assertEquals(setOf(3), RingKTopology(0).neighbours(3, 5))
        assertEquals(setOf(1, 3, 4, 5, 7), VonNeumannTopology(3).neighbours(4, 9))
        assertEquals(setOf(0, 1, 2, 3, 4), RandomTopology(10, SeededEntropy()).neighbours(0, 5))
    }

    @Test
    fun fitnessEvaluatorsShouldScoreSolutions() {
        val model = buildModel()
        val optimum = listOf(Flt64.one, Flt64(2.0))
        val fitness = ObjectiveFitnessEvaluator(model, optimum)
        assertNotNull(fitness)
        assertEquals(0.0, fitness.toDouble(), 1.0e-9)

        val individual = ObjectiveFitnessEvaluator.toIndividual(model, optimum)
        assertNotNull(individual)
        assertEquals(optimum, individual.solution)

        val aware = ConstraintAwareFitnessEvaluator(model, optimum)
        assertNotNull(aware)
    }

    @Test
    fun bestArchiveShouldKeepTopIndividuals() {
        val model = buildModel()
        val archive = BestArchive<Flt64, Flt64>(2)

        assertTrue(archive.update(model, listOf(individual(3.0), individual(1.0), individual(2.0))))
        assertEquals(2, archive.individuals.size)
        assertEquals(1.0, archive.individuals.first().fitness.toDouble(), 1.0e-9)

        assertFalse(archive.update(model, listOf(individual(5.0))))
        assertTrue(archive.update(model, listOf(individual(0.5))))
        assertEquals(0.5, archive.individuals.first().fitness.toDouble(), 1.0e-9)

        archive.clear()
        assertTrue(archive.individuals.isEmpty())
    }

    @Test
    fun historyBestArchiveShouldKeepOnlyTheBestEver() {
        val model = buildModel()
        val archive = HistoryBestArchive<Flt64, Flt64>()

        assertTrue(archive.update(model, listOf(individual(3.0))))
        assertFalse(archive.update(model, listOf(individual(4.0))))
        assertTrue(archive.update(model, listOf(individual(2.0))))
        assertEquals(1, archive.individuals.size)
        assertEquals(2.0, archive.individuals.first().fitness.toDouble(), 1.0e-9)
    }

    @Test
    fun initializersShouldProduceRequestedAmount() {
        val model = buildModel()
        val iteration = Iteration()

        val fromModel = ModelInitializer(iteration, model, UInt64(3), IntoValue.Identity)
        assertEquals(3, fromModel.size)

        // 无有限上下界时回退到模型初始解生成器 / Falls back to the model generator when bounds are not finite
        val random = RandomUniformInitializer(SeededEntropy())
        val fromRandom = random(iteration, model, UInt64(4), IntoValue.Identity)
        assertEquals(4, fromRandom.size)
    }

    @Test
    fun diversityMetricsShouldMeasureSpread() {
        val spread = listOf(
            listOf(Flt64.zero, Flt64.zero),
            listOf(Flt64(3.0), Flt64(4.0))
        )
        assertEquals(5.0, MeanPairwiseDistance(spread, IntoValue.Identity).toDouble(), 1.0e-9)
        assertEquals(0.0, MeanPairwiseDistance(listOf(spread.first()), IntoValue.Identity).toDouble(), 1.0e-9)

        val grid = ArrayList<List<Flt64>>()
        for (i in 0..9) {
            for (j in 0..9) {
                grid.add(listOf(Flt64(i / 9.0), Flt64(j / 9.0)))
            }
        }
        val dimension = FractalDimensionDiversity(grid, IntoValue.Identity).toDouble()
        assertTrue(dimension > 0.5, "fractal dimension diversity should be positive but was $dimension")
    }

    @Test
    fun survivorSelectionsShouldPickBestIndividuals() {
        val model = buildModel()
        val parents = listOf(individual(3.0), individual(4.0))
        val offspring = listOf(individual(1.0), individual(2.0))

        assertEquals(
            listOf(1.0, 2.0),
            ElitistSurvivor(model, parents, offspring, 2).map { it.fitness.toDouble() }
        )
        assertEquals(
            listOf(1.0, 2.0),
            GenerationalSurvivor(model, parents, offspring, 2).map { it.fitness.toDouble() }
        )
        assertEquals(
            listOf(1.0),
            TruncationSurvivor(1)(model, parents, offspring, 5).map { it.fitness.toDouble() }
        )
        assertEquals(
            2.0,
            ElitistPreservingSurvivor(1)(model, listOf(individual(2.0)), offspring, 2)
                .map { it.fitness.toDouble() }
                .max(),
            1.0e-9
        )
    }

    @Test
    fun neighborhoodShouldPerturbWithinBounds() {
        val model = buildModel()
        val individual = SolutionWithFitness(listOf(Flt64(0.5), Flt64(0.5)), Flt64.zero)
        val entropy = SeededEntropy()

        val random = RandomPerturbation(1)(Iteration(), individual, model, entropy, IntoValue.Identity)
        assertEquals(2, random.size)
        assertTrue(random.all { it.toDouble().isFinite() })

        val gaussian = GaussianPerturbation(2, Flt64(0.1))(Iteration(), individual, model, entropy, IntoValue.Identity)
        assertEquals(2, gaussian.size)
        assertTrue(gaussian.all { it.toDouble().isFinite() })

        val levy = LevyPerturbation(1)(Iteration(), individual, model, entropy, IntoValue.Identity)
        assertEquals(2, levy.size)
        assertTrue(levy.all { it.toDouble().isFinite() })
    }

    @Test
    fun stochasticStepsShouldHaveRequestedDimension() {
        val entropy = SeededEntropy()

        when (val step = LevyFlight(3)(entropy)) {
            is Ok -> {
                assertEquals(3, step.value.size)
                assertTrue(step.value.all { it.toDouble().isFinite() })
            }

            else -> fail("Lévy flight sampling failed: $step")
        }

        when (val step = NoiseStep(4, Uniform(Flt64.zero, Flt64.one))(entropy)) {
            is Ok -> {
                assertEquals(4, step.value.size)
                assertTrue(step.value.all { it.toDouble() in 0.0..1.0 })
            }

            else -> fail("noise sampling failed: $step")
        }
    }

    @Test
    fun parameterSourcesShouldFollowTheirSchedules() {
        val iteration = Iteration()
        repeat(5) { iteration.next(true) }

        assertEquals(5.0, ConstantSource(Flt64(5.0))(iteration).toDouble(), 1.0e-9)
        assertEquals(5.0, LinearSource(Flt64.zero, Flt64(10.0), UInt64(10))(iteration).toDouble(), 1.0e-9)
        assertEquals(0.03125, GeometricSource(Flt64.one, Flt64(0.5))(iteration).toDouble(), 1.0e-9)
        assertEquals(
            kotlin.math.exp(-0.5),
            ExponentialSource(Flt64.one, Flt64(0.1))(iteration).toDouble(),
            1.0e-9
        )
    }

    @Test
    fun chaoticAndFractalSourcesShouldStayWithinTargetRange() {
        val iteration = Iteration()

        val chaotic = ChaoticSource(
            generator = SeededEntropy(),
            domainLowerBound = Flt64.zero,
            domainUpperBound = Flt64.one,
            lowerBound = Flt64(-1.0),
            upperBound = Flt64.one
        )
        repeat(50) {
            val value = chaotic(iteration).toDouble()
            assertTrue(value in -1.0..1.0, "chaotic parameter out of range: $value")
        }

        val fractal = FractalSource(
            generator = Generator { point2(Flt64(3.0), Flt64(4.0)) },
            lowerBound = Flt64.zero,
            upperBound = Flt64(100.0)
        )
        val fractalValue = fractal(iteration).toDouble()
        assertTrue(fractalValue in 0.0..100.0, "fractal parameter out of range: $fractalValue")
    }

    @Test
    fun distributionAndJitteredSourcesShouldRespectBounds() {
        val iteration = Iteration()

        val distribution = DistributionSource(
            distribution = Uniform(Flt64.zero, Flt64.one),
            entropy = SeededEntropy(),
            lowerBound = Flt64(2.0),
            upperBound = Flt64(3.0)
        )
        repeat(50) {
            assertTrue(distribution(iteration).toDouble() in 2.0..3.0)
        }

        val jittered = JitteredSource(
            base = ConstantSource(Flt64.one),
            entropy = SeededEntropy(),
            amplitude = Flt64(0.1)
        )
        repeat(50) {
            assertTrue(jittered(iteration).toDouble() in 0.9..1.1)
        }
    }
}
