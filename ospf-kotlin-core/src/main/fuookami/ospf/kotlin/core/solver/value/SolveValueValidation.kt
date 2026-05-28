/**
 * 求解值验证函数
 * Solve value validation functions
 */
package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.intermediate.*

/**
 * 验证 Flt64 值是否符合转换策略要求。
 * Validate whether a Flt64 value meets the conversion policy requirements.
 *
 * @param value 待验证的值 / Value to validate
 * @param policy 转换策略 / Conversion policy
 * @param fieldName 字段名称（用于错误信息）/ Field name (for error messages)
 * @return 验证结果 / Validation result
 */
fun validateSolverFlt64Value(
    value: Flt64,
    policy: SolveValueConversionPolicy,
    fieldName: String
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    return try {
        value.toSolverDouble(policy = policy, fieldName = fieldName)
        ok
    } catch (e: IllegalArgumentException) {
        Failed(Err(ErrorCode.IllegalArgument, e.message ?: "Strict conversion rejected value at $fieldName."))
    }
}

/**
 * 验证 Flt64 边界值是否符合转换策略要求（允许无穷大）。
 * Validate whether a Flt64 bound value meets the conversion policy requirements (allowing infinity).
 *
 * @param value 待验证的边界值 / Bound value to validate
 * @param policy 转换策略 / Conversion policy
 * @param fieldName 字段名称（用于错误信息）/ Field name (for error messages)
 * @return 验证结果 / Validation result
 */
fun validateSolverFlt64Bound(
    value: Flt64,
    policy: SolveValueConversionPolicy,
    fieldName: String
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    return try {
        value.toSolverDouble(
            policy = policy,
            fieldName = fieldName,
            rejectInfinity = false,
            nanMessage = "Strict conversion rejected NaN bound at $fieldName."
        )
        ok
    } catch (_: IllegalArgumentException) {
        return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected NaN bound at $fieldName."))
    }
}

/**
 * 验证线性模型的所有值是否符合转换策略要求。
 * Validate all values in a linear model against the conversion policy.
 *
 * @param model 线性三元模型视图 / Linear triad model view
 * @param policy 转换策略 / Conversion policy
 * @return 验证结果 / Validation result
 */
fun validateLinearModelValueConversion(
    model: LinearTriadModelView,
    policy: SolveValueConversionPolicy
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    model.variables.forEachIndexed { index, variable ->
        when (val result = validateSolverFlt64Bound(
            value = variable.lowerBound,
            policy = policy,
            fieldName = "linear.variables[$index].lowerBound"
        )) {
            is Failed -> return result
            else -> {}
        }
        when (val result = validateSolverFlt64Bound(
            value = variable.upperBound,
            policy = policy,
            fieldName = "linear.variables[$index].upperBound"
        )) {
            is Failed -> return result
            else -> {}
        }
        variable.initialResult?.let { initial ->
            when (val result = validateSolverFlt64Value(
                value = initial,
                policy = policy,
                fieldName = "linear.variables[$index].initialResult"
            )) {
                is Failed -> return result
                else -> {}
            }
        }
    }

    for (rowIndex in model.constraints.indices) {
        var cellIndex = 0
        var error: Try? = null
        model.constraints.sparseLhs.forEachEntry(rowIndex) { _, coefficient ->
            if (error == null) {
                when (val result = validateSolverFlt64Value(
                    value = coefficient,
                    policy = policy,
                    fieldName = "linear.constraints.lhs[$rowIndex][$cellIndex].coefficient"
                )) {
                    is Failed -> { error = result }
                    else -> {}
                }
            }
            cellIndex++
        }
        error?.let { return it }
    }

    model.constraints.rhs.forEachIndexed { rowIndex, rhs ->
        when (val result = validateSolverFlt64Value(
            value = rhs,
            policy = policy,
            fieldName = "linear.constraints.rhs[$rowIndex]"
        )) {
            is Failed -> return result
            else -> {}
        }
    }

    model.objective.objective.forEachIndexed { index, cell ->
        when (val result = validateSolverFlt64Value(
            value = cell.coefficient,
            policy = policy,
            fieldName = "linear.objective.cells[$index].coefficient"
        )) {
            is Failed -> return result
            else -> {}
        }
    }

    return validateSolverFlt64Value(
        value = model.objective.constant,
        policy = policy,
        fieldName = "linear.objective.constant"
    )
}

/**
 * 验证二次模型的所有值是否符合转换策略要求。
 * Validate all values in a quadratic model against the conversion policy.
 *
 * @param model 二次四元模型视图 / Quadratic tetrad model view
 * @param policy 转换策略 / Conversion policy
 * @return 验证结果 / Validation result
 */
fun validateQuadraticModelValueConversion(
    model: QuadraticTetradModelView,
    policy: SolveValueConversionPolicy
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    model.variables.forEachIndexed { index, variable ->
        when (val result = validateSolverFlt64Bound(
            value = variable.lowerBound,
            policy = policy,
            fieldName = "quadratic.variables[$index].lowerBound"
        )) {
            is Failed -> return result
            else -> {}
        }
        when (val result = validateSolverFlt64Bound(
            value = variable.upperBound,
            policy = policy,
            fieldName = "quadratic.variables[$index].upperBound"
        )) {
            is Failed -> return result
            else -> {}
        }
        variable.initialResult?.let { initial ->
            when (val result = validateSolverFlt64Value(
                value = initial,
                policy = policy,
                fieldName = "quadratic.variables[$index].initialResult"
            )) {
                is Failed -> return result
                else -> {}
            }
        }
    }

    for (rowIndex in model.constraints.indices) {
        var cellIndex = 0
        var error: Try? = null
        model.constraints.sparseLhs.forEachEntry(rowIndex) { _, _, coefficient ->
            if (error == null) {
                when (val result = validateSolverFlt64Value(
                    value = coefficient,
                    policy = policy,
                    fieldName = "quadratic.constraints.lhs[$rowIndex][$cellIndex].coefficient"
                )) {
                    is Failed -> { error = result }
                    else -> {}
                }
            }
            cellIndex++
        }
        error?.let { return it }
    }

    model.constraints.rhs.forEachIndexed { rowIndex, rhs ->
        when (val result = validateSolverFlt64Value(
            value = rhs,
            policy = policy,
            fieldName = "quadratic.constraints.rhs[$rowIndex]"
        )) {
            is Failed -> return result
            else -> {}
        }
    }

    model.objective.objective.forEachIndexed { index, cell ->
        when (val result = validateSolverFlt64Value(
            value = cell.coefficient,
            policy = policy,
            fieldName = "quadratic.objective.cells[$index].coefficient"
        )) {
            is Failed -> return result
            else -> {}
        }
    }

    return validateSolverFlt64Value(
        value = model.objective.constant,
        policy = policy,
        fieldName = "quadratic.objective.constant"
    )
}
