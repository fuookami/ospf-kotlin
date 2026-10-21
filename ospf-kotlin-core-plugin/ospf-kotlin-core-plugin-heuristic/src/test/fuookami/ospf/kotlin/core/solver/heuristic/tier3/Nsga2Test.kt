/**
 * NSGA-II 契约测试 / NSGA-II contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.math.*
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
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.MinimizationDominance
import fuookami.ospf.kotlin.core.solver.heuristic.solver.MultiObjectiveSolvers

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

/**
 * ZDT1 的两个目标 / The two objectives of ZDT1
 *
 * f1 = x0
 * g  = 1 + 9·x1
 * f2 = g·(1 − sqrt(f1/g))
 *
 * @param solution 决策变量 / Decision variables
 * @return 目标向量 / Objective vector
*/
private fun zdt1(solution: List<Flt64>): List<Flt64> {
    val first = solution[0].toDouble().coerceIn(0.0, 1.0)
    val second = solution[1].toDouble().coerceIn(0.0, 1.0)
    val g = 1.0 + 9.0 * second
    val ratios = (first / g).coerceIn(0.0, 1.0)
    val f2 = g * (1.0 - sqrt(ratios))
    return listOf(Flt64(first), Flt64(f2))
}

/** 构建二变量模型，模型自身的标量目标只作占位 / Build a two-variable model; its scalar objective is a placeholder */
private fun buildModel() = CallBackModel(
    objectCategory = ObjectCategory.Minimum,
    initialSolutionGenerator = { (solution, variable) ->
        Flt64(((solution.toInt() * 5 + variable.toInt() * 3) % 11).toDouble() / 11.0)
    },
    converter = IntoValue.Identity
).also { model ->
    model.add(RealVar("x"))
    model.add(RealVar("y"))
    model.addObject(
        ObjectCategory.Minimum,
        { solution: List<Flt64> -> solution[0] }
    )
}

class Nsga2Test {

    @Test
    fun nsga2ShouldReturnAMutuallyNonDominatedFront(): Unit = runBlocking {
        val model = buildModel()
        val dominance = MinimizationDominance(2)
        val solver = MultiObjectiveSolvers.nsga2<Flt64>(
            size = 12,
            objectivesOf = { solution -> zdt1(solution) },
            dominance = dominance,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(163L),
            archiveAmount = 20
        )

        when (val result = solver(model)) {
            is Ok -> {
                val front = result.value.front
                val objectives = result.value.objectives

                assertEquals("nsga-ii", solver.name)
                assertTrue(front.isNotEmpty(), "NSGA-II returned an empty front")
                assertEquals(front.size, objectives.size, "front and objectives are misaligned")
                assertEquals(2, front.first().size, "decision vector has the wrong dimension")

                assertTrue(
                    objectives.all { vector -> vector.all { it.toDouble().isFinite() } },
                    "the front contains a non-finite objective"
                )

                for (left in objectives.indices) {
                    for (right in objectives.indices) {
                        if (left == right) {
                            continue
                        }
                        assertFalse(
                            dominance.dominates(objectives[left], objectives[right]),
                            "the front contains a dominated pair"
                        )
                    }
                }
            }

            else -> fail("NSGA-II failed: $result")
        }
    }

    @Test
    fun nsga2ShouldRespectTheArchiveCapacity(): Unit = runBlocking {
        val model = buildModel()
        val solver = MultiObjectiveSolvers.nsga2<Flt64>(
            size = 16,
            objectivesOf = { solution -> zdt1(solution) },
            dominance = MinimizationDominance(2),
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = SeededEntropy(167L),
            archiveAmount = 3
        )

        when (val result = solver(model)) {
            is Ok -> assertTrue(
                result.value.front.size <= 3,
                "archive capacity was exceeded: ${result.value.front.size}"
            )

            else -> fail("NSGA-II failed: $result")
        }
    }
}
