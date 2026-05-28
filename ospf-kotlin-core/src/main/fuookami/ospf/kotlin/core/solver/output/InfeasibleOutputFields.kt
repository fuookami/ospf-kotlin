@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.output

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 不可行输出统一字段
 * Infeasible output unified fields
 */

/**
 * 不可行求解输出的统一字段，用于从求解状态中提取信息。
 * Unified fields for infeasible solver output, used to extract information from solving status.
 *
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙（可选）/ MIP gap (optional)
 * @property solveTime 求解时间 / Solve time
 */
data class InfeasibleUnifiedFields(
    val iterations: UInt64?,
    val nodeCount: UInt64?,
    val bestBound: Flt64?,
    val mipGap: Flt64?,
    val solveTime: Duration
)

/**
 * 从求解状态中解析不可行统一字段。
 * Resolve infeasible unified fields from solving status.
 *
 * @param latestStatus 最新求解状态（可选）/ Latest solving status (optional)
 * @param fallbackSolveTime 回退求解时间 / Fallback solve time
 * @return 不可行统一字段 / Infeasible unified fields
 */
fun resolveInfeasibleUnifiedFields(
    latestStatus: SolvingStatus?,
    fallbackSolveTime: Duration
): InfeasibleUnifiedFields {
    return InfeasibleUnifiedFields(
        iterations = latestStatus?.iterations,
        nodeCount = latestStatus?.nodeCount,
        bestBound = latestStatus?.bestBound,
        mipGap = latestStatus?.mipGap,
        solveTime = latestStatus?.solveTime ?: fallbackSolveTime
    )
}
