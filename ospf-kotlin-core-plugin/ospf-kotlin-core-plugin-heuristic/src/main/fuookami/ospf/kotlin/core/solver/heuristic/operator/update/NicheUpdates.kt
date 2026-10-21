/**
 * 细分启发式位置更新算子 / Niche heuristic position update operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 战争策略优化（WarSO）。 / War strategy optimization (WarSO).
 *
 * 把每个个体视为士兵，按优度决定「进攻」还是「防守」： / Every individual is treated as a soldier, and its quality decides whether it attacks or defends:
 *
 * - 进攻（靠近最优）：X' = X + 2·r·(X_best − X)
 *   attack (closing on the best): X' = X + 2·r·(X_best − X)
 * - 防守（向随机战友靠拢以维持阵型）：X' = X + 2·r·(X_random − X)
 *   defence (regrouping towards a random comrade): X' = X + 2·r·(X_random − X)
 *
 * 排名靠前的士兵以 [attackRate] 概率进攻，其余按防守移动，由此同时维持收敛压力与阵型多样性。 /
 * Better-ranked soldiers attack with probability [attackRate] while the rest regroup, which keeps both convergence pressure and
 * formation diversity.
 *
 * @property attackRate 进攻概率 / Attack probability
 * @property strategyScale 策略步长系数 / Strategy step scale
*/
data class WarStrategyUpdate(
    val attackRate: Double = 0.6,
    val strategyScale: Double = 2.0
) : SolutionUpdate {

    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        val best = parents.firstOrNull()?.solution ?: return emptyList()

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val attacking = entropy.unit() < attackRate
            val target = if (attacking) {
                best
            } else {
                val comrade = (entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)
                parents[comrade].solution
            }

            val dimension = current.size
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                // 进攻对「向最优收缩」做 2 倍放大是原文的设定，收敛是安全的；但防守的目标是随机战友，
                // 对随机目标做 2 倍外推会让整代种群互相反馈、模长逐代放大，因此防守只取 1 倍步长，保持收缩。
                // The 2x amplification of the attack step towards the best is the original formulation and converges safely;
                // the defence target is a random comrade, and extrapolating 2x on a random target makes the whole generation
                // feed back into itself with a growing magnitude, so defence uses a plain 1x step and stays contracting.
                val scale = if (attacking) strategyScale else 1.0
                val step = scale * entropy.unit() *
                    (target[dim].asDouble() - current[dim].asDouble())
                moved.add(clampTo(current[dim].asDouble() + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 能量谷优化（EVO）。 / Energy valley optimizer (EVO).
 *
 * 借粒子物理的稳定性类比：排名靠前的个体处于「稳定态」，围绕最优解做小幅度振荡；
 * 其余个体「不稳定」，通过衰变跳向其他位置以获得新的能量谷。 / A particle-physics analogy is used: better-ranked individuals are "stable" and oscillate with a small amplitude around the
 * best solution, while the rest are "unstable" and decay towards other positions to reach a new energy valley.
 *
 * - 稳定：X' = X + r·(X_best − X)
 *   stable: X' = X + r·(X_best − X)
 * - 不稳定（衰变）：X' = X + r·(X_random − X)
 *   unstable (decay): X' = X + r·(X_random − X)
 *
 * @property stabilityRate 稳定态比例 / Proportion of the population treated as stable
 * @property decayScale 衰变步长系数 / Decay step scale
*/
data class EnergyValleyUpdate(
    val stabilityRate: Double = 0.5,
    val decayScale: Double = 1.0
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
        val stableAmount = (parents.size * stabilityRate).toInt().coerceIn(1, parents.size)

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val stable = index < stableAmount
            val target = if (stable) {
                best
            } else {
                val other = (entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)
                parents[other].solution
            }
            val scale = if (stable) 1.0 else decayScale

            val dimension = current.size
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val step = scale * entropy.unit() *
                    (target[dim].asDouble() - current[dim].asDouble())
                moved.add(clampTo(current[dim].asDouble() + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 生发中心优化（GCO）。 / Germinal center optimization (GCO).
 *
 * 借鉴免疫系统的亲和成熟：精英个体构成生发中心，其余个体向中心的**质心**靠拢并叠加一次随机突变，
 * 目的是让种群在中心附近精细搜索而不是全部塌缩到同一个点。 / Affinity maturation from the immune system is the inspiration: the elite individuals form the germinal centre, and the rest
 * contract towards the **centroid** of that centre with one random mutation on top. The goal is fine-grained search around the
 * centre rather than collapsing every individual onto the same point.
 *
 * X' = X + r·(centroid − X) + mutationScale·(2r − 1)·width
 *
 * @property eliteAmount 生发中心规模 / Germinal centre size
 * @property mutationScale 突变幅度，相对于变量取值范围 / Mutation magnitude relative to the variable range
*/
data class GerminalCenterUpdate(
    val eliteAmount: Int = 3,
    val mutationScale: Double = 0.1
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

        val eliteCount = eliteAmount.coerceIn(1, parents.size)
        val dimension = parents.first().solution.size
        val centroid = DoubleArray(dimension)
        for (index in 0 until eliteCount) {
            val solution = parents[index].solution
            for (dim in 0 until dimension) {
                centroid[dim] += solution[dim].asDouble()
            }
        }
        for (dim in 0 until dimension) {
            centroid[dim] /= eliteCount.toDouble()
        }

        return parents.map { parent ->
            val current = parent.solution
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val position = current[dim].asDouble()
                val contraction = entropy.unit() * (centroid[dim] - position)
                val mutation = mutationScale * (entropy.unit() * 2.0 - 1.0) * width
                moved.add(clampTo(position + contraction + mutation, lower, upper).asValue(converter))
            }
            moved
        }
    }
}
