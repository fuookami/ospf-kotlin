/**
 * PWL 连续半径近似配置与断点策略。
 * PWL continuous-radius approximation config and breakpoint strategies.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
/**
 * PWL 连续半径近似配置。
 * PWL continuous-radius approximation config.
 *
 * @property maxSegments 最大分段数 / maximum number of segments
 * @property relativeErrorTolerance 相对误差上限 / relative error tolerance
 * @property breakpointStrategy 断点生成策略 / breakpoint generation strategy
 * @property customBreakpoints 自定义断点（CSV/Gurobi 参数入口）/ custom breakpoints (CSV/Gurobi parameter entry)
 * @property enableDebugInfo 输出 PWL 调试信息 / output PWL debug info
 */
data class PWLRadiusApproximationConfig(
    val maxSegments: Int = 8,
    val relativeErrorTolerance: FltX = FltX(0.01),
    val breakpointStrategy: PWLBreakpointStrategy = PWLBreakpointStrategy.Uniform,
    val customBreakpoints: List<FltX>? = null,
    val enableDebugInfo: Boolean = false
)

/**
 * 断点生成策略。
 * Breakpoint generation strategy.
 */
enum class PWLBreakpointStrategy {
    /** 均匀分布 / Uniform distribution */
    Uniform,
    /** 曲率自适应 / Curvature-adaptive */
    Adaptive,
    /** 误差驱动 / Error-driven */
    ErrorDriven
}
