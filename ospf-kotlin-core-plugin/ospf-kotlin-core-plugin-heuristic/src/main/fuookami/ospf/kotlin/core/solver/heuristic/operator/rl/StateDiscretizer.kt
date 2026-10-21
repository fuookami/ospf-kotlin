/**
 * 状态离散化算子 / State discretization operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual

/**
 * 状态离散化算子契约：把连续搜索态势压成一个离散状态下标。 / State discretization contract compressing the continuous search situation into a discrete state index.
 *
 * Q 学习只能在有限状态上建表，而搜索态势是连续的，因此这一步是强化学习进入元启发式的必要环节。 /
 * Q-learning can only tabulate over finite states while the search situation is continuous, so this step is
 * what lets reinforcement learning enter a metaheuristic at all.
*/
interface StateDiscretizer {

    /** 离散状态总数 / Total number of discrete states */
    val stateAmount: Int

    /**
     * 计算离散状态 / Compute the discrete state
     *
     * @param V 值类型 / Value type
     * @param iteration 当前迭代 / Current iteration
     * @param members 本轮成员 / Members of this round
     * @param converter 值转换器 / Value converter
     * @return 状态下标，位于 `[0, stateAmount)` / State index in `[0, stateAmount)`
    */
    operator fun <V> invoke(
        iteration: Iteration,
        members: List<Individual<*, V>>,
        converter: IntoValue<V>
    ): Int where V : RealNumber<V>, V : NumberField<V>
}

/**
 * 以「多样性 × 停滞度」为两轴的离散化。 / Discretization over the two axes "diversity × stagnation".
 *
 * 两轴都不需要目标值，因此不必知道最优个体是谁，也就不必把 `ObjValue` 提升为类型参数： /
 * Neither axis needs the objective value, so the best individual need not be identified and `ObjValue` need not
 * become a type parameter:
 *
 * - **多样性**：平均成对距离除以最大成对距离，落在 [0, 1]。高多样性说明还在探索，低多样性说明已聚拢。 /
 *   **Diversity**: mean pairwise distance divided by the maximum pairwise distance, in [0, 1]. High values mean the
 *   population is still exploring; low values mean it has collapsed.
 * - **停滞度**：无改进迭代数除以总迭代数，落在 [0, 1]。零表示上一轮刚刚改进，一表示从未改进。 /
 *   **Stagnation**: no-improvement iterations divided by total iterations, in [0, 1]. Zero means the last round
 *   improved; one means it never improved.
 *
 * 旧实现用的是「密度 + 到最优个体的距离」，后者需要先确定最优个体，且在目标值不可比较时无从下手；
 * 换成停滞度以后，两轴都只依赖骨架已有的信息。 / The legacy version used "density + distance to the best
 * individual"; the latter requires knowing which individual is best, which is ambiguous when objectives are not
 * comparable. With stagnation, both axes rely only on information the skeleton already has.
 *
 * @property levels 每个轴的层级数 / Number of levels per axis
*/
data class DiversityStagnationDiscretizer(
    val levels: Int = 3
) : StateDiscretizer {

    override val stateAmount: Int get() = if (levels < 1) 0 else levels * levels

    override fun <V> invoke(
        iteration: Iteration,
        members: List<Individual<*, V>>,
        converter: IntoValue<V>
    ): Int where V : RealNumber<V>, V : NumberField<V> {
        if (levels < 2 || members.size < 2) {
            return 0
        }
        val dimension = members.first().solution.size
        if (dimension == 0) {
            return 0
        }

        var accumulated = 0.0
        var maximum = 0.0
        var pairs = 0L
        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                var squared = 0.0
                for (k in 0 until dimension) {
                    val difference = converter.fromValue(members[i].solution[k]).toDouble() -
                        converter.fromValue(members[j].solution[k]).toDouble()
                    squared += difference * difference
                }
                val distance = sqrt(squared)
                accumulated += distance
                if (distance > maximum) {
                    maximum = distance
                }
                ++pairs
            }
        }

        val diversity = if (pairs == 0L || maximum <= 0.0) {
            0.0
        } else {
            (accumulated / pairs.toDouble()) / maximum
        }

        val total = iteration.iteration.toFlt64().toDouble()
        val stagnation = if (total <= 0.0) {
            0.0
        } else {
            iteration.notBetterIteration.toFlt64().toDouble() / total
        }

        return levelOf(diversity) * levels + levelOf(stagnation)
    }

    /**
     * 把 [0, 1] 的连续量映射到层级 / Map a value in [0, 1] to a level
     *
     * @param value 连续量 / Continuous value
     * @return 层级下标 / Level index
    */
    private fun levelOf(value: Double): Int {
        val width = 1.0 / levels.toDouble()
        return (value / width).toInt().coerceIn(0, levels - 1)
    }
}
