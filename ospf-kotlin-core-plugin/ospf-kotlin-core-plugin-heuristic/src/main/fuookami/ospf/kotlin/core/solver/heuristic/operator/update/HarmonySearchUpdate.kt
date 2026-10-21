/**
 * 和声搜索位置更新算子 / Harmony search position update operator
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 和声搜索（HS）。 / Harmony search (HS).
 *
 * 逐个维度决定新和声的分量来源： / The source of each component of a new harmony is decided per dimension:
 *
 * - 以 HMCR 概率从记忆库（现有和声）中取一个分量，否则在整个取值域内随机生成；
 *   with probability HMCR take the component from the harmony memory, otherwise generate it uniformly over the domain;
 * - 取自分量再以 PAR 概率做微调：以带宽 `bw` 为幅度向上或向下扰动。
 *   a borrowed component is then pitch-adjusted with probability PAR by a bandwidth-sized step up or down.
 *
 * HMCR 控制「利用」，PAR 控制「局部扰动强度」，二者共同决定探索与开发的比例。 /
 * HMCR controls exploitation and PAR controls the strength of local perturbation; together they set the exploration/exploitation
 * balance.
 *
 * @property memoryConsiderationRate 和声记忆库取用率 HMCR / Harmony memory consideration rate
 * @property pitchAdjustmentRate 音高调整率 PAR / Pitch adjustment rate
 * @property bandwidth 音高调整带宽，相对于变量取值范围 / Pitch adjustment bandwidth relative to the variable range
*/
data class HarmonySearchUpdate(
    val memoryConsiderationRate: Double = 0.9,
    val pitchAdjustmentRate: Double = 0.3,
    val bandwidth: Double = 0.01
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

        return parents.map { parent ->
            val dimension = parent.solution.size
            val child = ArrayList<V>(dimension)

            for (index in 0 until dimension) {
                val lower = model.lowerBoundOf(index)
                val upper = model.upperBoundOf(index)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }

                val value = if (entropy.unit() < memoryConsiderationRate) {
                    // 记忆库取用：从现有和声中随机取一个分量。
                    // Memory consideration: borrow a component from a random harmony.
                    val donor = parents[(entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)]
                    var borrowed = donor.solution[index].asDouble()
                    if (entropy.unit() < pitchAdjustmentRate) {
                        val direction = if (entropy.unit() < 0.5) -1.0 else 1.0
                        borrowed += direction * bandwidth * width
                    }
                    borrowed
                } else {
                    // 随机生成：在整个取值域内均匀采样。
                    // Random generation: sample uniformly over the whole domain.
                    val origin = if (lower != null && lower.isFinite()) lower else 0.0
                    origin + entropy.unit() * width
                }

                child.add(clampTo(value, lower, upper).asValue(converter))
            }
            child
        }
    }
}
