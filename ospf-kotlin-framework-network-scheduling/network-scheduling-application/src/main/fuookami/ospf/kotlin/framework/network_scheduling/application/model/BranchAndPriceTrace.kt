package fuookami.ospf.kotlin.framework.network_scheduling.application.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Branch-and-Price 执行轨迹。 / Branch-and-Price execution trace.
 *
 * @property nodesExplored 已探索的节点数 / Number of explored nodes
 * @property nodesPruned 已剪枝的节点数 / Number of pruned nodes
 * @property globalLowerBound 全局下界 / Global lower bound
 * @property globalUpperBound 全局上界（incumbent 目标值） / Global upper bound (incumbent objective)
 * @property relativeGap 相对 gap / Relative gap
 * @property totalTime 总运行时间 / Total running time
 * @property totalIterations 总列生成迭代次数 / Total column generation iterations
 */
data class BranchAndPriceTrace(
    val nodesExplored: Int,
    val nodesPruned: Int,
    val globalLowerBound: Flt64,
    val globalUpperBound: Flt64,
    val relativeGap: Flt64,
    val totalTime: Duration,
    val totalIterations: Int
)
