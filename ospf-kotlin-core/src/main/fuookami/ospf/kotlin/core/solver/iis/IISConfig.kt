@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.iis

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * IIS 计算配置
 * IIS computation configuration
 */

/**
 * IIS（不可行子系统）计算配置。
 * IIS (Irreducible Infeasible Subsystem) computation configuration.
 *
 * @property time 计算时间限制 / Computation time limit
 * @property threadNum 线程数 / Thread count
 * @property notImprovementTime 无改进时间限制 / No improvement time limit
 * @property computingStatusCallBack 计算状态回调 / Computing status callback
 * @property slackTolerance 松弛容忍度 / Slack tolerance
 * @property extraConfig 额外配置 / Extra configuration
 */
data class IISConfig(
    val time: Duration = 30.seconds,
    val threadNum: UInt64 = if (Runtime.getRuntime().availableProcessors() <= 16) {
        UInt64(Runtime.getRuntime().availableProcessors())
    } else if (Runtime.getRuntime().availableProcessors() < 24) {
        UInt64(16)
    } else if (Runtime.getRuntime().availableProcessors() < 32) {
        UInt64(24)
    } else {
        UInt64(32)
    },
    val notImprovementTime: Duration? = null,
    val computingStatusCallBack: IISComputingStatusCallBack? = null,
    val slackTolerance: Flt64 = Flt64(1e-6),
    val extraConfig: Any? = null
)
