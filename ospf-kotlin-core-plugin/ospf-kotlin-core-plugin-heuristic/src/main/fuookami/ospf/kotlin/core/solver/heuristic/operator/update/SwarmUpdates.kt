/**
 * 正弦余弦位置更新算子 / Sine cosine position update operator
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
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 正弦余弦位置更新（SCA）。 / Sine cosine position update (SCA).
 *
 * 逐维用正弦或余弦把个体拉向（或推离）当前最优解： / Every dimension moves towards (or away from) the best solution through a sine or cosine wave:
 *
 * X' = X + r₁ · sin(r₂) · |r₃ · X_best − X|   (r₄ < 0.5)
 * X' = X + r₁ · cos(r₂) · |r₃ · X_best − X|   (otherwise)
 *
 * r₁ 从 2 衰减到 0 控制探索/开发切换，r₂ ∈ [0, 2π] 决定方向，r₃ ∈ [0, 2] 决定对最优解的拉伸。 /
 * r₁ decaying from 2 to 0 drives the exploration/exploitation shift, r₂ ∈ [0, 2π] sets the direction, and r₃ ∈ [0, 2]
 * sets the stretch relative to the best solution.
 *
 * @property amplitude r₁ 的演化源 / Evolution source of r₁
*/
data class SineCosineUpdate(
    val amplitude: ParameterSource = LinearSource(Flt64(2.0), Flt64.zero, UInt64(100))
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        // 种群骨架传入的父代组已按优度升序排列，因此第一个就是当前最优。
        // The parent group is ranked by ascending quality, so the first element is the current best.
        val best = parents.firstOrNull()?.solution ?: return emptyList()
        val r1 = amplitude(iteration).toDouble()

        return parents.map { parent ->
            val current = parent.solution
            val child = ArrayList<V>(current.size)
            for (index in current.indices) {
                val r2 = entropy.unit() * 2.0 * PI
                val r3 = entropy.unit() * 2.0
                val r4 = entropy.unit()
                val position = current[index].asDouble()
                val target = best[index].asDouble()
                val wave = if (r4 < 0.5) sin(r2) else cos(r2)
                val moved = position + r1 * wave * abs(r3 * target - position)
                child.add(
                    clampTo(moved, model.lowerBoundOf(index), model.upperBoundOf(index)).asValue(converter)
                )
            }
            child
        }
    }
}

/**
 * 多元宇宙位置更新（MVO）。 / Multi-verse position update (MVO).
 *
 * 每个宇宙按虫洞存在概率 WEP 决定「穿越到最优宇宙附近」还是「通过白洞获取更优宇宙的物质」： / Each universe either traverses towards the best universe according to the wormhole existence probability (WEP),
 * or acquires matter from a better universe through a white hole:
 *
 * X' = X_best ± TDR · ((UB − LB) · r₄ + LB)              (r₂ < WEP)
 * X' = X_donor                                          (r₂ < 归一化膨胀率)
 * X' = X                                                (otherwise)
 *
 * 由于解更新契约看不到目标值，白洞捐赠者按**排名加权**抽取：排名越靠前越可能被选中，
 * 而排名本身就是膨胀率的单调变换，因此这与「按膨胀率轮盘赌」等价。 /
 * The update contract cannot see objective values, so the white-hole donor is drawn with **rank weights**: earlier ranks are
 * more likely to be chosen, and rank is a monotone transform of the inflation rate, which makes this equivalent to roulette
 * selection over inflation rates.
 *
 * @property wormholeRate 虫洞存在概率 WEP 的演化源 / Evolution source of the wormhole existence probability
 * @property travellingRate 旅行距离率 TDR 的演化源 / Evolution source of the travelling distance rate
*/
data class MultiVerseUpdate(
    val wormholeRate: ParameterSource = LinearSource(Flt64(0.2), Flt64.one, UInt64(100)),
    val travellingRate: ParameterSource = LinearSource(Flt64.one, Flt64.zero, UInt64(100))
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

        val best = parents.first().solution
        val size = parents.size
        val wormhole = wormholeRate(iteration).toDouble()
        val travelling = travellingRate(iteration).toDouble()

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val inflation = (size - index).toDouble() / size.toDouble()
            val child = ArrayList<V>(current.size)

            for (dimension in current.indices) {
                val lower = model.lowerBoundOf(dimension)
                val upper = model.upperBoundOf(dimension)
                val second = entropy.unit()
                val third = entropy.unit()
                val fourth = entropy.unit()

                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val origin = if (lower != null && lower.isFinite()) lower else 0.0

                val value = when {
                    second < wormhole -> {
                        val offset = travelling * (width * fourth + origin)
                        if (third < 0.5) {
                            best[dimension].asDouble() + offset
                        } else {
                            best[dimension].asDouble() - offset
                        }
                    }

                    second < inflation -> {
                        parents[pickDonor(size, entropy)].solution[dimension].asDouble()
                    }

                    else -> current[dimension].asDouble()
                }
                child.add(clampTo(value, lower, upper).asValue(converter))
            }
            child
        }
    }

    /**
     * 按排名加权抽取白洞捐赠者 / Draw a white-hole donor weighted by rank
     *
     * @param size 宇宙数量 / Universe amount
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 捐赠者下标 / Donor index
    */
    private fun pickDonor(size: Int, entropy: Generator<Flt64>): Int {
        if (size <= 1) {
            return 0
        }
        val total = size * (size + 1) / 2.0
        var remaining = entropy.unit() * total
        for (rank in 0 until size) {
            val weight = (size - rank).toDouble()
            if (remaining < weight) {
                return rank
            }
            remaining -= weight
        }
        return 0
    }
}
