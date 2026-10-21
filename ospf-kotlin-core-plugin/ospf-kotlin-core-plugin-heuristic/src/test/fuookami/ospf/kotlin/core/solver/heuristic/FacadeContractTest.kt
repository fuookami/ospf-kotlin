/**
 * 门面契约测试：每个门面必须转发其工厂的全部参数 / Facade contract tests: every facade must forward all of its factory's parameters
 *
 * 这些用例的意义在于**编译期**保证：只要某个门面少了一个参数，下面的调用就无法通过编译。 /
 * What these cases guarantee is compile-time: if a facade were missing a parameter, the calls below would not compile.
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.GenerationalSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SineCosineUpdate

/** 确定性熵源 / Deterministic entropy source */
private val facadeEntropy: Generator<Flt64> = Generator { Flt64(0.5) }

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun facadeModel() = CallBackModel(
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

class PopulationFacadeContractTest {

    @Test
    fun everyPopulationFacadeShouldAcceptEveryParameter() {
        val stopping = IterationLimit(UInt64(5))
        val forwarded = SineCosineUpdate()

        // 每个调用都显式给出 update / survivor / archiveAmount / initializer 四项可选参数。
        // Every call spells out the four optional parameters update / survivor / archiveAmount / initializer.
        val solvers = listOf(
            arithmeticOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            batAlgorithmOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            cuckooSearchOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            differentialEvolution<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            energyValleyOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            fireflyOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            germinalCenterOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            grasshopperOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            gravitationalSearchOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            harmonySearch<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            harrisHawkOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            mothFlameOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            salpSwarmOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            seagullOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            teachingLearningOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            warStrategyOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            waterCycleAlgorithm<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer),
            whaleOptimization<Flt64, Flt64>(size = 6, stopping = stopping, converter = IntoValue.Identity, entropy = facadeEntropy, update = forwarded, survivor = GenerationalSurvivor, archiveAmount = 2, initializer = ModelInitializer)
        )

        assertEquals(18, solvers.size)
        for (solver in solvers) {
            assertTrue(solver.name.isNotEmpty(), "every facade must produce a named solver")
        }
    }

    @Test
    fun trajectoryFacadesShouldAcceptEveryParameter() {
        val stopping = IterationLimit(UInt64(5))
        val climbing = hillClimbing<Flt64, Flt64>(
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            neighborhood = RandomPerturbation(2),
            initializer = ModelInitializer
        )
        val neighborhood = neighborhoodSearch<Flt64, Flt64>(
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            neighborhood = RandomPerturbation(2),
            initializer = ModelInitializer
        )

        assertEquals("hill-climbing", climbing.name)
        assertEquals("ns", neighborhood.name)
    }

    @Test
    fun shadeFacadesShouldAcceptEveryParameter() {
        val stopping = IterationLimit(UInt64(5))
        val basic = shade<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            memorySize = 4,
            pBestRate = 0.15,
            archiveAmount = 2
        )
        val linear = lShade<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            policy = LShadePolicy(minimumSize = 4, memorySize = 4, pBestRate = 0.15, archiveAmount = 2)
        )

        assertEquals("shade", basic.name)
        assertEquals("l-shade", linear.name)
    }

    @Test
    fun particleSwarmFacadesShouldAcceptEveryParameter() {
        val stopping = IterationLimit(UInt64(5))
        val adaptive = adaptiveParticleSwarm<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            policy = AdaptiveParticleSwarmPolicy(
                maxVelocity = Flt64(500.0),
                archiveAmount = 2,
                initializer = ModelInitializer
            )
        )
        val comprehensive = comprehensiveLearningParticleSwarm<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            policy = ComprehensiveLearningParticleSwarmPolicy(
                learningWeight = Flt64(1.2),
                maxVelocity = Flt64(500.0),
                refreshInterval = 3,
                stagnationLimit = 3,
                archiveAmount = 2,
                initializer = ModelInitializer
            )
        )

        assertEquals("apso", adaptive.name)
        assertEquals("clpso", comprehensive.name)
    }

    @Test
    fun cmaEsFacadeShouldAcceptEveryParameter() {
        val solver = cmaEs<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(5)),
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            initialSigma = 0.5,
            archiveAmount = 2
        )
        assertEquals("cma-es", solver.name)
    }
}

class FacadeExecutionTest {

    @Test
    fun policyBackedFacadesShouldSolve(): Unit = runBlocking {
        val model = facadeModel()
        val stopping = IterationLimit(UInt64(8))

        val adaptive = adaptiveParticleSwarm<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy
        )
        val linear = lShade<Flt64, Flt64>(
            size = 8,
            stopping = stopping,
            converter = IntoValue.Identity,
            entropy = facadeEntropy
        )

        for (solver in listOf(adaptive, linear)) {
            when (val result = solver(model)) {
                is Ok -> {
                    val best = result.value.bestObjective
                    assertNotNull(best, "${solver.name} returned no objective")
                    assertTrue(best.toDouble().isFinite(), "${solver.name} returned a non-finite objective")
                }

                else -> fail("${solver.name} failed: $result")
            }
        }
    }

    @Test
    fun forwardedArchiveAmountShouldTakeEffect(): Unit = runBlocking {
        // archiveAmount = 3 应让档案保留 3 个解；若门面没转发该参数，档案只会保留默认的 1 个。
        // archiveAmount = 3 should keep three solutions; had the facade dropped it, only the default one would remain.
        val model = facadeModel()
        val solver = harmonySearch<Flt64, Flt64>(
            size = 10,
            stopping = IterationLimit(UInt64(10)),
            converter = IntoValue.Identity,
            entropy = facadeEntropy,
            archiveAmount = 3
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertNotNull(result.value.bestObjective, "harmonySearch returned no objective")
                val runner = (solver as RunnerSolver<Flt64, Flt64>).runner
                assertEquals(
                    3,
                    runner.archive.individuals.size,
                    "archiveAmount = 3 must reach the archive through the facade"
                )
            }

            else -> fail("harmonySearch failed: $result")
        }
    }
}
