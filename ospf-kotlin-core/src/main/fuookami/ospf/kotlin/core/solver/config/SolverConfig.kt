/**
 * 求解器通用配置
 * Common solver configuration
 */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 求解器通用配置，包含时间限制、线程数、间隙容忍度等参数。
 * Common solver configuration including time limit, thread count, gap tolerance, etc.
 *
 * @property time 求解时间限制 / Solve time limit
 * @property threadNum 线程数 / Thread count
 * @property gap 间隙容忍度 / Gap tolerance
 * @property notImprovementTime 无改进时间限制 / No improvement time limit
 * @property improveThreshold 改进阈值 / Improvement threshold
 * @property dumpMechanismModelConcurrent 是否并发转储机制模型 / Whether to dump mechanism model concurrently
 * @property dumpMechanismModelBlocking 是否阻塞转储机制模型 / Whether to dump mechanism model blocking
 * @property dumpIntermediateModelConcurrent 是否并发转储中间模型 / Whether to dump intermediate model concurrently
 * @property dumpIntermediateModelBounds 是否转储中间模型边界 / Whether to dump intermediate model bounds
 * @property dumpIntermediateModelForceBounds 是否强制转储中间模型边界 / Whether to force dump intermediate model bounds
 * @property extraConfig 额外配置 / Extra configuration
 */
data class SolverConfig(
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
    val gap: Flt64 = Flt64.zero,
    val notImprovementTime: Duration? = null,
    val improveThreshold: Flt64 = Flt64.decimalPrecision,
    val dumpMechanismModelConcurrent: Boolean? = null,
    val dumpMechanismModelBlocking: Boolean? = null,
    val dumpIntermediateModelConcurrent: Boolean? = null,
    val dumpIntermediateModelBounds: Boolean? = null,
    val dumpIntermediateModelForceBounds: Boolean? = null,
    val extraConfig: Any? = null
)
