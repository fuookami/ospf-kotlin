/**
 * 灰狼位置更新算子 / Grey wolf position update operator
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 灰狼位置更新（GWO）。 / Grey wolf position update (GWO).
 *
 * 三级领导层次：按优度取前 [leaderAmount] 名作为 α / β / δ，每个个体被三匹头狼共同牵引： / Three-level leadership: the best [leaderAmount] individuals act as α / β / δ, and every wolf is pulled by all three leaders:
 *
 * X' = (1/k) Σ (X_leader − A · |C · X_leader − X|)
 * A = 2a·r₁ − a,  C = 2r₂
 *
 * 其中 a 从 2 线性衰减到 0，使搜索由全局探索平滑过渡到局部开发。 /
 * Here a decays linearly from 2 to 0, smoothly shifting the search from global exploration to local exploitation.
 *
 * @property coefficient 收敛系数 a 的演化源 / Evolution source of the convergence coefficient a
 * @property leaderAmount 头狼数量 / Number of leaders
*/
data class GreyWolfUpdate(
    val coefficient: ParameterSource = LinearSource(Flt64(2.0), Flt64.zero, UInt64(100)),
    val leaderAmount: Int = 3
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        if (parents.isEmpty()) {
            return emptyList()
        }

        // 种群骨架传入的父代组已按优度升序排列，因此前三名即 α / β / δ。
        // The parent group handed over by the population skeleton is already ranked by ascending quality, so the first three are α / β / δ.
        val leaders = parents
            .take(min(max(leaderAmount, 1), parents.size))
            .map { it.solution }
        val convergence = coefficient(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val dimension = current.size
            val accumulated = DoubleArray(dimension)

            for (leader in leaders) {
                val firstRandom = entropy.unit()
                val secondRandom = entropy.unit()
                val a = 2.0 * convergence * firstRandom - convergence
                val c = 2.0 * secondRandom
                for (index in 0 until dimension) {
                    val target = leader[index].asDouble()
                    accumulated[index] += target - a * abs(c * target - current[index].asDouble())
                }
            }

            val scale = leaders.size.toDouble()
            val child = ArrayList<V>(dimension)
            for (index in 0 until dimension) {
                val lower = model.lowerBoundOf(index)
                val upper = model.upperBoundOf(index)
                child.add(clampTo(accumulated[index] / scale, lower, upper).asValue(converter))
            }
            child
        }
    }
}
