/**
 * 粒子群优化 / Particle Swarm Optimization
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ConstantSource
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 粒子群优化，即 `ParticleSwarmRunner` 骨架。 / Particle swarm optimization: the `ParticleSwarmRunner` skeleton.
 *
 * 全部参数与 `ComposedSolvers.particleSwarm` 一一对应，只是少了 `ComposedSolvers.` 前缀。 /
 * Every parameter mirrors `ComposedSolvers.particleSwarm`; only the `ComposedSolvers.` prefix is dropped.
 *
 * 三个权重都用 [ParameterSource] 表达，因此「固定权重」与「随迭代变化的权重」是同一份代码的两种配置；
 * 后者的现成门面是 [adaptiveParticleSwarm]。 / All three weights are parameter sources, so "constant weight"
 * and "time-varying weight" are two configurations of the same code; [adaptiveParticleSwarm] is the ready-made
 * facade for the latter.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param size 粒子数量 / Particle amount
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param inertia 惯性权重源 / Inertia weight source
 * @param personalWeight 个体学习因子源 / Personal learning factor source
 * @param socialWeight 社会学习因子源 / Social learning factor source
 * @param maxVelocity 最大速度 / Maximum velocity
 * @param archiveAmount 档案容量 / Archive capacity
 * @param initializer 初始化算子 / Initialization operator
 * @return 求解器 / Solver
*/
fun <ObjValue, V> particleSwarm(
    size: Int,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    inertia: ParameterSource = ConstantSource(Flt64(0.4)),
    personalWeight: ParameterSource = ConstantSource(Flt64(2.0)),
    socialWeight: ParameterSource = ConstantSource(Flt64(2.0)),
    maxVelocity: Flt64 = Flt64(10000),
    archiveAmount: Int = 1,
    initializer: Initializer = ModelInitializer
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.particleSwarm(
        size = size,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        inertia = inertia,
        personalWeight = personalWeight,
        socialWeight = socialWeight,
        maxVelocity = maxVelocity,
        archiveAmount = archiveAmount,
        initializer = initializer
    )
