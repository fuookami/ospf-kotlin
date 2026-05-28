/**
 * 核心错误类型体系，包含变量、模型和求解器错误的结构化定义。
 * Core error type system containing structured definitions for variable, model, and solver errors.
 */
package fuookami.ospf.kotlin.core.error

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret

/**
 * 结构化核心错误接口，提供错误码、消息和转换方法。
 * Structured core error interface providing error code, message, and conversion methods.
 */
interface StructuredCoreError {
    val errorCode: ErrorCode
    val message: String

    fun toError(): Error<ErrorCode> {
        return Err(errorCode, message)
    }

    fun <T> toFailed(): Ret<T> {
        return Failed(toError())
    }
}

/**
 * 核心错误的密封基类，聚合变量、模型和求解器错误。
 * Sealed base class for core errors, aggregating variable, model, and solver errors.
 *
 * @property errorCode 错误码 / Error code
 * @property message 错误消息 / Error message
 */
sealed class CoreError(
    override val errorCode: ErrorCode,
    override val message: String
) : StructuredCoreError {
    data class Variable(val detail: VariableError) : CoreError(detail.errorCode, detail.message)
    data class Model(val detail: ModelError) : CoreError(detail.errorCode, detail.message)
    data class Solver(val detail: SolverError) : CoreError(detail.errorCode, detail.message)
    data class NotImplemented(val detail: String) : CoreError(
        ErrorCode.IllegalArgument,
        "Not implemented: $detail"
    )
    data class Internal(val detail: String) : CoreError(
        ErrorCode.ApplicationException,
        "Internal error: $detail"
    )
}

/**
 * 变量相关错误的密封基类。
 * Sealed base class for variable-related errors.
 *
 * @property errorCode 错误码 / Error code
 * @property message 错误消息 / Error message
 */
sealed class VariableError(
    override val errorCode: ErrorCode,
    override val message: String
) : StructuredCoreError {
    data class NotFound(val variable: String) : VariableError(
        ErrorCode.DataNotFound,
        "Variable not found: $variable"
    )
    data class AlreadyExists(val variable: String) : VariableError(
        ErrorCode.TokenExisted,
        "Variable already exists: $variable"
    )
    data class InvalidRange(val lower: String?, val upper: String?) : VariableError(
        ErrorCode.IllegalArgument,
        "Invalid variable range: lower bound $lower > upper bound $upper"
    )
    data class InvalidValue(val variable: String, val value: String) : VariableError(
        ErrorCode.IllegalArgument,
        "Invalid variable value: $value for variable $variable"
    )
    data class NameConflict(val name: String) : VariableError(
        ErrorCode.SymbolRepetitive,
        "Variable name conflict: $name"
    )
}

/**
 * 模型相关错误的密封基类。
 * Sealed base class for model-related errors.
 *
 * @property errorCode 错误码 / Error code
 * @property message 错误消息 / Error message
 */
sealed class ModelError(
    override val errorCode: ErrorCode,
    override val message: String
) : StructuredCoreError {
    data object NotInitialized : ModelError(
        ErrorCode.ApplicationError,
        "Model not initialized"
    )
    data object AlreadySolved : ModelError(
        ErrorCode.ApplicationError,
        "Model already solved"
    )
    data class ConstraintConflict(val detail: String) : ModelError(
        ErrorCode.IllegalArgument,
        "Constraint conflict: $detail"
    )
    data object MissingObjective : ModelError(
        ErrorCode.DataEmpty,
        "Missing objective function"
    )
    data class InvalidConstraint(val detail: String) : ModelError(
        ErrorCode.IllegalArgument,
        "Invalid constraint: $detail"
    )
    data class SymbolNotRegistered(val symbol: String) : ModelError(
        ErrorCode.DataNotFound,
        "Symbol not registered: $symbol"
    )
}

/**
 * 求解器相关错误的密封基类。
 * Sealed base class for solver-related errors.
 *
 * @property errorCode 错误码 / Error code
 * @property message 错误消息 / Error message
 */
sealed class SolverError(
    override val errorCode: ErrorCode,
    override val message: String
) : StructuredCoreError {
    data class NotAvailable(val solver: String) : SolverError(
        ErrorCode.SolverNotFound,
        "Solver not available: $solver"
    )
    data class SolveFailed(val detail: String) : SolverError(
        ErrorCode.OREngineSolvingException,
        "Solve failed: $detail"
    )
    data object NoSolution : SolverError(
        ErrorCode.ORSolutionInvalid,
        "No solution found"
    )
    data object Unbounded : SolverError(
        ErrorCode.ORModelUnbounded,
        "Solution is unbounded"
    )
    data object Infeasible : SolverError(
        ErrorCode.ORModelInfeasible,
        "Problem is infeasible"
    )
    data class NumericalError(val detail: String) : SolverError(
        ErrorCode.OREngineSolvingException,
        "Numerical error: $detail"
    )
    data class PrecisionLoss(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Precision loss: $detail"
    )
    data class Overflow(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Overflow: $detail"
    )
    data class NonFinite(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Non-finite value: $detail"
    )
    data class UnsupportedValueType(val type: String) : SolverError(
        ErrorCode.IllegalArgument,
        "Unsupported value type: $type"
    )
    data class Timeout(val detail: String) : SolverError(
        ErrorCode.OREngineTerminated,
        "Solver timeout: $detail"
    )
    data class LicenseError(val detail: String) : SolverError(
        ErrorCode.AuthenticationError,
        "License error: $detail"
    )
}

fun VariableError.asCoreError(): CoreError = CoreError.Variable(this)
fun ModelError.asCoreError(): CoreError = CoreError.Model(this)
fun SolverError.asCoreError(): CoreError = CoreError.Solver(this)
