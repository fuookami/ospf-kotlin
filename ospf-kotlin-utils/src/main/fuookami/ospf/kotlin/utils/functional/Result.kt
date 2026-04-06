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

import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr

// Result: Basic result type with Ok and Failed states / 基础结果类型，包含 Ok 和 Failed 两种状态
sealed interface Result<out T, out E : Error> {
    val ok: Boolean get() = false
    val failed: Boolean get() = false
    val value: T? get() = null

    fun <U> map(transform: (T) -> U): Result<U, E>
}

// ExResult: Extended result type with Ok, Failed, and Warn states / 扩展结果类型，包含 Ok、Failed 和 Warn 三种状态
sealed interface ExResult<out T, out E : Error> {
    val ok: Boolean get() = false
    val failed: Boolean get() = false
    val warned: Boolean get() = false
    val value: T? get() = null

    fun <U> map(transform: (T) -> U): ExResult<U, E>
}

// Ok: Success result with value / 成功结果，包含值
// Implements both Result and ExResult for code reuse / 同时实现 Result 和 ExResult 以实现代码复用
class Ok<out T, out E : Error>(
    override val value: T
) : Result<T, E>, ExResult<T, E> {
    override val ok: Boolean get() = true
    override val failed: Boolean get() = false

    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    override fun <U> map(transform: (T) -> U): Ok<U, E> {
        return Ok(transform(value))
    }
}

// Failed: Failed result with error / 失败结果，包含错误
// Implements both Result and ExResult for code reuse / 同时实现 Result 和 ExResult 以实现代码复用
class Failed<out T, out E : Error>(
    val error: E
) : Result<T, E>, ExResult<T, E> {
    companion object {
        operator fun <T> invoke(code: ErrorCode, message: String? = null): Failed<T, Error> {
            return Failed(Err(code, message))
        }

        operator fun <T, E> invoke(code: ErrorCode, value: E): Failed<T, Error> {
            return Failed(ExErr(code, value))
        }

        operator fun <T, E> invoke(code: ErrorCode, message: String, value: E): Failed<T, Error> {
            return Failed(ExErr(code, message, value))
        }
    }

    override val ok: Boolean get() = false
    override val failed: Boolean get() = true
    override val value: T? get() = null

    val code by error::code
    val message by error::message

    val withValue by error::withValue
    val errValue by error::value

    override fun <U> map(transform: (T) -> U): Failed<U, E> {
        return Failed(error)
    }
}

// Fatal: Fatal result with multiple errors / 致命结果，包含多个错误
// Implements both Result and ExResult for code reuse / 同时实现 Result 和 ExResult 以实现代码复用
class Fatal<out T, out E : Error>(
    val errors: List<E>
) : Result<T, E>, ExResult<T, E> {
    companion object {
        operator fun <T> invoke(vararg errors: Error): Fatal<T, Error> {
            return Fatal(errors.toList())
        }

        operator fun <T> invoke(code: ErrorCode, message: String? = null): Fatal<T, Error> {
            return Fatal(listOf(Err(code, message)))
        }

        operator fun <T, E> invoke(code: ErrorCode, value: E): Fatal<T, Error> {
            return Fatal(listOf(ExErr(code, value)))
        }

        operator fun <T, E> invoke(code: ErrorCode, message: String, value: E): Fatal<T, Error> {
            return Fatal(listOf(ExErr(code, message, value)))
        }
    }

    override val ok: Boolean get() = false
    override val failed: Boolean get() = true
    override val value: T? get() = null

    val firstError: E? get() = errors.firstOrNull()
    val size: Int get() = errors.size
    val isEmpty: Boolean get() = errors.isEmpty()

    override fun <U> map(transform: (T) -> U): Fatal<U, E> {
        return Fatal(errors)
    }

    fun <U : Error> merge(other: Fatal<*, U>): Fatal<T, Error> {
        return Fatal(errors + other.errors)
    }

    inline fun forEach(action: (E) -> Unit) {
        errors.forEach(action)
    }
}

// Warn: Warning result with both value and warning error / 警告结果，包含值和警告错误
// Only implements ExResult / 仅实现 ExResult
class Warn<out T, out E : Error>(
    override val value: T,
    val warnings: List<E>
) : ExResult<T, E> {
    companion object {
        operator fun <T> invoke(value: T, code: ErrorCode, message: String? = null): Warn<T, Error> {
            return Warn(value, listOf(Err(code, message)))
        }

        operator fun <T, E> invoke(value: T, code: ErrorCode, warningValue: E): Warn<T, Error> {
            return Warn(value, listOf(ExErr(code, warningValue)))
        }

        operator fun <T, E> invoke(value: T, code: ErrorCode, message: String, warningValue: E): Warn<T, Error> {
            return Warn(value, listOf(ExErr(code, message, warningValue)))
        }
    }

    override val ok: Boolean get() = false
    override val failed: Boolean get() = false
    override val warned: Boolean get() = true

    val firstWarning: E? get() = warnings.firstOrNull()
    val size: Int get() = warnings.size
    val isEmpty: Boolean get() = warnings.isEmpty()

    val code get() = firstWarning?.code
    val warningMessage get() = firstWarning?.message

    val withWarningValue get() = firstWarning?.withValue ?: false
    val warningValue get() = firstWarning?.value

    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    override fun <U> map(transform: (T) -> U): Warn<U, E> {
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
inline fun <T, E : Error> Result<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): Result<T, E> {
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
inline fun <T, E : Error> Result<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): Result<T, E> {
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
inline fun <T, E : Error> Result<T, E>.ifFatal(crossinline func: Fatal<T, E>.() -> Unit): Result<T, E> {
    if (this is Fatal) func(this)
    return this
}

// Extension functions for ExResult / ExResult 的扩展函数
/**
 * 如果为 Ok 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Ok, then returns the result unchanged.
 * 如果 ExResult 是 Ok 则执行给定函数，然后返回未改变的结果。
 */
inline fun <T, E : Error> ExResult<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Ok) func(this)
    return this
}

/**
 * 如果为 Failed 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Failed, then returns the result unchanged.
 * 如果 ExResult 是 Failed 则执行给定函数，然后返回未改变的结果。
 */
inline fun <T, E : Error> ExResult<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Failed) func(this)
    return this
}

/**
 * 如果为 Fatal 则执行函数（ExResult 版本）
 *
 * Executes the given function if this ExResult is Fatal, then returns the result unchanged.
 * 如果 ExResult 是 Fatal 则执行给定函数，然后返回未改变的结果。
 */
inline fun <T, E : Error> ExResult<T, E>.ifFatal(crossinline func: Fatal<T, E>.() -> Unit): ExResult<T, E> {
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
inline fun <T, E : Error> ExResult<T, E>.ifWarned(crossinline func: Warn<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Warn) func(this)
    return this
}

/**
 * 无返回值的 Result 类型别名
 *
 * Type alias for Result without a meaningful return value.
 * 无有意义返回值的 Result 类型别名。
 */
typealias Try = Result<Success, Error>

/**
 * 自定义错误类型的 Try 类型别名
 *
 * Type alias for Try with a custom error type.
 * 自定义错误类型的 Try 类型别名。
 */
typealias TryWith<E> = Result<Success, E>

/**
 * 带返回值的 Result 类型别名
 *
 * Type alias for Result with a return value type.
 * 带返回值类型的 Result 类型别名。
 */
typealias Ret<T> = Result<T, Error>

/**
 * 全局 Ok 实例
 *
 * Global Ok instance for Try results.
 * 用于 Try 结果的全局 Ok 实例。
 */
val ok = Ok<Success, Error>(success)

/**
 * 创建 Ok 实例的工厂函数
 *
 * Factory function to create an Ok instance with custom error type.
 * 创建自定义错误类型 Ok 实例的工厂函数。
 */
fun <E : Error> ok(): Result<Success, E> = Ok(success)

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
typealias ExTry = ExResult<Success, Error>

/**
 * 自定义错误类型的 ExTry 类型别名
 *
 * Type alias for ExTry with a custom error type.
 * 自定义错误类型的 ExTry 类型别名。
 */
typealias ExTryWith<E> = ExResult<Success, E>

/**
 * 带返回值的 ExResult 类型别名
 *
 * Type alias for ExResult with a return value type.
 * 带返回值类型的 ExResult 类型别名。
 */
typealias ExRet<T> = ExResult<T, Error>

/**
 * 全局 ExOk 实例
 *
 * Global Ok instance for ExTry results.
 * 用于 ExTry 结果的全局 Ok 实例。
 */
val exOk = Ok<Success, Error>(success)

/**
 * 创建 ExOk 实例的工厂函数
 *
 * Factory function to create an Ok instance with custom error type for ExResult.
 * 创建自定义错误类型 ExOk 实例的工厂函数。
 */
fun <E : Error> exOk(): ExResult<Success, E> = Ok(success)

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
