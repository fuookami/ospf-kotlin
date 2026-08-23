package fuookami.ospf.kotlin.core.solver.report

import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation as ModelConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Evaluate all linear constraints against a solver-indexed value vector. /
 * 使用求解器索引值向量求值全部线性约束。
 *
 * The result is pure model-side evidence and does not depend on a backend. Missing values or a
 * negative tolerance are returned as structured input errors. /
 * 结果是纯模型侧证据，不依赖后端；缺失变量值或负容差会以结构化输入错误返回。
 *
 * @receiver linear triad model / 线性三元模型
 * @param values solver-indexed variable values / 求解器索引的变量值
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return one evaluation per model constraint / 每个模型约束对应一条求值结果
 */
fun LinearTriadModelView.evaluateConstraints(
    values: List<Flt64>,
    tolerance: Flt64 = Flt64.decimalPrecision
): Ret<List<ConstraintEvaluation<Flt64>>> {
    if (tolerance ls Flt64.zero) {
        return Failed(ErrorCode.IllegalArgument, "约束求值容差不能为负 / Constraint evaluation tolerance must not be negative")
    }
    val evaluations = constraints.indices.map { row ->
        var lhs = Flt64.zero
        for (cell in constraints.lhs[row]) {
            val value = values.getOrNull(cell.colIndex)
                ?: return Failed<List<ConstraintEvaluation<Flt64>>>(
                    ErrorCode.IllegalArgument,
                    "缺少变量值：${cell.colIndex} / Missing value for variable index ${cell.colIndex}"
                )
            lhs += cell.coefficient * value
        }
        val rhs = constraints.rhs[row]
        val relation = constraints.signs[row].toReportRelation()
        val (slack, violation) = relation.slackAndViolation(lhs, rhs)
        ConstraintEvaluation(
            constraintId = diagnosticConstraintId(row),
            lhs = lhs,
            rhs = rhs,
            relation = relation,
            slack = slack,
            violation = violation,
            tolerance = tolerance,
            satisfied = violation leq tolerance
        )
    }
    return ok(evaluations)
}

/**
 * Evaluate all quadratic constraints against a solver-indexed value vector. /
 * 使用求解器索引值向量求值全部二次约束。
 *
 * @receiver quadratic tetrad model / 二次四元模型
 * @param values solver-indexed variable values / 求解器索引的变量值
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return one evaluation per model constraint / 每个模型约束对应一条求值结果
 */
fun QuadraticTetradModelView.evaluateConstraints(
    values: List<Flt64>,
    tolerance: Flt64 = Flt64.decimalPrecision
): Ret<List<ConstraintEvaluation<Flt64>>> {
    if (tolerance ls Flt64.zero) {
        return Failed(ErrorCode.IllegalArgument, "约束求值容差不能为负 / Constraint evaluation tolerance must not be negative")
    }
    val evaluations = constraints.indices.map { row ->
        var lhs = Flt64.zero
        for (cell in constraints.lhs[row]) {
            val first = values.getOrNull(cell.colIndex1)
                ?: return Failed<List<ConstraintEvaluation<Flt64>>>(
                    ErrorCode.IllegalArgument,
                    "缺少变量值：${cell.colIndex1} / Missing value for variable index ${cell.colIndex1}"
                )
            lhs += if (cell.colIndex2 == null) {
                cell.coefficient * first
            } else {
                val second = values.getOrNull(cell.colIndex2)
                    ?: return Failed<List<ConstraintEvaluation<Flt64>>>(
                        ErrorCode.IllegalArgument,
                        "缺少变量值：${cell.colIndex2} / Missing value for variable index ${cell.colIndex2}"
                    )
                cell.coefficient * first * second
            }
        }
        val rhs = constraints.rhs[row]
        val relation = constraints.signs[row].toReportRelation()
        val (slack, violation) = relation.slackAndViolation(lhs, rhs)
        ConstraintEvaluation(
            constraintId = diagnosticConstraintId(row),
            lhs = lhs,
            rhs = rhs,
            relation = relation,
            slack = slack,
            violation = violation,
            tolerance = tolerance,
            satisfied = violation leq tolerance
        )
    }
    return ok(evaluations)
}

/**
 * Evaluate finite variable bounds against a solver-indexed value vector. /
 * 使用求解器索引值向量求值全部有限变量边界。
 *
 * Infinite solver bounds are omitted because they do not define a checkable boundary. Missing
 * values and negative tolerances are returned as structured input errors. /
 * 无穷求解器边界不构成可检查边界，因此不输出；缺少变量值或负容差会返回结构化输入错误。
 *
 * @receiver linear triad model / 线性三元模型
 * @param values solver-indexed variable values / 求解器索引的变量值
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return one evaluation per finite variable bound / 每个有限变量边界对应一条求值结果
 */
fun LinearTriadModelView.evaluateVariableBounds(
    values: List<Flt64>,
    tolerance: Flt64 = Flt64.decimalPrecision
): Ret<List<VariableBoundEvaluation<Flt64>>> {
    return variables.evaluateFiniteBounds(values, tolerance)
}

/**
 * Evaluate finite variable bounds against a solver-indexed value vector. /
 * 使用求解器索引值向量求值全部有限变量边界。
 *
 * @receiver quadratic tetrad model / 二次四元模型
 * @param values solver-indexed variable values / 求解器索引的变量值
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return one evaluation per finite variable bound / 每个有限变量边界对应一条求值结果
 */
fun QuadraticTetradModelView.evaluateVariableBounds(
    values: List<Flt64>,
    tolerance: Flt64 = Flt64.decimalPrecision
): Ret<List<VariableBoundEvaluation<Flt64>>> {
    return variables.evaluateFiniteBounds(values, tolerance)
}

/**
 * Attach model-side constraint and bound evaluations without changing the solve conclusion. /
 * 在不改变求解结论的前提下追加模型侧约束和边界求值。
 *
 * @receiver linear triad solve report / 线性三元求解报告
 * @param model source model / 源模型
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return report enriched with diagnostics / 追加诊断后的报告
 */
fun SolveReport<Flt64>.withModelDiagnostics(
    model: LinearTriadModelView,
    tolerance: Flt64 = Flt64.decimalPrecision
): SolveReport<Flt64> {
    return withModelDiagnostics(
        values = solution?.values,
        constraintEvaluation = { model.evaluateConstraints(it, tolerance) },
        boundEvaluation = { model.evaluateVariableBounds(it, tolerance) }
    )
}

/**
 * Attach model-side constraint and bound evaluations without changing the solve conclusion. /
 * 在不改变求解结论的前提下追加模型侧约束和边界求值。
 *
 * @receiver quadratic tetrad solve report / 二次四元求解报告
 * @param model source model / 源模型
 * @param tolerance accepted violation tolerance / 可接受的违反容差
 * @return report enriched with diagnostics / 追加诊断后的报告
 */
fun SolveReport<Flt64>.withModelDiagnostics(
    model: QuadraticTetradModelView,
    tolerance: Flt64 = Flt64.decimalPrecision
): SolveReport<Flt64> {
    return withModelDiagnostics(
        values = solution?.values,
        constraintEvaluation = { model.evaluateConstraints(it, tolerance) },
        boundEvaluation = { model.evaluateVariableBounds(it, tolerance) }
    )
}

private fun List<Variable>.evaluateFiniteBounds(
    values: List<Flt64>,
    tolerance: Flt64
): Ret<List<VariableBoundEvaluation<Flt64>>> {
    if (tolerance ls Flt64.zero) {
        return Failed(
            ErrorCode.IllegalArgument,
            "变量边界求值容差不能为负 / Variable-bound evaluation tolerance must not be negative"
        )
    }
    val evaluations = mutableListOf<VariableBoundEvaluation<Flt64>>()
    for (variable in this) {
        val value = values.getOrNull(variable.index)
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "缺少变量值：${variable.index} / Missing value for variable index ${variable.index}"
            )
        val variableId = variable.diagnosticVariableId()
        if (!(variable.lowerBound eq Flt64.negativeInfinity)) {
            val slack = value - variable.lowerBound
            evaluations += VariableBoundEvaluation(
                variableId = variableId,
                side = BoundSide.Lower,
                bound = variable.lowerBound,
                value = value,
                slack = slack,
                violation = if (slack ls Flt64.zero) -slack else Flt64.zero,
                tolerance = tolerance,
                satisfied = slack geq -tolerance
            )
        }
        if (!(variable.upperBound eq Flt64.infinity)) {
            val slack = variable.upperBound - value
            evaluations += VariableBoundEvaluation(
                variableId = variableId,
                side = BoundSide.Upper,
                bound = variable.upperBound,
                value = value,
                slack = slack,
                violation = if (slack ls Flt64.zero) -slack else Flt64.zero,
                tolerance = tolerance,
                satisfied = slack geq -tolerance
            )
        }
    }
    return ok(evaluations)
}

private fun SolveReport<Flt64>.withModelDiagnostics(
    values: List<Flt64>?,
    constraintEvaluation: (List<Flt64>) -> Ret<List<ConstraintEvaluation<Flt64>>>,
    boundEvaluation: (List<Flt64>) -> Ret<List<VariableBoundEvaluation<Flt64>>>
): SolveReport<Flt64> {
    if (values == null) {
        return this
    }
    val constraintResult = constraintEvaluation(values)
    val boundResult = boundEvaluation(values)
    val constraintEvaluations = when (constraintResult) {
        is Ok -> constraintResult.value
        else -> emptyList()
    }
    val boundEvaluations = when (boundResult) {
        is Ok -> boundResult.value
        else -> emptyList()
    }
    val errors = buildList {
        when (constraintResult) {
            is Failed -> add(evaluationIssue("constraint-evaluation-failed", constraintResult.error.message))
            is Fatal -> addAll(constraintResult.errors.map { error -> evaluationIssue("constraint-evaluation-failed", error.message) })
            is Ok -> {}
        }
        when (boundResult) {
            is Failed -> add(evaluationIssue("variable-bound-evaluation-failed", boundResult.error.message))
            is Fatal -> addAll(boundResult.errors.map { error -> evaluationIssue("variable-bound-evaluation-failed", error.message) })
            is Ok -> {}
        }
    }
    return copy(
        diagnostics = diagnostics.copy(
            constraintEvaluations = diagnostics.constraintEvaluations + constraintEvaluations,
            variableBoundEvaluations = diagnostics.variableBoundEvaluations + boundEvaluations,
            errors = diagnostics.errors + errors
        )
    )
}

private fun evaluationIssue(code: String, message: String?): SolveIssue {
    return SolveIssue(
        code = code,
        category = SolveIssueCategory.InvalidInput,
        message = "模型求值失败：${message ?: "未知错误"} / Model evaluation failed: ${message ?: "unknown error"}"
    )
}

private fun ModelConstraintRelation.toReportRelation(): ConstraintRelation = when (this) {
    ModelConstraintRelation.LessEqual -> ConstraintRelation.LessEqual
    ModelConstraintRelation.Equal -> ConstraintRelation.Equal
    ModelConstraintRelation.GreaterEqual -> ConstraintRelation.GreaterEqual
}

private fun ConstraintRelation.slackAndViolation(lhs: Flt64, rhs: Flt64): Pair<Flt64, Flt64> {
    val slack = when (this) {
        ConstraintRelation.LessEqual -> rhs - lhs
        ConstraintRelation.Equal -> rhs - lhs
        ConstraintRelation.GreaterEqual -> lhs - rhs
        ConstraintRelation.Range -> Flt64.zero
    }
    val violation = when (this) {
        ConstraintRelation.Equal -> (lhs - rhs).abs()
        ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual ->
            if (slack ls Flt64.zero) -slack else Flt64.zero
        ConstraintRelation.Range -> Flt64.zero
    }
    return slack to violation
}
