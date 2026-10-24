/**
 * 交叉与变异算子契约测试 / Crossover and mutation operator contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.Cross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.ArithmeticCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.BlendCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.BoundaryMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.CauchyMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.ChaoticMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.CycleCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.DifferentialCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.GaussianMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.InsertionMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.InversionMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.LevyMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.MultiPointCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.NonUniformMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.OrderCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.PolynomialMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.ScrambleMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SimulatedBinaryCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SinglePointCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.SwapMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.TwoPointCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.UniformCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.UniformMutation

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

/** 构建四变量测试模型 / Build a four-variable test model */
private fun buildModel() = CallBackModel(converter = IntoValue.Identity).also { model ->
    for (name in listOf("a", "b", "c", "d")) {
        model.add(RealVar(name))
    }
    model.addObject(
        ObjectCategory.Minimum,
        { solution: List<Flt64> ->
            var total = Flt64.zero
            for (value in solution) {
                total += value
            }
            total
        }
    )
}

/** 构造个体 / Build an individual */
private fun parent(vararg values: Double): SolutionWithFitness<Flt64, Flt64> {
    var total = 0.0
    for (value in values) {
        total += value
    }
    return SolutionWithFitness(
        solution = values.map { Flt64(it) },
        fitness = Flt64(total)
    )
}

/** 解的多重集合 / Multiset of a solution */
private fun multiset(solution: List<Flt64>): List<Double> {
    return solution.map { it.toDouble() }.sorted()
}

class CrossoverMutationTest {

    private val model = buildModel()
    private val iteration = Iteration()

    @Test
    fun cycleCrossoverMatchesFirstOccurrenceReferenceIncludingNonPermutations() {
        fun reference(source: List<Flt64>, other: List<Flt64>): List<Flt64> {
            val child = other.toMutableList()
            val assigned = BooleanArray(source.size)
            var index = 0
            while (index in source.indices && !assigned[index]) {
                assigned[index] = true
                child[index] = source[index]
                index = other.indexOf(source[index])
            }
            return child
        }
        val random = kotlin.random.Random(731)
        for (size in listOf(1, 2, 8, 128)) {
            repeat(20) {
                val first = List(size) { Flt64(random.nextInt(size + 1)) }
                val second = List(size) { Flt64(random.nextInt(size + 1)) }
                val children = cross(CycleCross, listOf(
                    SolutionWithFitness(first, Flt64.zero),
                    SolutionWithFitness(second, Flt64.zero)
                ))
                assertEquals(listOf(reference(first, second), reference(second, first)), children)
            }
        }
    }

    private fun cross(
        operator: Cross,
        parents: List<SolutionWithFitness<Flt64, Flt64>>,
        entropy: Generator<Flt64> = SeededEntropy()
    ): List<List<Flt64>> {
        return operator(
            iteration = iteration,
            parents = parents,
            model = model,
            converter = IntoValue.Identity,
            entropy = entropy
        )
    }

    private fun mutate(
        operator: Mutation,
        individual: SolutionWithFitness<Flt64, Flt64>,
        rate: Double,
        entropy: Generator<Flt64> = SeededEntropy()
    ): List<Flt64> {
        return operator(
            iteration = iteration,
            individual = individual,
            model = model,
            converter = IntoValue.Identity,
            entropy = entropy,
            mutationRate = Flt64(rate)
        )
    }

    @Test
    fun pointCrossoversShouldSpliceParents() {
        val first = parent(1.0, 1.0, 1.0, 1.0)
        val second = parent(2.0, 2.0, 2.0, 2.0)

        for (operator in listOf<Cross>(SinglePointCross, TwoPointCross)) {
            val children = cross(operator, listOf(first, second))
            assertEquals(2, children.size)
            for (child in children) {
                assertEquals(4, child.size)
                for (index in child.indices) {
                    assertTrue(
                        child[index].toDouble() == 1.0 || child[index].toDouble() == 2.0,
                        "gene $index came from neither parent"
                    )
                }
            }
        }
    }

    @Test
    fun multiPointCrossoverShouldKeepGeneProvenance() {
        val first = parent(1.0, 1.0, 1.0, 1.0)
        val second = parent(2.0, 2.0, 2.0, 2.0)
        val children = cross(MultiPointCross(2), listOf(first, second))

        assertEquals(2, children.size)
        for (child in children) {
            assertEquals(4, child.size)
            assertTrue(child.all { it.toDouble() == 1.0 || it.toDouble() == 2.0 })
        }
    }

    @Test
    fun uniformCrossoverShouldMixPerGene() {
        val first = parent(1.0, 1.0, 1.0, 1.0)
        val second = parent(2.0, 2.0, 2.0, 2.0)
        val children = cross(UniformCross(), listOf(first, second))

        assertEquals(2, children.size)
        assertTrue(children.all { child -> child.all { it.toDouble() == 1.0 || it.toDouble() == 2.0 } })
    }

    @Test
    fun arithmeticCrossoverShouldStayBetweenParents() {
        val first = parent(0.0, 0.0, 0.0, 0.0)
        val second = parent(4.0, 4.0, 4.0, 4.0)
        val children = cross(ArithmeticCross, listOf(first, second))

        assertEquals(2, children.size)
        for (child in children) {
            assertTrue(child.all { it.toDouble() in 0.0..4.0 }, "child out of hull: $child")
        }
    }

    @Test
    fun blendCrossoverShouldProduceFiniteGenes() {
        val children = cross(
            BlendCross(Flt64(0.5)),
            listOf(parent(0.0, 1.0, 2.0, 3.0), parent(1.0, 2.0, 3.0, 4.0))
        )
        assertEquals(2, children.size)
        assertTrue(children.all { child -> child.all { it.toDouble().isFinite() } })
    }

    @Test
    fun simulatedBinaryCrossoverShouldProduceFiniteGenes() {
        val children = cross(
            SimulatedBinaryCross(Flt64(20.0)),
            listOf(parent(0.0, 1.0, 2.0, 3.0), parent(1.0, 2.0, 3.0, 4.0))
        )
        assertEquals(2, children.size)
        assertTrue(children.all { child -> child.all { it.toDouble().isFinite() } })
    }

    @Test
    fun differentialCrossoverShouldProduceOneTrial() {
        val children = cross(
            DifferentialCross(Flt64(0.5), Flt64(0.9)),
            listOf(parent(1.0, 1.0, 1.0, 1.0), parent(2.0, 2.0, 2.0, 2.0), parent(3.0, 3.0, 3.0, 3.0))
        )
        assertEquals(1, children.size)
        assertEquals(4, children.first().size)
        assertTrue(children.first().all { it.toDouble().isFinite() })
    }

    @Test
    fun permutationCrossoversShouldPreserveTheMultiset() {
        val first = parent(1.0, 2.0, 3.0, 4.0)
        val second = parent(4.0, 3.0, 2.0, 1.0)

        for (operator in listOf<Cross>(OrderCross, CycleCross)) {
            val children = cross(operator, listOf(first, second))
            assertEquals(2, children.size)
            for (child in children) {
                assertEquals(listOf(1.0, 2.0, 3.0, 4.0), multiset(child))
            }
        }
    }

    @Test
    fun mutationShouldBeSuppressedAtZeroRate() {
        val individual = parent(0.5, 1.5, 2.5, 3.5)
        val operators = listOf<Mutation>(
            GaussianMutation(),
            CauchyMutation(),
            LevyMutation(),
            UniformMutation,
            BoundaryMutation,
            PolynomialMutation(),
            NonUniformMutation(),
            ChaoticMutation(),
            SwapMutation,
            InversionMutation,
            InsertionMutation,
            ScrambleMutation()
        )

        for (operator in operators) {
            val mutated = mutate(operator, individual, 0.0)
            assertEquals(
                individual.solution.map { it.toDouble() },
                mutated.map { it.toDouble() },
                "${operator::class.simpleName} mutated at zero rate"
            )
        }
    }

    @Test
    fun continuousMutationsShouldProduceFiniteGenes() {
        val individual = parent(0.5, 1.5, 2.5, 3.5)
        val operators = listOf(
            GaussianMutation(),
            CauchyMutation(),
            LevyMutation(),
            UniformMutation,
            BoundaryMutation,
            PolynomialMutation(),
            NonUniformMutation(),
            ChaoticMutation()
        )

        for (operator in operators) {
            val mutated = mutate(operator, individual, 1.0)
            assertEquals(4, mutated.size, "${operator::class.simpleName} changed the solution length")
            assertTrue(
                mutated.all { it.toDouble().isFinite() },
                "${operator::class.simpleName} produced a non-finite gene"
            )
        }
    }

    @Test
    fun permutationMutationsShouldPreserveTheMultiset() {
        val individual = parent(1.0, 2.0, 3.0, 4.0)

        for (operator in listOf<Mutation>(SwapMutation, InversionMutation, InsertionMutation, ScrambleMutation())) {
            val mutated = mutate(operator, individual, 1.0)
            assertEquals(
                listOf(1.0, 2.0, 3.0, 4.0),
                multiset(mutated),
                "${operator::class.simpleName} did not preserve the multiset"
            )
        }
    }

    @Test
    fun swapMutationShouldChangeExactlyTwoGenes() {
        val individual = parent(1.0, 2.0, 3.0, 4.0)
        val mutated = mutate(SwapMutation, individual, 1.0)
        val changed = mutated.indices.count { mutated[it].toDouble() != individual.solution[it].toDouble() }
        assertEquals(2, changed)
    }
}
