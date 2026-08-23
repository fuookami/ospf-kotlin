/**
 * CP solver/session/options SPI。 / CP solver, session, and options SPI.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.solver.progress.SolverProgressContext
import fuookami.ospf.kotlin.core.solver.report.CancellationToken
import fuookami.ospf.kotlin.core.solver.report.BackendConfiguration
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.VariableId

/**
 * CP 求解选项。 / CP solve options.
 *
 * @property timeLimit 时间上限 / Time limit
 * @property nodeLimit 节点上限 / Node limit
 * @property solutionLimit 解数量上限 / Solution limit
 * @property threadCount 并行线程数 / Parallel thread count
 * @property randomSeed 随机种子 / Random seed
 * @property cancellationToken 取消令牌 / Cancellation token
 * @property progressContext 进度上下文 / Progress context
 * @property collectConflict 是否收集冲突 / Whether to collect conflicts
 * @property shrinkConflict 是否缩减冲突 / Whether to shrink conflicts
 * @property conflictShrinkLimit 冲突缩减次数上限 / Conflict shrink limit
 * @property conflictActivationIds 强制激活的成员 ID / Forced activation IDs
 * @property deterministic 是否使用确定性模式 / Whether to use deterministic mode
 * @property relativeObjectiveGap 相对目标间隙 / Relative objective gap
 * @property absoluteObjectiveGap 绝对目标间隙 / Absolute objective gap
 * @property logEnabled 是否输出日志 / Whether logging is enabled
 * @property backendConfiguration 后端配置 / Backend configuration
 * @property configurationFingerprint 可审计的有效配置指纹 / Auditable effective-configuration fingerprint
 */
data class ConstraintProgrammingSolveOptions(
    val timeLimit: Duration? = null,
    val nodeLimit: UInt64? = null,
    val solutionLimit: UInt64? = null,
    val threadCount: Int? = null,
    val randomSeed: Long? = null,
    val cancellationToken: CancellationToken? = null,
    val progressContext: SolverProgressContext? = null,
    val collectConflict: Boolean = false,
    val shrinkConflict: Boolean = false,
    val conflictShrinkLimit: UInt64? = null,
    /**
     * 诊断复验时强制激活的原始成员 ID；为空表示全部激活。 / / Original-member activation IDs forced on during diagnostic verification; null means all.
     */
    val conflictActivationIds: Set<String>? = null,
    val deterministic: Boolean = false,
    val relativeObjectiveGap: Flt64? = null,
    val absoluteObjectiveGap: Flt64? = null,
    val logEnabled: Boolean = false,
    val backendConfiguration: BackendConfiguration? = null,
    val configurationFingerprint: String? = null
)

/**
 * CP session，允许在同一模型上重复使用编译计划。 / CP session for reusing a compiled plan on one model.
 */
interface ConstraintProgrammingSession : AutoCloseable {
    /** 关联的模型。 / Associated model. */
    val model: ConstraintProgrammingModel

    /** 会话选项。 / Session options. */
    val options: ConstraintProgrammingSolveOptions

    /** 会话是否已关闭。 / Whether the session is closed. */
    val isClosed: Boolean

    /**
     * 求解一次；assumptions 只改变本次求解的激活文字。 / Solve once; assumptions affect only this solve.
     *
     * @param assumptions 激活文字 / Assumption literals
     * @param fixedValues 本轮必须固定的整数变量值 / Integer values fixed for this solve
     * @param hints 可选整数解提示 / Optional integer solution hint
     * @return CP 输出或结构化错误 / CP output or a structured error
     */
    suspend fun solve(
        assumptions: List<BooleanLiteral> = emptyList(),
        fixedValues: Map<VariableId, Int64> = emptyMap(),
        hints: ConstraintProgrammingSolution? = null
    ): Ret<ConstraintProgrammingSolverOutput>
}

/** CP 求解器通用接口。 / Common CP solver interface. */
interface ConstraintProgrammingSolver {
    /** 求解器描述符。 / Solver descriptor. */
    val descriptor: SolverDescriptor

    /**
     * 一次性求解。 / One-shot solve.
     *
     * @param model CP 模型 / CP model
     * @param options 求解选项 / Solve options
     * @return CP 输出或结构化错误 / CP output or a structured error
     */
    suspend fun solve(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
    ): Ret<ConstraintProgrammingSolverOutput>

    /**
     * 创建可复用 session。 / Create a reusable session.
     *
     * @param model CP 模型 / CP model
     * @param options 求解选项 / Solve options
     * @return CP session 或结构化错误 / CP session or a structured error
     */
    fun createSession(
        model: ConstraintProgrammingModel,
        options: ConstraintProgrammingSolveOptions = ConstraintProgrammingSolveOptions()
    ): Ret<ConstraintProgrammingSession>
}
