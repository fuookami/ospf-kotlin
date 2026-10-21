/**
 * 带线性种群缩减的 SHADE / SHADE with Linear Population Size Reduction
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * L-SHADE 的超参数。 / Hyper-parameters of L-SHADE.
 *
 * 这五个参数都只在 SHADE 内部有意义，集中成一个策略对象后，门面签名从 9 个参数降到 5 个。 /
 * All five are meaningful only inside SHADE; grouping them drops the facade signature from nine
 * parameters to five.
 *
 * @property minimumSize 种群规模下限 / Lower bound of the population size
 * @property sizeRatio 种群规模缩减调度 / Population-size reduction schedule
 * @property memorySize 历史记忆容量 / Success-history memory size
 * @property pBestRate p 最优个体比例 / p-best proportion
 * @property archiveAmount 档案容量 / Archive capacity
*/
data class LShadePolicy(
    val minimumSize: Int = 4,
    val sizeRatio: ParameterSource = LinearSource(Flt64.one, Flt64(0.05), UInt64(100)),
    val memorySize: Int = 5,
    val pBestRate: Double = 0.2,
    val archiveAmount: Int = 1
)

/**
 * L-SHADE，即 `ShadeRunner` 骨架 + 线性种群缩减。 / L-SHADE: the `ShadeRunner` skeleton plus linear
 * population size reduction.
 *
 * 全部可调项集中在 [policy] 里。 / Every tunable lives in [policy].
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param size 种群规模上限 / Upper bound of the population size
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param policy L-SHADE 超参数 / L-SHADE hyper-parameters
 * @return 求解器 / Solver
*/
fun <ObjValue, V> lShade(
    size: Int,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    policy: LShadePolicy = LShadePolicy()
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.lShade(
        size = size,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        minimumSize = policy.minimumSize,
        sizeRatio = policy.sizeRatio,
        memorySize = policy.memorySize,
        pBestRate = policy.pBestRate,
        archiveAmount = policy.archiveAmount
    )
