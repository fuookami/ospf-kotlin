/**
 * CMA-ES 与特征分解契约测试 / CMA-ES and eigendecomposition contract tests
*/
package fuookami.ospf.kotlin.core.solver.heuristic.tier3

import kotlin.math.abs
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
import fuookami.ospf.kotlin.core.solver.heuristic.HeuristicSolutionStatus
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.IterationLimit
import fuookami.ospf.kotlin.core.solver.heuristic.operator.linear.SymmetricEigen
import fuookami.ospf.kotlin.core.solver.heuristic.cmaEs

/**
 * 确定性伪随机熵源 / Deterministic pseudo-random entropy source
 *
 * @property seed 初始种子 / Initial seed
*/
private class CmaEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

/** 构建二变量二次模型 / Build a two-variable quadratic model */
private fun cmaModel() = CallBackModel(
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
private fun cmaStartingObjective(model: AbstractCallBackModelInterface<*, Flt64, Flt64>): Double {
    val solution = model.initialSolutions(UInt64.one).firstOrNull() ?: return Double.POSITIVE_INFINITY
    return model.objective(solution)?.toDouble() ?: Double.POSITIVE_INFINITY
}

class CmaEsTest {

    @Test
    fun symmetricEigenShouldRecoverKnownEigenvalues() {
        // [[2,1],[1,2]] 的特征值是 3 与 1。
        // The eigenvalues of [[2,1],[1,2]] are 3 and 1.
        val matrix = listOf(
            listOf(2.0, 1.0),
            listOf(1.0, 2.0)
        )
        val result = SymmetricEigen.decompose(matrix)
        assertTrue(result.isValid)

        val sorted = result.values.sorted()
        assertEquals(1.0, sorted[0], 1.0e-9)
        assertEquals(3.0, sorted[1], 1.0e-9)

        // 重建矩阵：A = Σ λ_k · v_k v_kᵀ
        // Reconstruct the matrix: A = Σ λ_k · v_k v_kᵀ
        for (row in 0..1) {
            for (column in 0..1) {
                var rebuilt = 0.0
                for (index in result.values.indices) {
                    rebuilt += result.values[index] *
                        result.vectors[index][row] * result.vectors[index][column]
                }
                assertEquals(matrix[row][column], rebuilt, 1.0e-9)
            }
        }

        // 非法输入不抛异常。
        // Invalid input does not throw.
        assertFalse(SymmetricEigen.decompose(emptyList()).isValid)
        assertFalse(SymmetricEigen.decompose(listOf(listOf(1.0, 2.0))).isValid)
    }

    @Test
    fun diagonalMatrixShouldKeepItsAxes() {
        val matrix = listOf(
            listOf(4.0, 0.0),
            listOf(0.0, 9.0)
        )
        val result = SymmetricEigen.decompose(matrix)
        assertTrue(result.isValid)
        val sorted = result.values.sorted()
        assertEquals(4.0, sorted[0], 1.0e-9)
        assertEquals(9.0, sorted[1], 1.0e-9)
    }

    @Test
    fun cmaEsShouldConvergeOnAQuadratic(): Unit = runBlocking {
        val model = cmaModel()
        val baseline = cmaStartingObjective(model)
        val solver = cmaEs<Flt64, Flt64>(
            stopping = IterationLimit(UInt64(80)),
            converter = IntoValue.Identity,
            entropy = CmaEntropy(211L)
        )

        when (val result = solver(model)) {
            is Ok -> {
                assertEquals("cma-es", solver.name)
                val best = result.value.bestObjective
                assertNotNull(best)
                assertTrue(best.toDouble().isFinite(), "CMA-ES returned a non-finite objective")
                assertTrue(
                    best.toDouble() <= baseline + 1.0e-9,
                    "CMA-ES worsened the objective: ${best.toDouble()} > $baseline"
                )
                assertEquals(2, result.value.bestSolution!!.size)
                assertEquals(HeuristicSolutionStatus.Feasible, result.value.status)
            }

            else -> fail("CMA-ES failed: $result")
        }
    }
}
