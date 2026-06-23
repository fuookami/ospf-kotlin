/**
 * 核心错误类型体系，包含变量、模型和求解器错误的结构化定义。
 * Core error type system containing structured definitions for variable, model, and solver errors.
 */
package fuookami.ospf.kotlin.core.error

import java.time.Duration
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 结构化核心错误接口，提供错误码、消息和转换方法。
 * Structured core error interface providing error code, message, and conversion methods.
 */
interface StructuredCoreError {
    val errorCode: ErrorCode
    val message: String

    /** 将此错误转换为通用 Error 对象 / Convert this error to a generic Error object */
    fun toError(): Error<ErrorCode> {
        return Err(errorCode, message)
    }

    /** 将此错误转换为失败的 Ret 结果 / Convert this error to a failed Ret result */
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
    /**
     * 变量相关错误的包装器 / Wrapper for variable-related errors
     *
     * @property detail 变量错误详情 / The underlying variable error
     */
    data class Variable(val detail: VariableError) : CoreError(detail.errorCode, detail.message)
    /**
     * 模型相关错误的包装器 / Wrapper for model-related errors
     *
     * @property detail 模型错误详情 / The underlying model error
     */
    data class Model(val detail: ModelError) : CoreError(detail.errorCode, detail.message)
    /**
     * 求解器相关错误的包装器 / Wrapper for solver-related errors
     *
     * @property detail 求解器错误详情 / The underlying solver error
     */
    data class Solver(val detail: SolverError) : CoreError(detail.errorCode, detail.message)
    /**
     * 功能未实现错误 / Feature not implemented error
     *
     * @property detail 未实现功能的描述 / Description of the unimplemented feature
     */
    data class NotImplemented(val detail: String) : CoreError(
        ErrorCode.IllegalArgument,
        "Not implemented: $detail"
    )
    /**
     * 内部错误 / Internal error
     *
     * @property detail 内部错误的描述 / Description of the internal error
     */
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
    /**
     * 变量未找到错误 / Variable not found error
     *
     * @property variable 未找到的变量名 / Name of the variable that was not found
     */
    data class NotFound(val variable: String) : VariableError(
        ErrorCode.DataNotFound,
        "Variable not found: $variable"
    )
    /**
     * 变量已存在错误 / Variable already exists error
     *
     * @property variable 已存在的变量名 / Name of the variable that already exists
     */
    data class AlreadyExists(val variable: String) : VariableError(
        ErrorCode.TokenExisted,
        "Variable already exists: $variable"
    )
    /**
     * 变量范围无效错误（下界大于上界）/ Invalid variable range error (lower bound exceeds upper bound)
     *
     * @property lower 下界值 / The lower bound value
     * @property upper 上界值 / The upper bound value
     */
    data class InvalidRange(val lower: String?, val upper: String?) : VariableError(
        ErrorCode.IllegalArgument,
        "Invalid variable range: lower bound $lower > upper bound $upper"
    )
    /**
     * 变量值无效错误 / Invalid variable value error
     *
     * @property variable 变量名 / Name of the variable
     * @property value 无效的值 / The invalid value
     */
    data class InvalidValue(val variable: String, val value: String) : VariableError(
        ErrorCode.IllegalArgument,
        "Invalid variable value: $value for variable $variable"
    )
    /**
     * 变量名冲突错误 / Variable name conflict error
     *
     * @property name 冲突的变量名 / The conflicting variable name
     */
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
    /** 模型未初始化错误 / Model not initialized error */
    data object NotInitialized : ModelError(
        ErrorCode.ApplicationError,
        "Model not initialized"
    )
    /** 模型已求解错误 / Model already solved error */
    data object AlreadySolved : ModelError(
        ErrorCode.ApplicationError,
        "Model already solved"
    )
    /**
     * 约束冲突错误 / Constraint conflict error
     *
     * @property detail 约束冲突的描述 / Description of the constraint conflict
     */
    data class ConstraintConflict(val detail: String) : ModelError(
        ErrorCode.IllegalArgument,
        "Constraint conflict: $detail"
    )
    /** 缺少目标函数错误 / Missing objective function error */
    data object MissingObjective : ModelError(
        ErrorCode.DataEmpty,
        "Missing objective function"
    )
    /**
     * 无效约束错误 / Invalid constraint error
     *
     * @property detail 无效约束的描述 / Description of the invalid constraint
     */
    data class InvalidConstraint(val detail: String) : ModelError(
        ErrorCode.IllegalArgument,
        "Invalid constraint: $detail"
    )
    /**
     * 符号未注册错误 / Symbol not registered error
     *
     * @property symbol 未注册的符号名 / Name of the unregistered symbol
     */
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
    /**
     * 求解器不可用错误 / Solver not available error
     *
     * @property solver 不可用的求解器名称 / Name of the unavailable solver
     */
    data class NotAvailable(val solver: String) : SolverError(
        ErrorCode.SolverNotFound,
        "Solver not available: $solver"
    )
    /**
     * 求解失败错误 / Solve failed error
     *
     * @property detail 求解失败的描述 / Description of the solve failure
     */
    data class SolveFailed(val detail: String) : SolverError(
        ErrorCode.OREngineSolvingException,
        "Solve failed: $detail"
    )
    /** 无可行解错误 / No solution found error */
    data object NoSolution : SolverError(
        ErrorCode.ORSolutionInvalid,
        "No solution found"
    )
    /** 解无界错误 / Solution unbounded error */
    data object Unbounded : SolverError(
        ErrorCode.ORModelUnbounded,
        "Solution is unbounded"
    )
    /** 问题不可行错误 / Problem infeasible error */
    data object Infeasible : SolverError(
        ErrorCode.ORModelInfeasible,
        "Problem is infeasible"
    )
    /**
     * 数值计算错误 / Numerical computation error
     *
     * @property detail 数值错误的描述 / Description of the numerical error
     */
    data class NumericalError(val detail: String) : SolverError(
        ErrorCode.OREngineSolvingException,
        "Numerical error: $detail"
    )
    /**
     * 精度损失错误 / Precision loss error
     *
     * @property detail 精度损失的描述 / Description of the precision loss
     */
    data class PrecisionLoss(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Precision loss: $detail"
    )
    /**
     * 数值溢出错误 / Numerical overflow error
     *
     * @property detail 溢出的描述 / Description of the overflow
     */
    data class Overflow(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Overflow: $detail"
    )
    /**
     * 非有限值错误 / Non-finite value error
     *
     * @property detail 非有限值的描述 / Description of the non-finite value
     */
    data class NonFinite(val detail: String) : SolverError(
        ErrorCode.ORSolutionInvalid,
        "Non-finite value: $detail"
    )
    /**
     * 不支持的值类型错误 / Unsupported value type error
     *
     * @property type 不支持的值类型 / The unsupported value type
     */
    data class UnsupportedValueType(val type: String) : SolverError(
        ErrorCode.IllegalArgument,
        "Unsupported value type: $type"
    )
    /**
     * 求解器超时错误 / Solver timeout error
     *
     * @property duration 超时时长 / Duration of the timeout
     */
    data class Timeout(val duration: Duration) : SolverError(
        ErrorCode.OREngineTerminated,
        "Solver timeout after $duration"
    )
    /**
     * 许可证错误 / License error
     *
     * @property detail 许可证错误的描述 / Description of the license error
     */
    data class LicenseError(val detail: String) : SolverError(
        ErrorCode.AuthenticationError,
        "License error: $detail"
    )
}

/** 将变量错误转换为核心错误 / Convert a variable error to a core error */
fun VariableError.asCoreError(): CoreError = CoreError.Variable(this)
/** 将模型错误转换为核心错误 / Convert a model error to a core error */
fun ModelError.asCoreError(): CoreError = CoreError.Model(this)
/** 将求解器错误转换为核心错误 / Convert a solver error to a core error */
fun SolverError.asCoreError(): CoreError = CoreError.Solver(this)

// ============================================================================
// 命名错误子类型：用于重复构造点去重，调用方可通过 `when (error is XxxError)` 稳定断言
// Named error subclasses: deduplicate repeated construction points, callers can assert via `when (error is XxxError)`
// ============================================================================

/**
 * 求解器未找到错误。
 * Solver not found error.
 *
 * 替代重复的 `Failed(ErrorCode.SolverNotFound, "No solver valid.")` 构造。
 * Replaces repeated `Failed(ErrorCode.SolverNotFound, "No solver valid.")` constructions.
 *
 * @property solver 尝试查找的求解器名称（可选）/ Name of the solver that was looked up (optional)
 */
class SolverNotFoundError(
    val solver: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.SolverNotFound,
    message = if (solver != null) "No solver valid: $solver" else "No solver valid."
)

/**
 * 求解器环境丢失错误。
 * Solver environment lost error.
 *
 * 替代重复的 `Failed(ErrorCode.OREngineEnvironmentLost, ...)` 构造。
 * Replaces repeated `Failed(ErrorCode.OREngineEnvironmentLost, ...)` constructions.
 *
 * @property detail 环境丢失的描述 / Description of the environment loss
 */
class SolverEnvironmentLostError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.OREngineEnvironmentLost,
    message = detail ?: "Solver environment lost."
)

/**
 * 求解器求解异常错误。
 * Solver solving exception error.
 *
 * 替代重复的 `Failed(ErrorCode.OREngineSolvingException, ...)` 构造。
 * Replaces repeated `Failed(ErrorCode.OREngineSolvingException, ...)` constructions.
 *
 * @property detail 求解异常的描述 / Description of the solving exception
 */
class SolverSolvingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.OREngineSolvingException,
    message = detail ?: "Solver solving exception."
)

/**
 * 求解器建模异常错误。
 * Solver modeling exception error.
 *
 * 替代重复的 `Failed(ErrorCode.OREngineModelingException, ...)` 构造。
 * Replaces repeated `Failed(ErrorCode.OREngineModelingException, ...)` constructions.
 *
 * @property detail 建模异常的描述 / Description of the modeling exception
 */
class SolverModelingError(
    val detail: String? = null
) : Err<ErrorCode>(
    code = ErrorCode.OREngineModelingException,
    message = detail ?: "Solver modeling exception."
)

/**
 * 求解器终止错误。
 * Solver terminated error.
 *
 * 替代重复的 `Failed(ErrorCode.OREngineTerminated)` 构造。
 * Replaces repeated `Failed(ErrorCode.OREngineTerminated)` constructions.
 */
class SolverTerminatedError : Err<ErrorCode>(
    code = ErrorCode.OREngineTerminated,
    message = "Solver terminated."
)
