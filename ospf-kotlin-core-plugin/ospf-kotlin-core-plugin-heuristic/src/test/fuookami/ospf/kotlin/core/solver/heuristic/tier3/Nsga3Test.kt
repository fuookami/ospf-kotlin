/**
 * NSGA-III 与参考点契约测试 / NSGA-III and reference point contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.callback.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.MinimizationDominance
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.ReferencePoints
import fuookami.ospf.kotlin.core.solver.heuristic.solver.MultiObjectiveSolvers

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class Nsga3Entropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}


/** 显式累加，避免项目内 Iterable 扩展遮蔽标准库 / Accumulate explicitly so project extensions cannot shadow the stdlib */
private fun accumulate(values: List<Double>): Double {
    var total = 0.0
    for (value in values) {
        total += value
    }
    return total
}

/** ZDT1 的两个目标 / The two objectives of ZDT1 */
private fun nsga3Objectives(solution: List<Flt64>): List<Flt64> {
    val first = solution[0].toDouble().coerceIn(0.0, 1.0)
    val second = solution[1].toDouble().coerceIn(0.0, 1.0)
    val g = 1.0 + 9.0 * second
    val ratios = (first / g).coerceIn(0.0, 1.0)
    return listOf(Flt64(first), Flt64(g * (1.0 - sqrt(ratios))))
}

/** 构建二变量模型 / Build a two-variable model */
private fun nsga3Model() = CallBackModel(
    objectCategory = ObjectCategory.Minimum,
    initialSolutionGenerator = { (solution, variable) ->
        Flt64(((solution.toInt() * 5 + variable.toInt() * 3) % 11).toDouble() / 11.0)
    },
    converter = IntoValue.Identity
).also { model ->
    model.add(RealVar("x"))
    model.add(RealVar("y"))
    model.addObject(ObjectCategory.Minimum, { solution: List<Flt64> -> solution[0] })
}

class Nsga3Test {

    @Test
    fun dasDennisShouldCoverTheSimplex() {
        // 2 目标 4 分割 → 5 个点；3 目标 4 分割 → C(6, 2) = 15 个点。
        // 2 objectives with 4 divisions gives 5 points; 3 objectives with 4 divisions gives C(6, 2) = 15 points.
        val twoObjectives = ReferencePoints.dasDennis(objectiveAmount = 2, divisions = 4)
        assertEquals(5, twoObjectives.size)
        assertTrue(twoObjectives.all { point -> abs(accumulate(point) - 1.0) < 1.0e-9 })
        assertTrue(twoObjectives.all { point -> point.all { it >= 0.0 } })

        val threeObjectives = ReferencePoints.dasDennis(objectiveAmount = 3, divisions = 4)
        assertEquals(15, threeObjectives.size)
        assertTrue(threeObjectives.all { point -> abs(accumulate(point) - 1.0) < 1.0e-9 })

        // 非法参数不抛异常，返回空列表。
        // Invalid arguments do not throw; an empty list is returned.
        assertTrue(ReferencePoints.dasDennis(objectiveAmount = 0, divisions = 4).isEmpty())
        assertTrue(ReferencePoints.dasDennis(objectiveAmount = 2, divisions = 0).isEmpty())
    }

    @Test
    fun perpendicularDistanceShouldBeZeroOnTheDirection() {
        val reference = listOf(0.5, 0.5)
        assertEquals(0.0, ReferencePoints.perpendicularDistance(listOf(1.0, 1.0), reference), 1.0e-9)
        assertTrue(ReferencePoints.perpendicularDistance(listOf(1.0, 0.0), reference) > 0.0)
    }

    @Test
    fun nsga3ShouldReturnAMutuallyNonDominatedFront(): Unit = runBlocking {
        val model = nsga3Model()
        val dominance = MinimizationDominance(2)
        val solver = MultiObjectiveSolvers.nsga3<Flt64>(
            size = 12,
            objectivesOf = { solution -> nsga3Objectives(solution) },
            dominance = dominance,
            stopping = IterationLimit(UInt64(30)),
            converter = IntoValue.Identity,
            entropy = Nsga3Entropy(191L),
            archiveAmount = 20,
            divisions = 8
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("nsga-iii", solver.name)
                val objectives = result.value.objectives
                assertTrue(objectives.isNotEmpty(), "NSGA-III returned an empty front")
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

            else -> fail("NSGA-III failed: $result")
        }
    }
}
