/**
 * Memetic 组合契约测试 / Memetic composition contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier3

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
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.FixedInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers
import fuookami.ospf.kotlin.core.solver.heuristic.solver.MemeticSolvers

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class MemeticEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun memeticModel() = CallBackModel(
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

class MemeticTest {

    @Test
    fun fixedInitializerShouldReturnTheGivenValues(): Unit = runBlocking {
        val model = memeticModel()
        val initializer = FixedInitializer(listOf(3.0, 4.0))

        val solutions = initializer(
            iteration = fuookami.ospf.kotlin.core.solver.heuristic.Iteration(),
            model = model,
            amount = UInt64.one,
            converter = IntoValue.Identity
        )

        assertEquals(1, solutions.size)
        assertEquals(2, solutions.first().size)
        assertEquals(3.0, solutions.first()[0].toDouble(), 1.0e-9)
        assertEquals(4.0, solutions.first()[1].toDouble(), 1.0e-9)

        // 空数值列表返回空列表，不抛异常。
        // An empty value list yields an empty result instead of throwing.
        val empty = FixedInitializer(emptyList())( 
            iteration = fuookami.ospf.kotlin.core.solver.heuristic.Iteration(),
            model = model,
            amount = UInt64.one,
            converter = IntoValue.Identity
        )
        assertTrue(empty.isEmpty())
    }

    @Test
    fun memeticShouldNotWorsenTheGlobalResult(): Unit = runBlocking {
        val model = memeticModel()
        val genetic = ComposedSolvers.genetic<Flt64, Flt64>(
            size = 8,
            stopping = IterationLimit(UInt64(15)),
            converter = IntoValue.Identity,
            entropy = MemeticEntropy(223L)
        )
        val solver = MemeticSolvers.memetic(
            global = genetic,
            polishStopping = IterationLimit(UInt64(15)),
            converter = IntoValue.Identity,
            entropy = MemeticEntropy(227L)
        )

        when (val globalResult = genetic(model)) {
            is Ok -> {
                val globalBest = globalResult.value.bestObjective
                assertNotNull(globalBest)

                when (val result = solver(model)) {
                    is Ok -> {
                        assertTrue(solver.name.startsWith("memetic-"), "unexpected name: ${solver.name}")
                        val best = result.value.bestObjective
                        assertNotNull(best)
                        assertTrue(best.toDouble().isFinite())
                        assertTrue(
                            best.toDouble() <= globalBest.toDouble() + 1.0e-9,
                            "memetic result is worse than the global phase: ${best.toDouble()} > ${globalBest.toDouble()}"
                        )
                        assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
                        assertEquals(2, result.value.bestSolution!!.size)
                    }

                    else -> fail("memetic solver failed: $result")
                }
            }

            else -> fail("global solver failed: $globalResult")
        }
    }
}
