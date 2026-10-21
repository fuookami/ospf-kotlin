/**
 * SHADE 契约测试 / SHADE contract tests
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
import fuookami.ospf.kotlin.core.solver.heuristic.runner.ShadeMemory
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.HeuristicSolutionStatus
import fuookami.ospf.kotlin.core.solver.heuristic.shade
import fuookami.ospf.kotlin.core.solver.heuristic.lShade

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class ShadeEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun shadeModel() = CallBackModel(
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
private fun shadeStartingObjective(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): Double {
    val solution = model.initialSolutions(UInt64.one).firstOrNull() ?: return Double.POSITIVE_INFINITY
    return model.objective(solution)?.toDouble() ?: Double.POSITIVE_INFINITY
}

class ShadeTest {

    @Test
    fun shadeMemoryShouldMoveTowardsSuccessfulParameters() {
        val entropy = ShadeEntropy(173L)
        val memory = ShadeMemory()

        // 全部成功参数都偏大时，莱默均值会把 F 的记忆推高。
        // When every successful parameter is large, the Lehmer mean pushes the F memory upwards.
        val updated = memory.update(
            successfulF = listOf(0.9, 0.95, 0.85),
            successfulCr = listOf(0.8, 0.9, 0.85)
        )

        val written = updated.memoryF[memory.cursor]
        assertTrue(written > 0.8, "Lehmer mean should be biased high, was $written")
        assertTrue(written <= 1.0, "the memory must stay within [0, 1], was $written")
        assertEquals(1, updated.cursor)
        assertEquals(ShadeMemory.DEFAULT_SIZE, updated.memoryF.size)

        // 空成功集不改变记忆。
        // An empty success set leaves the memory untouched.
        assertEquals(updated, updated.update(emptyList(), emptyList()))

        val sampled = updated.sample(entropy)
        assertTrue(sampled.differentialWeight in 0.0..1.0)
        assertTrue(sampled.crossRate in 0.0..1.0)
    }

    @Test
    fun shadeShouldConvergeOnAQuadratic(): Unit = runBlocking {
        val model = shadeModel()
        val baseline = shadeStartingObjective(model)
        val solver = shade<Flt64, Flt64>(
            size = 20,
            stopping = IterationLimit(UInt64(60)),
            converter = IntoValue.Identity,
            entropy = ShadeEntropy(179L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("shade", solver.name)
                val best = result.value.bestObjective
                assertNotNull(best)
                assertTrue(best.toDouble().isFinite(), "SHADE returned a non-finite objective")
                assertTrue(
                    best.toDouble() <= baseline + 1.0e-9,
                    "SHADE worsened the objective: ${best.toDouble()} > $baseline"
                )
                assertEquals(2, result.value.bestSolution!!.size)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
            }

            else -> fail("SHADE failed: $result")
        }
    }

    @Test
    fun lShadeShouldConvergeWithShrinkingPopulation(): Unit = runBlocking {
        val model = shadeModel()
        val baseline = shadeStartingObjective(model)
        val solver = lShade<Flt64, Flt64>(
            size = 20,
            stopping = IterationLimit(UInt64(60)),
            converter = IntoValue.Identity,
            entropy = ShadeEntropy(181L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("l-shade", solver.name)
                val best = result.value.bestObjective
                assertNotNull(best)
                assertTrue(best.toDouble().isFinite(), "L-SHADE returned a non-finite objective")
                assertTrue(
                    best.toDouble() <= baseline + 1.0e-9,
                    "L-SHADE worsened the objective: ${best.toDouble()} > $baseline"
                )
                assertEquals(2, result.value.bestSolution!!.size)
            }

            else -> fail("L-SHADE failed: $result")
        }
    }
}
