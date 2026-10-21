/**
 * 自适应粒子群优化 / Adaptive Particle Swarm Optimization
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
 * APSO 的超参数。 / Hyper-parameters of APSO.
 *
 * 三个权重都用 [ParameterSource] 表达，因此「随迭代递减」只是一次调度配置，而不是另一个算法。 /
 * All three weights are parameter sources, so "decreasing over time" is a schedule configuration
 * rather than a different algorithm.
 *
 * @property inertia 惯性权重调度 / Inertia weight schedule
 * @property personalWeight 个体学习因子调度 / Personal learning factor schedule
 * @property socialWeight 社会学习因子调度 / Social learning factor schedule
 * @property maxVelocity 最大速度 / Maximum velocity
 * @property archiveAmount 档案容量 / Archive capacity
 * @property initializer 初始化算子 / Initialization operator
*/
data class AdaptiveParticleSwarmPolicy(
    val inertia: ParameterSource = LinearSource(Flt64(0.9), Flt64(0.4), UInt64(100)),
    val personalWeight: ParameterSource = LinearSource(Flt64(2.5), Flt64(0.5), UInt64(100)),
    val socialWeight: ParameterSource = LinearSource(Flt64(0.5), Flt64(2.5), UInt64(100)),
    val maxVelocity: Flt64 = Flt64(10000),
    val archiveAmount: Int = 1,
    val initializer: Initializer = ModelInitializer
)

/**
 * 自适应粒子群优化，即 `ParticleSwarmRunner` + 随时间变化的权重。 /
 * Adaptive PSO: `ParticleSwarmRunner` with time-varying weights.
 *
 * 全部可调项集中在 [policy] 里。 / Every tunable lives in [policy].
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param size 种群规模 / Population size
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param policy APSO 超参数 / APSO hyper-parameters
 * @return 求解器 / Solver
*/
fun <ObjValue, V> adaptiveParticleSwarm(
    size: Int,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    policy: AdaptiveParticleSwarmPolicy = AdaptiveParticleSwarmPolicy()
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.adaptiveParticleSwarm(
        size = size,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        inertia = policy.inertia,
        personalWeight = policy.personalWeight,
        socialWeight = policy.socialWeight,
        maxVelocity = policy.maxVelocity,
        archiveAmount = policy.archiveAmount,
        initializer = policy.initializer
    )
