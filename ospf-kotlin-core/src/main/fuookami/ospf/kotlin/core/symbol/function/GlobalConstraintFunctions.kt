@file:Suppress("unused")

/** 全局与排程约束函数工厂 / Global and scheduling constraint factories. */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.constraint_programming.*
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweringPolicy
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer

/**
 * 复用 CP 模型内建约束 AST 的函数入口，并提供到普通线性元模型的显式降阶路径。
 *
 * Function entry points that reuse the CP model's built-in constraint AST, with an explicit lowering path to a linear meta-model.
 */
object GlobalConstraintFunctions {
    /**
     * 创建全异约束。
     *
     * Create an all-different constraint.
     *
     * @param expressions 待约束的整数表达式 / Integer expressions that must differ
     * @return 全异 CP 约束或结构化错误 / All-different CP constraint or a structured error
     */
    @JvmStatic
    fun allDifferent(expressions: Iterable<ConstraintProgrammingExpression>): Ret<ConstraintProgrammingConstraint.AllDifferent> {
        return ConstraintProgrammingConstraint.allDifferent(expressions)
    }

    /**
     * 创建互不重叠的 interval 约束；支持已有 optional interval presence 语义。
     *
     * Create a non-overlap constraint over intervals, preserving optional-interval presence semantics.
     *
     * @param intervals 参与排程的 interval / Intervals to schedule
     * @return NoOverlap CP 约束或结构化错误 / NoOverlap CP constraint or a structured error
     */
    @JvmStatic
    fun noOverlap(intervals: Iterable<IntervalVariable>): Ret<NoOverlap> {
        return NoOverlap(intervals)
    }

    /**
     * 创建累计资源容量约束；输入 AST 保留 CP 原生的表达式 demand/capacity 能力。
     * MIP lowering 目前要求 demand 与 capacity 为非负整数常量。
     *
     * Create a cumulative resource constraint; the AST preserves native CP expression demands and capacity.
     * MIP lowering currently requires non-negative integer constants for demands and capacity.
     *
     * @param intervals 消耗资源的 interval / Intervals consuming the resource
     * @param demands 与 interval 一一对应的整数需求表达式 / Integer demand expressions paired with intervals
     * @param capacity 资源容量表达式 / Resource-capacity expression
     * @return Cumulative CP 约束或结构化错误 / Cumulative CP constraint or a structured error
     */
    @JvmStatic
    fun cumulative(
        intervals: Iterable<IntervalVariable>,
        demands: Iterable<ConstraintProgrammingExpression>,
        capacity: ConstraintProgrammingExpression
    ): Ret<Cumulative> {
        return Cumulative(
            intervals = intervals,
            demands = demands,
            capacity = capacity
        )
    }

    /**
     * 将 CP 模型精确降为可交给普通线性求解器的元模型。
     * Cumulative 仅在 [policy] 显式允许且满足有限整数时域、常量需求/容量和规模限制时可用。
     *
     * Lower a CP model exactly to a meta-model that can be passed to an ordinary linear solver.
     * Cumulative is available only when [policy] enables it and finite integer horizon, constant demand/capacity, and size limits are satisfied.
     *
     * @param model 待降阶的 CP 模型 / CP model to lower
     * @param policy 精确降阶策略 / Exact lowering policy
     * @return 降阶模型或结构化错误 / Lowered model or a structured error
     */
    @JvmStatic
    fun lower(
        model: ConstraintProgrammingModel,
        policy: ConstraintProgrammingLoweringPolicy = ConstraintProgrammingLoweringPolicy.Strict
    ): Ret<ConstraintProgrammingLoweredLinearModel> {
        return ConstraintProgrammingToLinearModelLowerer(policy).lower(model)
    }

    /**
     * 将不可变 CP 快照精确降为可交给普通线性求解器的元模型。
     *
     * Lower an immutable CP snapshot exactly to a meta-model that can be passed to an ordinary linear solver.
     *
     * @param snapshot CP 模型快照 / CP model snapshot
     * @param policy 精确降阶策略 / Exact lowering policy
     * @return 降阶模型或结构化错误 / Lowered model or a structured error
     */
    @JvmStatic
    fun lower(
        snapshot: ConstraintProgrammingModelSnapshot,
        policy: ConstraintProgrammingLoweringPolicy = ConstraintProgrammingLoweringPolicy.Strict
    ): Ret<ConstraintProgrammingLoweredLinearModel> {
        return ConstraintProgrammingToLinearModelLowerer(policy).lower(snapshot)
    }
}
