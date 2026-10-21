/**
 * 综合学习粒子群优化 / Comprehensive Learning Particle Swarm Optimization
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * CLPSO 的超参数。 / Hyper-parameters of CLPSO.
 *
 * CLPSO 的差别在于每个维度独立向不同个体的最优分量学习，因此多了 [refreshInterval] 与
 * [stagnationLimit] 两个调度参数。 / CLPSO learns each dimension from a different exemplar, so it
 * adds two scheduling knobs: refresh interval and stagnation limit.
 *
 * @property inertia 惯性权重调度 / Inertia weight schedule
 * @property learningWeight 学习因子 / Learning factor
 * @property maxVelocity 最大速度 / Maximum velocity
 * @property refreshInterval 例子的刷新间隔 / Exemplar refresh interval
 * @property stagnationLimit 触发刷新的停滞代数 / Stagnation rounds that trigger a refresh
 * @property archiveAmount 档案容量 / Archive capacity
 * @property initializer 初始化算子 / Initialization operator
*/
data class ComprehensiveLearningParticleSwarmPolicy(
    val inertia: ParameterSource = LinearSource(Flt64(0.9), Flt64(0.4), UInt64(100)),
    val learningWeight: Flt64 = Flt64(1.49445),
    val maxVelocity: Flt64 = Flt64(10000),
    val refreshInterval: Int = 7,
    val stagnationLimit: Int = 5,
    val archiveAmount: Int = 1,
    val initializer: Initializer = ModelInitializer
)

/**
 * 综合学习粒子群优化，即 `ClpsoRunner` 骨架。 / Comprehensive learning PSO: the `ClpsoRunner` skeleton.
 *
 * 全部可调项集中在 [policy] 里。 / Every tunable lives in [policy].
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param size 种群规模 / Population size
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param policy CLPSO 超参数 / CLPSO hyper-parameters
 * @return 求解器 / Solver
*/
fun <ObjValue, V> comprehensiveLearningParticleSwarm(
    size: Int,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    policy: ComprehensiveLearningParticleSwarmPolicy = ComprehensiveLearningParticleSwarmPolicy()
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.comprehensiveLearningParticleSwarm(
        size = size,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        inertia = policy.inertia,
        learningWeight = policy.learningWeight,
        maxVelocity = policy.maxVelocity,
        refreshInterval = policy.refreshInterval,
        stagnationLimit = policy.stagnationLimit,
        archiveAmount = policy.archiveAmount,
        initializer = policy.initializer
    )
