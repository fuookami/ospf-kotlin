/**
 * Q 学习算子族与骨架反馈通道测试 / Q-learning operator family and skeleton reward channel tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

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
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ConstantSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.NoMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.topology.GlobalTopology
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.GreyWolfUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SineCosineUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.runner.PopulationRunner
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class QlearningEntropy(seed: Long = 20240601L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun qlearningModel() = CallBackModel(
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
 * 构造一个只带解与目标值的个体 / Build an individual carrying only a solution and an objective value
 *
 * @param values 解分量 / Solution components
 * @return 个体 / Individual
*/
private fun qlearningIndividual(vararg values: Double): SolutionWithFitness<Flt64, Flt64> {
    val solution = values.map { Flt64(it) }
    var fitness = 0.0
    for (component in values) {
        fitness += component * component
    }
    return SolutionWithFitness(solution = solution, fitness = Flt64(fitness))
}

class QTableTest {

    @Test
    fun shouldAllocateOneEntryPerAction() {
        // 旧实现用 `mutableListOf(Flt64(9))` 构造，得到的是只有一个元素的列表，Q 表退化成标量。
        // The legacy code built the row with `mutableListOf(Flt64(9))`, a single-element list, collapsing the table.
        val table = QTable(stateAmount = 1, actionAmount = 9)
        assertEquals(9, table.valuesOf(0).size)
    }

    @Test
    fun shouldUseTheNextStateInTheTemporalDifferenceTarget() {
        val table = QTable(stateAmount = 2, actionAmount = 2)

        // 先在下一状态写入一个已知价值。 / Seed a known value in the next state first.
        when (val seeded = table.update(state = 1, action = 0, reward = Flt64(4.0), nextState = 1, alpha = Flt64.one, gamma = Flt64.zero)) {
            is Ok -> assertEquals(4.0, seeded.value.toDouble(), 1.0e-9)
            else -> fail("seeding failed: $seeded")
        }

        // α=1、γ=0.5、r=0 ⇒ Q(0,0) = 0 + 0.5 · max Q(1,·) = 2；若误用当前状态则结果会是 0。
        // α=1, γ=0.5, r=0 ⇒ Q(0,0) = 0 + 0.5 · max Q(1,·) = 2; using the current state instead would give 0.
        when (val result = table.update(state = 0, action = 0, reward = Flt64.zero, nextState = 1, alpha = Flt64.one, gamma = Flt64(0.5))) {
            is Ok -> assertEquals(2.0, result.value.toDouble(), 1.0e-9)
            else -> fail("update failed: $result")
        }
        assertEquals(2.0, table.valueOf(0, 0).toDouble(), 1.0e-9)
    }

    @Test
    fun shouldRejectIllegalArgumentsExplicitly() {
        val table = QTable(stateAmount = 2, actionAmount = 2)
        assertTrue(
            table.update(state = 5, action = 0, reward = Flt64.zero, nextState = 0, alpha = Flt64.one, gamma = Flt64.one) is Failed,
            "out-of-range state must fail"
        )
        assertTrue(
            table.update(state = 0, action = 5, reward = Flt64.zero, nextState = 0, alpha = Flt64.one, gamma = Flt64.one) is Failed,
            "out-of-range action must fail"
        )
        assertTrue(
            table.update(state = 0, action = 0, reward = Flt64.zero, nextState = 0, alpha = Flt64(1.5), gamma = Flt64.one) is Failed,
            "alpha outside [0, 1] must fail"
        )
        assertTrue(
            table.update(state = 0, action = 0, reward = Flt64.zero, nextState = 0, alpha = Flt64.one, gamma = Flt64(-0.5)) is Failed,
            "gamma outside [0, 1] must fail"
        )
    }

    @Test
    fun shouldPreferTheLargestActionValueWithLowestIndexTieBreak() {
        val table = QTable(stateAmount = 1, actionAmount = 3)
        assertEquals(0, table.bestAction(0), "all-equal values must resolve to the lowest index")

        table.update(state = 0, action = 2, reward = Flt64(3.0), nextState = 0, alpha = Flt64.one, gamma = Flt64.zero)
        assertEquals(2, table.bestAction(0))

        table.update(state = 0, action = 1, reward = Flt64(3.0), nextState = 0, alpha = Flt64.one, gamma = Flt64.zero)
        assertEquals(1, table.bestAction(0), "equal best values must still resolve to the lowest index")
    }
}

class StateDiscretizerTest {

    @Test
    fun shouldDeclareNineStatesForThreeLevels() {
        val discretizer = DiversityStagnationDiscretizer(levels = 3)
        assertEquals(9, discretizer.stateAmount)
        assertEquals(4, DiversityStagnationDiscretizer(levels = 2).stateAmount)
    }

    @Test
    fun shouldMapACollapsedPopulationToTheFirstState() {
        val discretizer = DiversityStagnationDiscretizer(levels = 3)
        val collapsed = listOf(
            qlearningIndividual(1.0, 1.0),
            qlearningIndividual(1.0, 1.0),
            qlearningIndividual(1.0, 1.0)
        )
        val state = discretizer(
            iteration = Iteration(),
            members = collapsed,
            converter = IntoValue.Identity
        )
        assertEquals(0, state, "a fully collapsed population has zero diversity and zero stagnation")
    }

    @Test
    fun shouldSeparateSpreadFromCollapsedPopulations() {
        val discretizer = DiversityStagnationDiscretizer(levels = 3)
        val collapsed = listOf(qlearningIndividual(1.0, 1.0), qlearningIndividual(1.0, 1.0))
        val spread = listOf(qlearningIndividual(-5.0, -5.0), qlearningIndividual(5.0, 5.0))

        val collapsedState = discretizer(iteration = Iteration(), members = collapsed, converter = IntoValue.Identity)
        val spreadState = discretizer(iteration = Iteration(), members = spread, converter = IntoValue.Identity)
        assertTrue(
            spreadState > collapsedState,
            "a spread population must map to a higher state than a collapsed one, got $spreadState vs $collapsedState"
        )
    }

    @Test
    fun shouldRaiseTheStateWhenSearchStagnates() {
        val discretizer = DiversityStagnationDiscretizer(levels = 3)
        val spread = listOf(qlearningIndividual(-5.0, -5.0), qlearningIndividual(5.0, 5.0))

        val fresh = Iteration()
        val stagnant = Iteration()
        repeat(9) { stagnant.next(false) }

        val freshState = discretizer(iteration = fresh, members = spread, converter = IntoValue.Identity)
        val stagnantState = discretizer(iteration = stagnant, members = spread, converter = IntoValue.Identity)
        assertTrue(
            stagnantState % 3 > freshState % 3,
            "the stagnation axis must rise after consecutive non-improving rounds"
        )
    }
}

class ActionSelectorTest {

    @Test
    fun greedyShouldPickTheArgmax() {
        val values = listOf(Flt64(1.0), Flt64(3.0), Flt64(2.0))
        assertEquals(1, GreedyActionSelector(values, QlearningEntropy()))
    }

    @Test
    fun epsilonGreedyShouldExploreAcrossManyDraws() {
        val values = listOf(Flt64(9.0), Flt64.zero)
        val entropy = QlearningEntropy(97L)
        val selector = EpsilonGreedyActionSelector(epsilon = Flt64.one)

        var explored = false
        repeat(50) {
            if (selector(values, entropy) != 0) {
                explored = true
            }
        }
        assertTrue(explored, "epsilon = 1 must be able to pick a non-greedy action")
    }

    @Test
    fun epsilonGreedyWithZeroEpsilonShouldStayGreedy() {
        val values = listOf(Flt64.zero, Flt64(5.0))
        val selector = EpsilonGreedyActionSelector(epsilon = Flt64.zero)
        repeat(20) {
            assertEquals(1, selector(values, QlearningEntropy()))
        }
    }

    @Test
    fun softmaxShouldReachSubOptimalActions() {
        val values = listOf(Flt64(2.0), Flt64.one)
        val entropy = QlearningEntropy(101L)
        val selector = SoftmaxActionSelector(temperature = Flt64(2.0))

        var reachedSecond = false
        repeat(80) {
            if (selector(values, entropy) == 1) {
                reachedSecond = true
            }
        }
        assertTrue(reachedSecond, "softmax sampling must reach a sub-optimal action")
    }
}

class QlearningAgentTest {

    @Test
    fun firstStepShouldBootstrapWithoutUpdating() {
        val agent = QlearningAgent(
            discretizer = DiversityStagnationDiscretizer(),
            actionAmount = 2,
            selector = GreedyActionSelector,
            reward = ImprovementReward(),
            alpha = Flt64.one,
            gamma = Flt64.one,
            randomGenerator = QlearningEntropy()
        )
        val members = listOf(qlearningIndividual(0.0, 1.0), qlearningIndividual(2.0, 3.0))

        assertFalse(agent.started)
        val first = agent.step(Iteration(), members, improved = true, converter = IntoValue.Identity)
        assertNull(first, "the first round only bootstraps the state")
        assertTrue(agent.started)

        val second = agent.step(Iteration(), members, improved = true, converter = IntoValue.Identity)
        assertNotNull(second, "the second round must produce a temporal-difference update")
    }

    @Test
    fun shouldClampHyperParameters() {
        val agent = QlearningAgent(
            discretizer = DiversityStagnationDiscretizer(),
            actionAmount = 2,
            selector = GreedyActionSelector,
            reward = ImprovementReward(),
            alpha = Flt64(9.0),
            gamma = Flt64(-4.0),
            randomGenerator = QlearningEntropy()
        )
        assertEquals(1.0, agent.alpha.toDouble(), 1.0e-9)
        assertEquals(0.0, agent.gamma.toDouble(), 1.0e-9)
    }
}

class QlearningIntegrationTest {

    @Test
    fun skeletonShouldFeedRewardsBackIntoTheTable(): Unit = runBlocking {
        val model = qlearningModel()
        val entropy = QlearningEntropy(107L)
        val agent = QlearningAgent(
            discretizer = DiversityStagnationDiscretizer(),
            actionAmount = 2,
            selector = EpsilonGreedyActionSelector(epsilon = Flt64.one),
            reward = ImprovementReward(),
            alpha = Flt64(0.5),
            gamma = Flt64(0.9),
            randomGenerator = entropy
        )
        val update = QlearningUpdate(candidates = listOf(GreyWolfUpdate(), SineCosineUpdate()), agent = agent)

        val runner = PopulationRunner<Flt64, Flt64>(
            size = 6,
            update = update,
            mutation = NoMutation,
            survivor = ElitistSurvivor,
            mutationRate = Flt64.zero,
            topology = GlobalTopology,
            entropy = entropy,
            archive = BestArchive(1),
            stopping = IterationLimit(UInt64(15)),
            converter = IntoValue.Identity,
            observers = listOf(update)
        )

        val members = runner.run(model)
        assertTrue(members.isNotEmpty())

        var learned = false
        for (state in 0 until agent.table.stateAmount) {
            for (value in agent.table.valuesOf(state)) {
                if (value.toDouble() != 0.0) {
                    learned = true
                }
            }
        }
        assertTrue(learned, "the skeleton must push real rewards into the Q table")
    }

    @Test
    fun qLearningSourceShouldSelectAmongCandidates(): Unit = runBlocking {
        val source = QlearningSource(
            candidates = listOf(ConstantSource(Flt64(0.1)), ConstantSource(Flt64(0.9))),
            randomGenerator = QlearningEntropy(113L)
        )
        val members = listOf(qlearningIndividual(0.0, 1.0), qlearningIndividual(2.0, 3.0))
        repeat(6) {
            source.observe(Iteration(), members, improved = it % 2 == 0, converter = IntoValue.Identity)
        }

        val value = source(Iteration()).toDouble()
        assertTrue(
            value == 0.1 || value == 0.9,
            "a Q-learning parameter source must return one of its candidates, got $value"
        )
    }

    @Test
    fun qLearningSolverShouldConverge(): Unit = runBlocking {
        val model = qlearningModel()
        val solver = ComposedSolvers.qLearning<Flt64, Flt64>(
            size = 8,
            stopping = IterationLimit(UInt64(20)),
            converter = IntoValue.Identity,
            entropy = QlearningEntropy(127L)
        )

        assertEquals("q-learning", solver.name)
        when (val result = solver(model)) {
            is Ok -> {
                val best = result.value.bestObjective
                assertNotNull(best)
                assertTrue(best.toDouble().isFinite(), "Q-learning search returned a non-finite objective")
                assertEquals(2, result.value.bestSolution!!.size)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
            }

            else -> fail("Q-learning solver failed: $result")
        }
    }
}
