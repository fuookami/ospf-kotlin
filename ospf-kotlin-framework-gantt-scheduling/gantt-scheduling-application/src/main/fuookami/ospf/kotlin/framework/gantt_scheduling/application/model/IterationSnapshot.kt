@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 迭代状态快照 / Iteration state snapshot */
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 迭代目标值物理量 / Iteration objective quantity */
typealias IterationObjectiveQuantity<V> = Quantity<V>

/**
 * 迭代状态快照，将 application 算法内部 Flt64 状态转换为对外泛型结果。 / Iteration state snapshot converting application-internal Flt64 state to generic outward-facing results.
 *
 * @param V 数值类型 / Numeric type
 * @property iteration 迭代次数 / Iteration count
 * @property runTime 运行时间 / Runtime
 * @property bestObjective 最佳整数目标值 / Best integer objective
 * @property bestLpObjective 最佳 LP 目标值 / Best LP objective
 * @property bestDualObjective 最佳对偶目标值；无可用证据时为空 / Best dual objective; null when no evidence is available
 * @property lowerBound 已证明的下界；尚无证明时为空 / Certified lower bound, null when no proof is available
 * @property upperBound 上界 / Upper bound
 * @property slowLpImprovementStep LP 改进缓慢步长 / Slow LP improvement step
 * @property optimalRate 启发式进度指标，无量纲，不作为最优性证据 / Heuristic progress indicator, dimensionless, not an optimality certificate
 * @property isImprovementSlow 是否改进缓慢 / Whether improvement is slow
*/
data class IterationSnapshot<V : RealNumber<V>>(
    val iteration: UInt64,
    val runTime: Duration,
    val bestObjective: IterationObjectiveQuantity<V>,
    val bestLpObjective: IterationObjectiveQuantity<V>,
    val bestDualObjective: IterationObjectiveQuantity<V>?,
    val lowerBound: IterationObjectiveQuantity<V>?,
    val upperBound: IterationObjectiveQuantity<V>?,
    val slowLpImprovementStep: IterationObjectiveQuantity<V>?,
    val optimalRate: V,
    val isImprovementSlow: Boolean
)

