@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 求解器输出数据结构 / Solver output data structures */
package fuookami.ospf.kotlin.core.solver.output

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.report.SolveDiagnostics
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 求解器输出的密封接口。 / Sealed interface for solver output.
*/
interface SolverOutput

/**
 * 统一求解器输出接口，包含通用的求解统计信息。 / Unified solver output interface, containing common solving statistics.
*/
interface UnifiedSolverOutput : SolverOutput {

    /** 迭代次数（可选） / Iteration count (optional) */
    val iterations: UInt64?

    /** 节点数（可选） / Node count (optional) */
    val nodeCount: UInt64?

    /** 最优界（可选） / Best bound (optional) */
    val bestBound: Flt64?

    /** MIP 间隙（可选） / MIP gap (optional) */
    val mipGap: Flt64?

    /** 求解时间（可选） / Solve time (optional) */
    val solveTime: Duration?
}

/**
 * 线性求解器输出接口。 / Linear solver output interface.
*/
interface LinearSolverOutput : SolverOutput {}

/**
 * 二次求解器输出接口。 / Quadratic solver output interface.
*/
interface QuadraticSolverOutput : SolverOutput {}

/**
 * 线性不可行求解器输出，包含 IIS 信息。 / Linear infeasible solver output, containing IIS information.
 *
 * @property iis 不可行子系统模型视图 / Infeasible subsystem model view
 * @property iisAvailable IIS 是否已物化；false 时 `iis` 仅为原模型快照 / Whether the IIS was materialized; when false, `iis` is only an original-model snapshot
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙（可选）/ MIP gap (optional)
 * @property solveTime 求解时间（可选）/ Solve time (optional)
 * @property diagnostics IIS 编排诊断；IIS 失败时保留原始不可行结论 / IIS diagnostics; the original infeasible conclusion is retained when IIS fails
*/
data class LinearInfeasibleSolverOutput(
    val iis: BasicLinearTriadModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null,
    val iisAvailable: Boolean = true,
    val diagnostics: SolveDiagnostics<Flt64> = SolveDiagnostics()
) : LinearSolverOutput, UnifiedSolverOutput

/**
 * 二次不可行求解器输出，包含 IIS 信息。 / Quadratic infeasible solver output, containing IIS information.
 *
 * @property iis 不可行子系统模型视图 / Infeasible subsystem model view
 * @property iisAvailable IIS 是否已物化；false 时 `iis` 仅为原模型快照 / Whether the IIS was materialized; when false, `iis` is only an original-model snapshot
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙（可选）/ MIP gap (optional)
 * @property solveTime 求解时间（可选）/ Solve time (optional)
 * @property diagnostics IIS 编排诊断；IIS 失败时保留原始不可行结论 / IIS diagnostics; the original infeasible conclusion is retained when IIS fails
*/
data class QuadraticInfeasibleSolverOutput(
    val iis: QuadraticTetradModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null,
    val iisAvailable: Boolean = true,
    val diagnostics: SolveDiagnostics<Flt64> = SolveDiagnostics()
) : QuadraticSolverOutput, UnifiedSolverOutput

/**
 * 带 IIS 的求解器输出包装。 / Solver output wrapper with IIS.
 *
 * @param IIS IIS 类型 / IIS type
 * @property output 求解器输出 / Solver output
 * @property iis IIS 信息（可选）/ IIS information (optional)
*/
data class SolverOutputWithIIS<out IIS>(
    val output: SolverOutput,
    val iis: IIS?
)

/**
 * 将求解器输出与 IIS 信息组合。 / Combine solver output with IIS information.
 *
 * @param IIS IIS 类型 / IIS type
 * @param iis IIS 信息（可为 null） / IIS information (nullable)
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun <IIS> SolverOutput.withIIS(iis: IIS?): SolverOutputWithIIS<IIS> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}

/**
 * 将求解器输出包装为无 IIS 信息的形式。 / Wrap solver output without IIS information.
 *
 * @return 无 IIS 的求解器输出 / Solver output without IIS
*/
fun SolverOutput.withoutIIS(): SolverOutputWithIIS<Nothing> {
    return SolverOutputWithIIS(
        output = this,
        iis = null
    )
}

/**
 * 将线性不可行求解器输出与内置 IIS 信息组合。 / Combine linear infeasible solver output with its built-in IIS information.
 *
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun LinearInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<BasicLinearTriadModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis.takeIf { iisAvailable }
    )
}

/**
 * 将二次不可行求解器输出与内置 IIS 信息组合。 / Combine quadratic infeasible solver output with its built-in IIS information.
 *
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun QuadraticInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<QuadraticTetradModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis.takeIf { iisAvailable }
    )
}
