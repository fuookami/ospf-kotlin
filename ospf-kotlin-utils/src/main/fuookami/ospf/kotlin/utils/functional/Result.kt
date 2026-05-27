/**
 * 结果类型
 *
 * Result types for error handling with Ok, Failed, Fatal, and Warn states.
 * Provides functional error handling similar to Rust's Result type.
 * 结果类型，用于错误处理，包含 Ok、Failed、Fatal 和 Warn 状态。
 * 提供类似 Rust Result 类型的函数式错误处理。
 *
 * Key types:
 * - [Result]: Basic result with Ok and Failed states
 * - [ExResult]: Extended result with Ok, Failed, and Warn states
 * - [Ok]: Success result containing a value
 * - [Failed]: Failure result containing an error
 * - [Fatal]: Fatal result containing multiple errors
 * - [Warn]: Warning result containing both value and warnings
 *
 * 主要类型：
 * - [Result]: 基础结果，包含 Ok 和 Failed 状态
 * - [ExResult]: 扩展结果，包含 Ok、Failed 和 Warn 状态
 * - [Ok]: 成功结果，包含值
 * - [Failed]: 失败结果，包含错误
 * - [Fatal]: 致命结果，包含多个错误
 * - [Warn]: 警告结果，同时包含值和警告
 */
package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*

/**
 * 基础结果类型，包含 Ok 和 Failed 两种状态
 *
 * Basic result type with Ok and Failed states.
 * 基础结果类型，包含 Ok 和 Failed 两种状态。
 *
 * @param T 值的类型 / The type of the success value
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 */
sealed interface Result<out T, C : Any, out E : Error<C>> {
    /** 是否为成功结果 / Whether this is a successful result */
    val ok: Boolean get() = false
    /** 是否为失败结果 / Whether this is a failed result */
    val failed: Boolean get() = false
    /** 成功值，失败时为 null / The success value, or null if failed */
    val value: T? get() = null

    /**
     * 映射成功值
     *
     * Transforms the success value using the given function.
     * 使用给定函数转换成功值。
     *
     * @param transform 值转换函数 / The transformation function
     * @return 包含转换后值的新结果 / A new result containing the transformed value
     */
    fun <U> map(transform: (T) -> U): Result<U, C, E>
}

/**
 * 扩展结果类型，包含 Ok、Failed 和 Warn 三种状态
 *
 * Extended result type with Ok, Failed, and Warn states.
 * 扩展结果类型，包含 Ok、Failed 和 Warn 三种状态。
 *
 * @param T 值的类型 / The type of the success value
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 */
sealed interface ExResult<out T, C : Any, out E : Error<C>> {
    /** 是否为成功结果 / Whether this is a successful result */
    val ok: Boolean get() = false
    /** 是否为失败结果 / Whether this is a failed result */
    val failed: Boolean get() = false
    /** 是否为警告结果 / Whether this is a warned result */
    val warned: Boolean get() = false
    /** 成功值，失败时为 null / The success value, or null if failed */
    val value: T? get() = null

    /**
     * 映射成功值
     *
     * Transforms the success value using the given function.
     * 使用给定函数转换成功值。
     *
     * @param transform 值转换函数 / The transformation function
     * @return 包含转换后值的新扩展结果 / A new ExResult containing the transformed value
     */
    fun <U> map(transform: (T) -> U): ExResult<U, C, E>
}

/**
 * 成功结果，包含值
 *
 * Success result containing a value.
 * Implements both Result and ExResult for code reuse.
 * 成功结果，包含值。同时实现 Result 和 ExResult 以实现代码复用。
 *
 * @param T 值的类型 / The type of the success value
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 * @property value 成功值 / The success value
 */
class Ok<out T, C : Any, out E : Error<C>>(
    override val value: T
) : Result<T, C, E>, ExResult<T, C, E> {
    /** 始终为 true / Always true for Ok */
    override val ok: Boolean get() = true
    /** 始终为 false / Always false for Ok */
    override val failed: Boolean get() = false

    /**
     * 将值转换为指定类型
     *
     * Attempts to cast the value to the specified type.
     * 尝试将值转换为指定类型。
     *
     * @param U 目标类型 / The target type
     * @return 转换后的值，类型不匹配时返回 null / The cast value, or null if the type doesn't match
     */
    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    /**
     * 映射成功值
     *
     * Transforms the success value using the given function.
     * 使用给定函数转换成功值。
     *
     * @param transform 值转换函数 / The transformation function
     * @return 包含转换后值的新 Ok 实例 / A new Ok instance containing the transformed value
     */
    override fun <U> map(transform: (T) -> U): Ok<U, C, E> {
        return Ok(transform(value))
    }
}

/**
 * 失败结果，包含错误
 *
 * Failed result containing an error.
 * Implements both Result and ExResult for code reuse.
 * 失败结果，包含错误。同时实现 Result 和 ExResult 以实现代码复用。
 *
 * @param T 值的类型 / The type of the success value (never used)
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 * @property error 错误信息 / The error information
 */
class Failed<out T, C : Any, out E : Error<C>>(
    val error: E
) : Result<T, C, E>, ExResult<T, C, E> {
    companion object {
        /** 从错误码和消息创建 Failed / Create Failed from error code and message */
        operator fun <T> invoke(code: ErrorCode, message: String? = null): Failed<T, ErrorCode, Error<ErrorCode>> {
            return Failed(Err(code, message))
        }

        /** 从错误码和附加值创建 Failed / Create Failed from error code and value */
        operator fun <T, E> invoke(code: ErrorCode, value: E): Failed<T, ErrorCode, Error<ErrorCode>> {
            return Failed(ExErr(code, value))
        }

        /** 从错误码、消息和附加值创建 Failed / Create Failed from error code, message, and value */
        operator fun <T, E> invoke(code: ErrorCode, message: String, value: E): Failed<T, ErrorCode, Error<ErrorCode>> {
            return Failed(ExErr(code, message, value))
        }

        /** 从自定义错误码和消息创建 Failed / Create Failed from custom error code and message */
        operator fun <T, C : Any> invoke(code: C, message: String? = null): Failed<T, C, Error<C>> {
            return Failed(Err(code, message))
        }

        /** 从自定义错误码和附加值创建 Failed / Create Failed from custom error code and value */
        operator fun <T, C : Any, E> invoke(code: C, value: E): Failed<T, C, Error<C>> {
            return Failed(ExErr(code, value))
        }

        /** 从自定义错误码、消息和附加值创建 Failed / Create Failed from custom error code, message, and value */
        operator fun <T, C : Any, E> invoke(code: C, message: String, value: E): Failed<T, C, Error<C>> {
            return Failed(ExErr(code, message, value))
        }
    }

    /** 始终为 false / Always false for Failed */
    override val ok: Boolean get() = false
    /** 始终为 true / Always true for Failed */
    override val failed: Boolean get() = true
    /** 始终为 null / Always null for Failed */
    override val value: T? get() = null

    /** 错误码 / The error code */
    val code by error::code
    /** 错误消息 / The error message */
    val message by error::message

    /** 是否包含附加值 / Whether the error contains an additional value */
    val withValue by error::withValue
    /** 错误附加值 / The additional value attached to the error */
    val errValue by error::value

    /**
     * 映射（Failed 状态下保持不变）
     *
     * Returns a new Failed with the same error, ignoring the transform.
     * 返回具有相同错误的新 Failed，忽略转换函数。
     *
     * @param transform 值转换函数（未使用）/ The transformation function (unused)
     * @return 包含相同错误的新 Failed / A new Failed with the same error
     */
    override fun <U> map(transform: (T) -> U): Failed<U, C, E> {
        return Failed(error)
    }
}

/**
 * 致命结果，包含多个错误
 *
 * Fatal result containing multiple errors.
 * Implements both Result and ExResult for code reuse.
 * 致命结果，包含多个错误。同时实现 Result 和 ExResult 以实现代码复用。
 *
 * @param T 值的类型 / The type of the success value (never used)
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 * @property errors 错误列表 / The list of errors
 */
class Fatal<out T, C : Any, out E : Error<C>>(
    val errors: List<E>
) : Result<T, C, E>, ExResult<T, C, E> {
    companion object {
        /** 从多个错误创建 Fatal / Create Fatal from multiple errors */
        operator fun <T, C : Any> invoke(vararg errors: Error<C>): Fatal<T, C, Error<C>> {
            return Fatal(errors.toList())
        }

        /** 从错误码和消息创建 Fatal / Create Fatal from error code and message */
        operator fun <T> invoke(code: ErrorCode, message: String? = null): Fatal<T, ErrorCode, Error<ErrorCode>> {
            return Fatal(listOf(Err(code, message)))
        }

        /** 从错误码和附加值创建 Fatal / Create Fatal from error code and value */
        operator fun <T, E> invoke(code: ErrorCode, value: E): Fatal<T, ErrorCode, Error<ErrorCode>> {
            return Fatal(listOf(ExErr(code, value)))
        }

        /** 从错误码、消息和附加值创建 Fatal / Create Fatal from error code, message, and value */
        operator fun <T, E> invoke(code: ErrorCode, message: String, value: E): Fatal<T, ErrorCode, Error<ErrorCode>> {
            return Fatal(listOf(ExErr(code, message, value)))
        }

        /** 从自定义错误码和消息创建 Fatal / Create Fatal from custom error code and message */
        operator fun <T, C : Any> invoke(code: C, message: String? = null): Fatal<T, C, Error<C>> {
            return Fatal(listOf(Err(code, message)))
        }

        /** 从自定义错误码和附加值创建 Fatal / Create Fatal from custom error code and value */
        operator fun <T, C : Any, E> invoke(code: C, value: E): Fatal<T, C, Error<C>> {
            return Fatal(listOf(ExErr(code, value)))
        }

        /** 从自定义错误码、消息和附加值创建 Fatal / Create Fatal from custom error code, message, and value */
        operator fun <T, C : Any, E> invoke(code: C, message: String, value: E): Fatal<T, C, Error<C>> {
            return Fatal(listOf(ExErr(code, message, value)))
        }
    }

    /** 始终为 false / Always false for Fatal */
    override val ok: Boolean get() = false
    /** 始终为 true / Always true for Fatal */
    override val failed: Boolean get() = true
    /** 始终为 null / Always null for Fatal */
    override val value: T? get() = null

    /** 第一个错误，列表为空时为 null / The first error, or null if the list is empty */
    val firstError: E? get() = errors.firstOrNull()
    /** 错误数量 / The number of errors */
    val size: Int get() = errors.size
    /** 错误列表是否为空 / Whether the error list is empty */
    val isEmpty: Boolean get() = errors.isEmpty()

    /**
     * 映射（Fatal 状态下保持不变）
     *
     * Returns a new Fatal with the same errors, ignoring the transform.
     * 返回具有相同错误的新 Fatal，忽略转换函数。
     *
     * @param transform 值转换函数（未使用）/ The transformation function (unused)
     * @return 包含相同错误的新 Fatal / A new Fatal with the same errors
     */
    override fun <U> map(transform: (T) -> U): Fatal<U, C, E> {
        return Fatal(errors)
    }

    /**
     * 合并两个 Fatal 的错误列表
     *
     * Merges this Fatal's errors with another Fatal's errors.
     * 将此 Fatal 的错误列表与另一个 Fatal 的错误列表合并。
     *
     * @param other 要合并的另一个 Fatal / The other Fatal to merge with
     * @return 包含所有错误的新 Fatal / A new Fatal containing all errors
     */
    fun <U : Error<C>> merge(other: Fatal<*, C, U>): Fatal<T, C, Error<C>> {
        return Fatal(errors + other.errors)
    }

    /**
     * 遍历所有错误
     *
     * Iterates over all errors, applying the given action.
     * 遍历所有错误，执行给定的操作。
     *
     * @param action 对每个错误执行的操作 / The action to perform on each error
     */
    inline fun forEach(action: (E) -> Unit) {
        errors.forEach(action)
    }
}

/**
 * 警告结果，包含值和警告错误
 *
 * Warning result containing both a value and warning errors.
 * Only implements ExResult.
 * 警告结果，包含值和警告错误。仅实现 ExResult。
 *
 * @param T 值的类型 / The type of the success value
 * @param C 错误码的类型 / The type of the error code
 * @param E 错误的类型 / The type of the error
 * @property value 成功值 / The success value
 * @property warnings 警告列表 / The list of warnings
 */
class Warn<out T, C : Any, out E : Error<C>>(
    override val value: T,
    val warnings: List<E>
) : ExResult<T, C, E> {
    companion object {
        /** 从值、错误码和消息创建 Warn / Create Warn from value, error code, and message */
        operator fun <T> invoke(value: T, code: ErrorCode, message: String? = null): Warn<T, ErrorCode, Error<ErrorCode>> {
            return Warn(value, listOf(Err(code, message)))
        }

        /** 从值、错误码和附加值创建 Warn / Create Warn from value, error code, and warning value */
        operator fun <T, E> invoke(value: T, code: ErrorCode, warningValue: E): Warn<T, ErrorCode, Error<ErrorCode>> {
            return Warn(value, listOf(ExErr(code, warningValue)))
        }

        /** 从值、错误码、消息和附加值创建 Warn / Create Warn from value, error code, message, and warning value */
        operator fun <T, E> invoke(value: T, code: ErrorCode, message: String, warningValue: E): Warn<T, ErrorCode, Error<ErrorCode>> {
            return Warn(value, listOf(ExErr(code, message, warningValue)))
        }

        /** 从值、自定义错误码和消息创建 Warn / Create Warn from value, custom error code, and message */
        operator fun <T, C : Any> invoke(value: T, code: C, message: String? = null): Warn<T, C, Error<C>> {
            return Warn(value, listOf(Err(code, message)))
        }

        /** 从值、自定义错误码和附加值创建 Warn / Create Warn from value, custom error code, and warning value */
        operator fun <T, C : Any, E> invoke(value: T, code: C, warningValue: E): Warn<T, C, Error<C>> {
            return Warn(value, listOf(ExErr(code, warningValue)))
        }

        /** 从值、自定义错误码、消息和附加值创建 Warn / Create Warn from value, custom error code, message, and warning value */
        operator fun <T, C : Any, E> invoke(value: T, code: C, message: String, warningValue: E): Warn<T, C, Error<C>> {
            return Warn(value, listOf(ExErr(code, message, warningValue)))
        }
    }

    /** 始终为 false / Always false for Warn */
    override val ok: Boolean get() = false
    /** 始终为 false / Always false for Warn */
    override val failed: Boolean get() = false
    /** 始终为 true / Always true for Warn */
    override val warned: Boolean get() = true

    /** 第一个警告，列表为空时为 null / The first warning, or null if the list is empty */
    val firstWarning: E? get() = warnings.firstOrNull()
    /** 警告数量 / The number of warnings */
    val size: Int get() = warnings.size
    /** 警告列表是否为空 / Whether the warning list is empty */
    val isEmpty: Boolean get() = warnings.isEmpty()

    /** 第一个警告的错误码 / The error code of the first warning */
    val code get() = firstWarning?.code
    /** 第一个警告的消息 / The message of the first warning */
    val warningMessage get() = firstWarning?.message

    /** 第一个警告是否包含附加值 / Whether the first warning contains an additional value */
    val withWarningValue get() = firstWarning?.withValue ?: false
    /** 第一个警告的附加值 / The additional value of the first warning */
    val warningValue get() = firstWarning?.value

    /**
     * 将值转换为指定类型
     *
     * Attempts to cast the value to the specified type.
     * 尝试将值转换为指定类型。
     *
     * @param U 目标类型 / The target type
     * @return 转换后的值，类型不匹配时返回 null / The cast value, or null if the type doesn't match
     */
    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    /**
     * 映射成功值，保留警告
     *
     * Transforms the success value using the given function, preserving warnings.
     * 使用给定函数转换成功值，保留警告。
     *
     * @param transform 值转换函数 / The transformation function
     * @return 包含转换后值和相同警告的新 Warn / A new Warn with the transformed value and same warnings
     */
    override fun <U> map(transform: (T) -> U): Warn<U, C, E> {
        return Warn(transform(value), warnings)
    }
}

/**
 * 成功标记类
 *
 * Marker class for successful operations without a return value.
 * 用于标记没有返回值的成功操作的标记类。
 */
class Success

/**
 * 全局成功实例
 *
 * Global singleton instance of Success for use in Try results.
 * 用于 Try 结果的全局单例 Success 实例。
 */
val success = Success()

/**
 * 如果为 Ok 则执行函数
 *
 * Executes the given function if this result is Ok, then returns the result unchanged.
 * 如果结果是 Ok 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Ok 结果的处理函数 / The handler function for Ok result
 * @return 未改变的结果 / The unchanged result
 */
inline fun <T, C : Any, E : Error<C>> Result<T, C, E>.ifOk(crossinline func: Ok<T, C, E>.() -> Unit): Result<T, C, E> {
    if (this is Ok) func(this)
    return this
}

/**
 * 如果为 Failed 则执行函数
 *
 * Executes the given function if this result is Failed, then returns the result unchanged.
 * 如果结果是 Failed 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Failed 结果的处理函数 / The handler function for Failed result
 * @return 未改变的结果 / The unchanged result
 */
inline fun <T, C : Any, E : Error<C>> Result<T, C, E>.ifFailed(crossinline func: Failed<T, C, E>.() -> Unit): Result<T, C, E> {
    if (this is Failed) func(this)
    return this
}

/**
 * 如果为 Fatal 则执行函数
 *
 * Executes the given function if this result is Fatal, then returns the result unchanged.
 * 如果结果是 Fatal 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Fatal 结果的处理函数 / The handler function for Fatal result
 * @return 未改变的结果 / The unchanged result
 */
inline fun <T, C : Any, E : Error<C>> Result<T, C, E>.ifFatal(crossinline func: Fatal<T, C, E>.() -> Unit): Result<T, C, E> {
    if (this is Fatal) func(this)
    return this
}

// Extension functions for ExResult / ExResult 的扩展函数
/**
 * 如果为 Ok 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Ok, then returns the result unchanged.
 * 如果 ExResult 是 Ok 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Ok 结果的处理函数 / The handler function for Ok result
 * @return 未改变的扩展结果 / The unchanged ExResult
 */
inline fun <T, C : Any, E : Error<C>> ExResult<T, C, E>.ifOk(crossinline func: Ok<T, C, E>.() -> Unit): ExResult<T, C, E> {
    if (this is Ok) func(this)
    return this
}

/**
 * 如果为 Failed 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Failed, then returns the result unchanged.
 * 如果 ExResult 是 Failed 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Failed 结果的处理函数 / The handler function for Failed result
 * @return 未改变的扩展结果 / The unchanged ExResult
 */
inline fun <T, C : Any, E : Error<C>> ExResult<T, C, E>.ifFailed(crossinline func: Failed<T, C, E>.() -> Unit): ExResult<T, C, E> {
    if (this is Failed) func(this)
    return this
}

/**
 * 如果为 Fatal 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Fatal, then returns the result unchanged.
 * 如果 ExResult 是 Fatal 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Fatal 结果的处理函数 / The handler function for Fatal result
 * @return 未改变的扩展结果 / The unchanged ExResult
 */
inline fun <T, C : Any, E : Error<C>> ExResult<T, C, E>.ifFatal(crossinline func: Fatal<T, C, E>.() -> Unit): ExResult<T, C, E> {
    if (this is Fatal) func(this)
    return this
}

/**
 * 如果为 Warned 则执行函数
 *
 * Executes the given function if this ExResult is Warned, then returns the result unchanged.
 * 如果 ExResult 是 Warned 则执行给定函数，然后返回未改变的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param E 错误的类型 / The type of the error
 * @param func Warn 结果的处理函数 / The handler function for Warn result
 * @return 未改变的结果 / The unchanged result
 */
inline fun <T, C : Any, E : Error<C>> ExResult<T, C, E>.ifWarned(crossinline func: Warn<T, C, E>.() -> Unit): ExResult<T, C, E> {
    if (this is Warn) func(this)
    return this
}

/**
 * 无返回值的 Result 类型别名
 *
 * Type alias for Result without a meaningful return value.
 * 无有意义返回值的 Result 类型别名。
 */
typealias Try = Result<Success, ErrorCode, Error<ErrorCode>>
typealias TryOf<C> = Result<Success, C, Error<C>>

/**
 * 自定义错误类型的 Try 类型别名
 *
 * Type alias for Try with a custom error type.
 * 自定义错误类型的 Try 类型别名。
 */
typealias TryWith<E> = Result<Success, ErrorCode, E>
typealias TryWithOf<C, E> = Result<Success, C, E>

/**
 * 带返回值的 Result 类型别名
 *
 * Type alias for Result with a return value type.
 * 带返回值类型的 Result 类型别名。
 */
typealias Ret<T> = Result<T, ErrorCode, Error<ErrorCode>>
typealias RetOf<T, C> = Result<T, C, Error<C>>

/**
 * 全局 Ok 实例
 *
 * Global Ok instance for Try results.
 * 用于 Try 结果的全局 Ok 实例。
 */
val ok = Ok<Success, ErrorCode, Error<ErrorCode>>(success)

/**
 * 创建 Ok 实例的工厂函数
 *
 * Factory function to create an Ok instance with custom error type.
 * 创建自定义错误类型 Ok 实例的工厂函数。
 */
fun <E : Error<ErrorCode>> ok(): Result<Success, ErrorCode, E> = Ok(success)

/**
 * 顺序执行多个操作块
 *
 * Executes multiple operation blocks sequentially, returning the first failure or Ok.
 * 顺序执行多个操作块，返回第一个失败或 Ok。
 *
 * @param blocks 要执行的操作块 / The operation blocks to execute
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
fun run(
    vararg blocks: () -> Try
): Try {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

/**
 * 异步顺序执行多个操作块
 *
 * Executes multiple suspend operation blocks sequentially, returning the first failure or Ok.
 * 异步顺序执行多个 suspend 操作块，返回第一个失败或 Ok。
 *
 * @param blocks 要执行的异步操作块 / The suspend operation blocks to execute
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
suspend fun syncRun(
    vararg blocks: suspend () -> Try
): Try {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

/**
 * 顺序执行 Iterable 中的多个操作块
 *
 * Executes multiple operation blocks from an Iterable sequentially, returning the first failure or Ok.
 * 顺序执行 Iterable 中的多个操作块，返回第一个失败或 Ok。
 *
 * @param blocks 包含操作块的 Iterable / The Iterable containing operation blocks
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
fun run(
    blocks: Iterable<() -> Try>
): Try {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

/**
 * 异步顺序执行 Iterable 中的多个操作块
 *
 * Executes multiple suspend operation blocks from an Iterable sequentially, returning the first failure or Ok.
 * 异步顺序执行 Iterable 中的多个 suspend 操作块，返回第一个失败或 Ok。
 *
 * @param blocks 包含异步操作块的 Iterable / The Iterable containing suspend operation blocks
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
suspend fun syncRun(
    blocks: Iterable<suspend () -> Try>
): Try {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}

/**
 * 顺序执行多个操作块并返回最终结果
 *
 * Executes multiple operation blocks sequentially, then executes lastBlock and returns its result.
 * 顺序执行多个操作块，然后执行 lastBlock 并返回其结果。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 要执行的操作块 / The operation blocks to execute
 * @param lastBlock 最终返回结果的操作块 / The operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
fun <T> run(
    vararg blocks: () -> Try,
    lastBlock: () -> Ret<T>
): Ret<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return lastBlock()
}

/**
 * 异步顺序执行多个操作块并返回最终结果
 *
 * Executes multiple suspend operation blocks sequentially, then executes lastBlock and returns its result.
 * 异步顺序执行多个 suspend 操作块，然后执行 lastBlock 并返回其结果。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 要执行的异步操作块 / The suspend operation blocks to execute
 * @param lastBlock 最终返回结果的异步操作块 / The suspend operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
suspend fun <T> syncRun(
    vararg blocks: suspend () -> Try,
    lastBlock: suspend () -> Ret<T>
): Ret<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return lastBlock()
}

/**
 * 顺序执行 Iterable 中的多个操作块并返回最终结果
 *
 * Executes multiple operation blocks from an Iterable sequentially, then executes lastBlock and returns its result.
 * 顺序执行 Iterable 中的多个操作块，然后执行 lastBlock 并返回其结果。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 包含操作块的 Iterable / The Iterable containing operation blocks
 * @param lastBlock 最终返回结果的操作块 / The operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
fun <T> run(
    blocks: Iterable<() -> Try>,
    lastBlock: () -> Ret<T>
): Ret<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return lastBlock()
}

/**
 * 异步顺序执行 Iterable 中的多个操作块并返回最终结果
 *
 * Executes multiple suspend operation blocks from an Iterable sequentially, then executes lastBlock and returns its result.
 * 异步顺序执行 Iterable 中的多个 suspend 操作块，然后执行 lastBlock 并返回其结果。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 包含异步操作块的 Iterable / The Iterable containing suspend operation blocks
 * @param lastBlock 最终返回结果的异步操作块 / The suspend operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
suspend fun <T> syncRun(
    blocks: Iterable<suspend () -> Try>,
    lastBlock: suspend () -> Ret<T>
): Ret<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return lastBlock()
}

/**
 * 无返回值的 ExResult 类型别名
 *
 * Type alias for ExResult without a meaningful return value.
 * 无有意义返回值的 ExResult 类型别名。
 */
typealias ExTry = ExResult<Success, ErrorCode, Error<ErrorCode>>
typealias ExTryWithCode<C> = ExResult<Success, C, Error<C>>

/**
 * 自定义错误类型的 ExTry 类型别名
 *
 * Type alias for ExTry with a custom error type.
 * 自定义错误类型的 ExTry 类型别名。
 */
typealias ExTryWith<E> = ExResult<Success, ErrorCode, E>

/**
 * 带返回值的 ExResult 类型别名
 *
 * Type alias for ExResult with a return value type.
 * 带返回值类型的 ExResult 类型别名。
 */
typealias ExRet<T> = ExResult<T, ErrorCode, Error<ErrorCode>>
typealias ExRetWithCode<T, C> = ExResult<T, C, Error<C>>

/**
 * 全局 ExOk 实例
 *
 * Global Ok instance for ExTry results.
 * 用于 ExTry 结果的全局 Ok 实例。
 */
val exOk = Ok<Success, ErrorCode, Error<ErrorCode>>(success)

/**
 * 创建 ExOk 实例的工厂函数
 *
 * Factory function to create an Ok instance with custom error type for ExResult.
 * 创建自定义错误类型 ExOk 实例的工厂函数。
 */
fun <E : Error<ErrorCode>> exOk(): ExResult<Success, ErrorCode, E> = Ok(success)

/**
 * 顺序执行多个扩展操作块
 *
 * Executes multiple extended operation blocks sequentially, returning the first failure or Ok.
 * Warnings are ignored and execution continues.
 * 顺序执行多个扩展操作块，返回第一个失败或 Ok。警告被忽略并继续执行。
 *
 * @param blocks 要执行的操作块 / The operation blocks to execute
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
fun exRun(
    vararg blocks: () -> ExTry
): ExTry {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return exOk
}

/**
 * 异步顺序执行多个扩展操作块
 *
 * Executes multiple suspend extended operation blocks sequentially, returning the first failure or Ok.
 * Warnings are ignored and execution continues.
 * 异步顺序执行多个扩展 suspend 操作块，返回第一个失败或 Ok。警告被忽略并继续执行。
 *
 * @param blocks 要执行的异步操作块 / The suspend operation blocks to execute
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
suspend fun exSyncRun(
    vararg blocks: suspend () -> ExTry
): ExTry {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return exOk
}

/**
 * 顺序执行 Iterable 中的多个扩展操作块
 *
 * Executes multiple extended operation blocks from an Iterable sequentially, returning the first failure or Ok.
 * Warnings are ignored and execution continues.
 * 顺序执行 Iterable 中的多个扩展操作块，返回第一个失败或 Ok。警告被忽略并继续执行。
 *
 * @param blocks 包含操作块的 Iterable / The Iterable containing operation blocks
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
fun exRun(
    blocks: Iterable<() -> ExTry>
): ExTry {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return exOk
}

/**
 * 异步顺序执行 Iterable 中的多个扩展操作块
 *
 * Executes multiple suspend extended operation blocks from an Iterable sequentially, returning the first failure or Ok.
 * Warnings are ignored and execution continues.
 * 异步顺序执行 Iterable 中的多个扩展 suspend 操作块，返回第一个失败或 Ok。警告被忽略并继续执行。
 *
 * @param blocks 包含异步操作块的 Iterable / The Iterable containing suspend operation blocks
 * @return 第一个失败结果或 Ok / The first failure result or Ok
 */
suspend fun exSyncRun(
    blocks: Iterable<suspend () -> ExTry>
): ExTry {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return exOk
}

/**
 * 顺序执行多个扩展操作块并返回最终结果
 *
 * Executes multiple extended operation blocks sequentially, then executes lastBlock and returns its result.
 * Warnings are ignored and execution continues.
 * 顺序执行多个扩展操作块，然后执行 lastBlock 并返回其结果。警告被忽略并继续执行。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 要执行的操作块 / The operation blocks to execute
 * @param lastBlock 最终返回结果的操作块 / The operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
fun <T> exRun(
    vararg blocks: () -> ExTry,
    lastBlock: () -> ExRet<T>
): ExRet<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return lastBlock()
}

/**
 * 异步顺序执行多个扩展操作块并返回最终结果
 *
 * Executes multiple suspend extended operation blocks sequentially, then executes lastBlock and returns its result.
 * Warnings are ignored and execution continues.
 * 异步顺序执行多个扩展 suspend 操作块，然后执行 lastBlock 并返回其结果。警告被忽略并继续执行。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 要执行的异步操作块 / The suspend operation blocks to execute
 * @param lastBlock 最终返回结果的异步操作块 / The suspend operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
suspend fun <T> exSyncRun(
    vararg blocks: suspend () -> ExTry,
    lastBlock: suspend () -> ExRet<T>
): ExRet<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return lastBlock()
}

/**
 * 顺序执行 Iterable 中的多个扩展操作块并返回最终结果
 *
 * Executes multiple extended operation blocks from an Iterable sequentially, then executes lastBlock and returns its result.
 * Warnings are ignored and execution continues.
 * 顺序执行 Iterable 中的多个扩展操作块，然后执行 lastBlock 并返回其结果。警告被忽略并继续执行。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 包含操作块的 Iterable / The Iterable containing operation blocks
 * @param lastBlock 最终返回结果的操作块 / The operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
fun <T> exRun(
    blocks: Iterable<() -> ExTry>,
    lastBlock: () -> ExRet<T>
): ExRet<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return lastBlock()
}

/**
 * 异步顺序执行 Iterable 中的多个扩展操作块并返回最终结果
 *
 * Executes multiple suspend extended operation blocks from an Iterable sequentially, then executes lastBlock and returns its result.
 * Warnings are ignored and execution continues.
 * 异步顺序执行 Iterable 中的多个扩展 suspend 操作块，然后执行 lastBlock 并返回其结果。警告被忽略并继续执行。
 *
 * @param T 最终返回值的类型 / The type of the final return value
 * @param blocks 包含异步操作块的 Iterable / The Iterable containing suspend operation blocks
 * @param lastBlock 最终返回结果的异步操作块 / The suspend operation block that returns the final result
 * @return 最后一个块的结果或第一个失败 / The result of the last block or the first failure
 */
suspend fun <T> exSyncRun(
    blocks: Iterable<suspend () -> ExTry>,
    lastBlock: suspend () -> ExRet<T>
): ExRet<T> {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
            is Warn -> {}
        }
    }
    return lastBlock()
}
