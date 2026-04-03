package fuookami.ospf.kotlin.core.backend.solver.value

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64

fun validateSolverFlt64Value(
    value: Flt64,
    policy: SolveValueConversionPolicy,
    fieldName: String
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    val raw = value.toDouble()
    if (raw.isNaN()) {
        return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected NaN at $fieldName."))
    }
    if (raw.isInfinite()) {
        return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected infinity at $fieldName."))
    }
    return ok
}

fun validateSolverFlt64Bound(
    value: Flt64,
    policy: SolveValueConversionPolicy,
    fieldName: String
): Try {
    if (policy == SolveValueConversionPolicy.AllowRounding) {
        return ok
    }

    val raw = value.toDouble()
    if (raw.isNaN()) {
        return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected NaN bound at $fieldName."))
    }
    return ok
}

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

    model.constraints.lhs.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { cellIndex, cell ->
            when (val result = validateSolverFlt64Value(
                value = cell.coefficient,
                policy = policy,
                fieldName = "linear.constraints.lhs[$rowIndex][$cellIndex].coefficient"
            )) {
                is Failed -> return result
                else -> {}
            }
        }
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

    model.constraints.lhs.forEachIndexed { rowIndex, row ->
        row.forEachIndexed { cellIndex, cell ->
            when (val result = validateSolverFlt64Value(
                value = cell.coefficient,
                policy = policy,
                fieldName = "quadratic.constraints.lhs[$rowIndex][$cellIndex].coefficient"
            )) {
                is Failed -> return result
                else -> {}
            }
        }
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
