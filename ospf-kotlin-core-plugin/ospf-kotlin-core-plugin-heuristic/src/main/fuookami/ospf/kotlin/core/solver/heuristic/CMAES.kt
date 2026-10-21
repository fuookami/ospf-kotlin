/**
 * 协方差矩阵自适应演化策略 / Covariance Matrix Adaptation Evolution Strategy
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 协方差矩阵自适应演化策略，即 `CmaEsRunner` 骨架。 / CMA-ES: the distribution-state skeleton `CmaEsRunner`.
 *
 * 全部参数与 `ComposedSolvers.cmaEs` 一一对应，只是少了 `ComposedSolvers.` 前缀。 /
 * Every parameter mirrors `ComposedSolvers.cmaEs`; only the `ComposedSolvers.` prefix is dropped.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param initialSigma 初始步长 / Initial step size
 * @param archiveAmount 档案容量 / Archive capacity
 * @return 求解器 / Solver
*/
fun <ObjValue, V> cmaEs(
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    initialSigma: Double = 0.3,
    archiveAmount: Int = 1
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.cmaEs(
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        initialSigma = initialSigma,
        archiveAmount = archiveAmount
    )
