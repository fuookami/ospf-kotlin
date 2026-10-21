/**
 * Tier-2 位置更新算子（第二批）/ Tier-2 position update operators (second batch)
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
 * 教与学优化的「教师阶段」（TLBO）。 / The teacher phase of teaching-learning-based optimization (TLBO).
 *
 * X' = X + r·(X_teacher − T_F·M)
 *
 * 其中 `X_teacher` 是当前最优（父代已按优度升序），`M` 是种群均值，`T_F = 1 + round(r)` 是教学因子，
 * 表示教师能把班级均值拉向自己多少。 / Here `X_teacher` is the current best (the parents are ranked by ascending quality), `M` is the population mean, and
 * `T_F = 1 + round(r)` is the teaching factor describing how far the teacher can pull the class mean towards itself.
 *
 * TLBO 的「学生阶段」（两两个体互学）需要比较两个体的优度，而解更新契约看不到目标值；
 * 由于骨架保证父代按优度升序，学生阶段可用下标代替优度比较，故另立 [LearnerPhaseUpdate]。 /
 * The learner phase of TLBO needs to compare two individuals, which the update contract cannot do because it cannot see objective
 * values. Since the skeleton guarantees ascending quality, the learner phase can use indices instead, so it lives in
 * [LearnerPhaseUpdate] separately.
 *
 * @property teachingFactorRange 教学因子的上界 / Upper bound of the teaching factor
*/
data class TeachingLearningUpdate(
    val teachingFactorRange: Double = 2.0
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

        val teacher = parents.first().solution
        val dimension = teacher.size
        val mean = DoubleArray(dimension)
        for (parent in parents) {
            for (dim in 0 until dimension) {
                mean[dim] += parent.solution[dim].asDouble()
            }
        }
        for (dim in 0 until dimension) {
            mean[dim] /= parents.size.toDouble()
        }

        return parents.map { parent ->
            val current = parent.solution
            val factor = 1.0 + Math.round(entropy.unit() * teachingFactorRange).toDouble()
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val step = entropy.unit() *
                    (teacher[dim].asDouble() - factor * mean[dim])
                moved.add(clampTo(current[dim].asDouble() + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 教与学优化的「学生阶段」（TLBO）。 / The learner phase of teaching-learning-based optimization (TLBO).
 *
 * 学生随机找一个同学互学：比自己差就向自己学习，比自己好就向对方学习。 / A learner picks a classmate at random: it learns from itself when the classmate is worse, and from the classmate when the
 * classmate is better.
 *
 * - 自身排名更好：X' = X + r·(X − X_partner)
 *   Better ranked: X' = X + r·(X − X_partner)
 * - 否则：X' = X + r·(X_partner − X)
 *   Otherwise: X' = X + r·(X_partner − X)
 *
 * 优劣判断直接用**下标**：骨架保证父代按优度升序，因此下标更小即更优。 / Quality is compared by **index**: the skeleton guarantees ascending quality, so a smaller index is better.
*/
data object LearnerPhaseUpdate : SolutionUpdate {
    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        if (parents.size < 2) {
            return parents.map { it.solution.toList() }
        }

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            var partnerIndex = (entropy.unit() * parents.size).toInt().coerceIn(0, parents.size - 1)
            if (partnerIndex == index) {
                partnerIndex = (index + 1) % parents.size
            }
            val partner = parents[partnerIndex].solution
            val sign = if (index < partnerIndex) 1.0 else -1.0

            val dimension = current.size
            val moved = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val step = entropy.unit() * sign *
                    (partner[dim].asDouble() - current[dim].asDouble())
                moved.add(clampTo(current[dim].asDouble() + step, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 樽海鞘群算法（SSA）。 / Salp swarm algorithm (SSA).
 *
 * 链首是领导者，其余是跟随者。领导者围绕最优解在变量域内摆动，跟随者向自己的前一个个体靠拢： / The chain has one leader and the rest are followers. The leader swings around the best solution within the variable domain, and
 * each follower moves towards the individual in front of it:
 *
 * - 领导者：X' = X_best + c₁·((UB − LB)·c₂ + LB)，`c₃ < 0.5` 时取负号
 *   leader: X' = X_best + c₁·((UB − LB)·c₂ + LB), negated when `c₃ < 0.5`
 * - 跟随者：X' = (X + X_previous) / 2
 *   follower: X' = (X + X_previous) / 2
 *
 * `c₁ = 2·e^{−(4t/T)²}` 从 2 衰减到接近 0，使链首由大幅摆动逐步收敛到微调。 /
 * `c₁ = 2·e^{−(4t/T)²}` decays from 2 towards 0, gradually turning the leader's large swings into fine adjustments.
 *
 * @property decay 控制参数 c₁ 的演化源 / Evolution source of the control parameter c₁
*/
data class SalpSwarmUpdate(
    val decay: ParameterSource = LinearSource(Flt64.two, Flt64.zero, UInt64(100))
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

        val leader = parents.first().solution
        val coefficient = decay(iteration).toDouble()

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val origin = if (lower != null && lower.isFinite()) lower else 0.0

                val value = if (index == 0) {
                    val swing = entropy.unit()
                    val sign = if (entropy.unit() < 0.5) -1.0 else 1.0
                    leader[dim].asDouble() + sign * coefficient * (width * swing + origin)
                } else {
                    (current[dim].asDouble() + parents[index - 1].solution[dim].asDouble()) / 2.0
                }
                moved.add(clampTo(value, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 引力搜索算法（GSA）。 / Gravitational search algorithm (GSA).
 *
 * 每个个体的质量与其优度成正比，个体之间按万有引力互相吸引： / Each individual's mass is proportional to its quality, and individuals attract each other through gravity:
 *
 * a_i = G·Σ_j m_j·(X_j − X_i) / (r_ij + ε)
 * X' = X + a_i
 *
 * 由于解更新契约看不到目标值，质量用**排名**近似：`m_i ∝ (n − rank_i)`。排名是优度的单调变换，
 * 因此这与「按优度归一化质量」等价。原文的加速度需要速度累积，这里直接作用于位置，等价于单位时间步长。 /
 * Because the update contract cannot see objective values, mass is approximated by **rank**: `m_i ∝ (n − rank_i)`. Rank is a
 * monotone transform of quality, so this is equivalent to normalising mass by quality. The original accumulates velocity, whereas
 * here acceleration acts directly on the position, which is a unit time step.
 *
 * @property gravitationalConstant 引力常数 G 的演化源 / Evolution source of the gravitational constant G
 * @property softness 距离软化的 ε / Softening ε of the distance
*/
data class GravitationalSearchUpdate(
    val gravitationalConstant: ParameterSource = LinearSource(Flt64.one, Flt64(0.01), UInt64(100)),
    val softness: Double = 1.0e-6
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

        val size = parents.size
        val constant = gravitationalConstant(iteration).toDouble()

        // 排名越靠前质量越大；归一化到总和为 1。
        // Better ranks carry larger mass; masses are normalised to sum to 1.
        val massTotal = (size * (size + 1) / 2).toDouble()
        val masses = List(size) { index -> (size - index).toDouble() / massTotal }

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            for (dim in 0 until dimension) {
                var acceleration = 0.0
                for (other in 0 until size) {
                    if (other == index) {
                        continue
                    }
                    var distanceSquared = 0.0
                    for (axis in 0 until dimension) {
                        val delta = parents[other].solution[axis].asDouble() - current[axis].asDouble()
                        distanceSquared += delta * delta
                    }
                    val distance = sqrt(distanceSquared) + softness
                    acceleration += constant * masses[other] *
                        (parents[other].solution[dim].asDouble() - current[dim].asDouble()) / distance
                }
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                moved.add(clampTo(current[dim].asDouble() + acceleration, lower, upper).asValue(converter))
            }
            moved
        }
    }
}

/**
 * 萤火虫算法（FA）。 / Firefly algorithm (FA).
 *
 * 萤火虫被更亮的同伴吸引，吸引力随距离按指数衰减： / A firefly is attracted to brighter companions, and the attraction decays exponentially with distance:
 *
 * β = β₀·e^{−γ·r²}
 * X' = X + β·(X_bright − X) + α·(rand − 0.5)
 *
 * 亮度即优度；契约看不到目标值，因此「更亮」用**排名**判定：向任一排名更靠前的同伴移动。
 * 随机项 `α·(rand − 0.5)` 提供无方向的抖动，避免全群塌缩到同一点。 /
 * Brightness is quality; the contract cannot see objective values, so "brighter" is decided by **rank**: the firefly moves towards a
 * randomly chosen better-ranked companion. The random term `α·(rand − 0.5)` supplies an undirected jitter and prevents the whole
 * swarm from collapsing onto one point.
 *
 * @property attractiveness 初始吸引力 β₀ / Initial attractiveness β₀
 * @property absorption 光吸收系数 γ / Light absorption coefficient γ
 * @property randomness 随机项系数 α / Randomness coefficient α
*/
data class FireflyUpdate(
    val attractiveness: Double = 1.0,
    val absorption: Double = 1.0,
    val randomness: Double = 0.2
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

        return parents.mapIndexed { index, parent ->
            val current = parent.solution
            val dimension = current.size
            val moved = ArrayList<V>(dimension)

            // 排名更靠前的候选同伴；自己已是最优时不移动，只做随机抖动。
            // Candidates with a better rank; the best firefly only jitters.
            val betterAmount = index
            val partnerIndex = if (betterAmount > 0) {
                (entropy.unit() * betterAmount).toInt().coerceIn(0, betterAmount - 1)
            } else {
                index
            }
            val partner = parents[partnerIndex].solution

            var distanceSquared = 0.0
            for (dim in 0 until dimension) {
                val delta = partner[dim].asDouble() - current[dim].asDouble()
                distanceSquared += delta * delta
            }
            val attraction = attractiveness * exp(-absorption * distanceSquared)

            for (dim in 0 until dimension) {
                val lower = model.lowerBoundOf(dim)
                val upper = model.upperBoundOf(dim)
                val width = if (lower != null && upper != null && lower.isFinite() && upper.isFinite()) {
                    upper - lower
                } else {
                    1.0
                }
                val position = current[dim].asDouble()
                val pull = attraction * (partner[dim].asDouble() - position)
                val jitter = randomness * (entropy.unit() - 0.5) * width
                moved.add(clampTo(position + pull + jitter, lower, upper).asValue(converter))
            }
            moved
        }
    }
}
