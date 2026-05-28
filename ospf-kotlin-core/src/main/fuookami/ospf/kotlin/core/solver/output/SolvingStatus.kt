@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.output

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig

/**
 * 求解状态数据结构
 * Solving status data structure
 */

/**
 * 求解过程状态，包含求解器信息、目标值、间隙等实时数据。
 * Solving process status, containing solver information, objective value, gap, and other real-time data.
 *
 * @property solver 求解器名称 / Solver name
 * @property solverIndex 求解器索引 / Solver index
 * @property solverConfig 求解器配置 / Solver configuration
 * @property intermediateModel 中间模型视图（可选）/ Intermediate model view (optional)
 * @property solverModel 求解器模型（可选）/ Solver model (optional)
 * @property solverCallBack 求解器回调（可选）/ Solver callback (optional)
 * @property objectCategory 目标类别（可选）/ Object category (optional)
 * @property time 当前时间 / Current time
 * @property obj 当前目标值 / Current objective value
 * @property possibleBestObj 可能的最优目标值 / Possible best objective value
 * @property initialBestObj 初始最优目标值 / Initial best objective value
 * @property gap 间隙 / Gap
 * @property currentBestSolution 当前最优解（可选）/ Current best solution (optional)
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙 / MIP gap
 * @property solveTime 求解时间 / Solve time
 */
data class SolvingStatus(
    val solver: String,
    val solverIndex: UInt64 = UInt64.zero,
    val solverConfig: SolverConfig,
    val intermediateModel: ModelView<*, *>? = null,
    val solverModel: Any? = null,
    val solverCallBack: Any? = null,
    val objectCategory: ObjectCategory?,
    val time: Duration,
    val obj: Flt64,
    val possibleBestObj: Flt64,
    val initialBestObj: Flt64,
    val gap: Flt64,
    val currentBestSolution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null,
    val iterations: UInt64? = null,
    val nodeCount: UInt64? = null,
    val bestBound: Flt64? = null,
    val mipGap: Flt64 = gap,
    val solveTime: Duration = time
)

typealias SolvingStatusCallBack = (SolvingStatus) -> Try
