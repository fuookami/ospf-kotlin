/**
 * CP 求解器解、冲突与输出。 / CP solver solutions, conflicts, and outputs.
 */
package fuookami.ospf.kotlin.core.solver.output

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalValue
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVariable

private const val MAX_COMPATIBLE_DOUBLE_INTEGER = 9_007_199_254_740_991L

/** 将精确 CP 目标投影为不丢精度的兼容浮点值。 / Project an exact CP objective to a lossless compatibility floating value. */
fun Int64.toCompatibilityFlt64(): Flt64? {
    val value = toLong()
    return if (abs(value) <= MAX_COMPATIBLE_DOUBLE_INTEGER) {
        toFlt64()
    } else {
        null
    }
}

/**
 * CP 精确整数解。 / Exact integer CP solution.
 *
 * @property values 稳定变量 ID 到整数值的映射 / Stable variable ID to integer value mapping
 * @property intervals interval 解映射 / Interval solution mapping
 */
data class ConstraintProgrammingSolution(
    val values: Map<VariableId, Int64> = emptyMap(),
    val intervals: Map<IntervalId, IntervalValue> = emptyMap()
) {
    /**
     * 按稳定变量 ID 获取值。 / Get a value by stable variable ID.
     *
     * @param id 稳定变量 ID / Stable variable ID
     * @return 整数值或结构化错误 / Integer value or a structured error
     */
    fun value(id: VariableId): Ret<Int64> {
        return values[id]?.let(::ok) ?: Failed(
            ErrorCode.DataNotFound,
            "缺少 CP 解变量：$id / CP solution does not contain variable: $id"
        )
    }

    /**
     * 按 OSPF 变量获取值。 / Get a value by an OSPF variable.
     *
     * @param variable OSPF 变量 / OSPF variable
     * @return 整数值或结构化错误 / Integer value or a structured error
     */
    fun value(variable: AbstractVariableItem<*, *>): Ret<Int64> {
        return value(VariableId("${variable.identifier}:${variable.index}"))
    }

    /**
     * 获取二值变量值。 / Get a binary variable value.
     *
     * @param variable 二值变量 / Binary variable
     * @return 布尔值或结构化错误 / Boolean value or a structured error
     */
    fun boolean(variable: BinVariable): Ret<Boolean> {
        return value(variable).map { it == Int64.one }
    }

    /**
     * 获取 interval 解。 / Get an interval value.
     *
     * @param id interval ID / Interval ID
     * @return interval 值或结构化错误 / Interval value or a structured error
     */
    fun interval(id: IntervalId): Ret<IntervalValue> {
        return intervals[id]?.let(::ok) ?: Failed(
            ErrorCode.DataNotFound,
            "缺少 CP 解 interval：$id / CP solution does not contain interval: $id"
        )
    }

    /**
     * 返回线性求解器兼容的值列表。 / Return a linear-solver-compatible value list.
     *
     * @param order 目标变量顺序 / Requested variable order
     * @return 按顺序排列的整数值 / Ordered integer values
     */
    fun asList(order: List<VariableId>): Solution<Int64> {
        return order.mapNotNull { values[it] }
    }
}

/** Minimality status of a CP conflict. / CP 冲突的最小性状态。 */
enum class ConstraintProgrammingConflictMinimality {
    /** 未执行必要性检查。 / No irreducibility checks were performed. */
    NotChecked,

    /** 所有候选均通过删除复验。 / Every remaining candidate passed deletion checks. */
    Irreducible,

    /** 至少一次删除复验未能得到证明终态。 / At least one deletion check lacked a proven terminal state. */
    Partial
}

/** CP conflict 的最小公共表示。 / Minimal backend-neutral CP conflict representation.
 *
 * @property constraintIds Conflicting constraint IDs. / 冲突约束 ID。
 * @property variableIds Conflicting variable IDs. / 冲突变量 ID。
 * @property message Optional conflict message. / 可选冲突说明。
 * @property assumptions Assumptions participating in the conflict. / 参与冲突的假设。
 * @property minimality Minimality status. / 最小性状态。
 * @property members Original model members, excluding solver auxiliaries. / 原始模型成员，不含求解器辅助项。
 * @property activationIds Stable activation IDs. / activation 稳定 ID。
 * @property activationMembers Stable activation-to-member projection. / activation 到原始成员的稳定映射。
 * @property validity Validity of the current conflict set. / 当前冲突集合的有效性。
 * @property verificationChecks Number of verification/deletion checks. / 复验或删除检查次数。
 * @property terminationReason Verification termination reason. / 复验终止原因。
 */
data class ConstraintProgrammingConflict(
    val constraintIds: Set<ConstraintId> = emptySet(),
    val variableIds: Set<VariableId> = emptySet(),
    val message: String? = null,
    val assumptions: List<BooleanLiteral> = emptyList(),
    val minimality: ConstraintProgrammingConflictMinimality = ConstraintProgrammingConflictMinimality.NotChecked,
    /** 原始模型成员；不包含 solver 辅助约束。 / Original model members, excluding solver auxiliaries. */
    val members: Set<InfeasibilityMember> = emptySet(),
    /** activation 的稳定 ID。 / Stable activation IDs. */
    val activationIds: Set<String> = emptySet(),
    /** activation 到原始成员的稳定投影。 / Stable activation-to-member projection. */
    val activationMembers: Map<String, InfeasibilityMember> = emptyMap(),
    /** 当前 conflict 成员集合的不可行性复验状态。 / Verification status of the current conflict set. */
    val validity: EvidenceValidity = EvidenceValidity.Verified,
    /** 删除复验次数。 / Number of deletion checks. */
    val verificationChecks: UInt64? = null,
    /** 最终复验或删除复验的终止原因。 / Termination reason of verification. */
    val terminationReason: TerminationReason? = null
)

/**
 * CP 求解输出。 / CP solver output.
 *
 * @property report 统一求解报告 / Unified solve report
 */
sealed interface ConstraintProgrammingSolverOutput : SolverOutput {
    /**
     * 统一报告：CP 解/诊断保持 Int64，统计保持 Flt64。 /
     * Unified report: CP solutions/diagnostics stay Int64 while statistics stay Flt64.
     */
    val report: SolveReport<Int64>?
}

/**
 * CP 可行/最优输出。 / CP feasible/optimal output.
 *
 * @property solution CP 整数解 / CP integer solution
 * @property objective 目标值 / Objective value
 * @property bestBound 最佳界 / Best bound
 * @property status 求解状态 / Solver status
 * @property proofStatus 证明状态 / Proof status
 * @property report 统一求解报告 / Unified solve report
 */
data class ConstraintProgrammingFeasibleOutput(
    val solution: ConstraintProgrammingSolution,
    val objective: Flt64? = null,
    val bestBound: Flt64? = null,
    val status: SolverStatus = SolverStatus.Optimal,
    val proofStatus: ProofStatus = ProofStatus.None,
    override val report: SolveReport<Int64>? = null,
    /** 精确整数目标；兼容浮点字段不应作为精确 CP 计算输入。 / Exact integer objective; the compatibility floating field must not drive exact CP calculations. */
    val exactObjective: Int64? = null
) : ConstraintProgrammingSolverOutput

/**
 * CP 已证明不可行输出。 / CP proven-infeasible output.
 *
 * @property conflict 冲突证据 / Conflict evidence
 * @property proofStatus 证明状态 / Proof status
 * @property report 统一求解报告 / Unified solve report
 */
data class ConstraintProgrammingInfeasibleOutput(
    val conflict: ConstraintProgrammingConflict? = null,
    val proofStatus: ProofStatus = ProofStatus.Verified,
    override val report: SolveReport<Int64>? = null
) : ConstraintProgrammingSolverOutput

/**
 * CP 未知/达到限制输出。 / CP unknown/limit output.
 *
 * @property terminationReason 终止原因 / Termination reason
 * @property report 统一求解报告 / Unified solve report
 */
data class ConstraintProgrammingUnknownOutput(
    val terminationReason: TerminationReason,
    override val report: SolveReport<Int64>? = null
) : ConstraintProgrammingSolverOutput
