package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*

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
    val warning: E
) : ExResult<T, E> {
    companion object {
        operator fun <T> invoke(value: T, code: ErrorCode, message: String? = null): Warn<T, Error> {
            return Warn(value, Err(code, message))
        }

        operator fun <T, E> invoke(value: T, code: ErrorCode, warningValue: E): Warn<T, Error> {
            return Warn(value, ExErr(code, warningValue))
        }

        operator fun <T, E> invoke(value: T, code: ErrorCode, message: String, warningValue: E): Warn<T, Error> {
            return Warn(value, ExErr(code, message, warningValue))
        }
    }

    override val ok: Boolean get() = false
    override val failed: Boolean get() = false
    override val warned: Boolean get() = true

    val code by warning::code
    val warningMessage by warning::message

    val withWarningValue by warning::withValue
    val warningValue by warning::value

    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    override fun <U> map(transform: (T) -> U): Warn<U, E> {
        return Warn(transform(value), warning)
    }
}

class Success

val success = Success()

// Extension functions for Result / Result 的扩展函数
inline fun <T, E : Error> Result<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): Result<T, E> {
    if (this is Ok) func(this)
    return this
}

inline fun <T, E : Error> Result<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): Result<T, E> {
    if (this is Failed) func(this)
    return this
}

inline fun <T, E : Error> Result<T, E>.ifFatal(crossinline func: Fatal<T, E>.() -> Unit): Result<T, E> {
    if (this is Fatal) func(this)
    return this
}

// Extension functions for ExResult / ExResult 的扩展函数
inline fun <T, E : Error> ExResult<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Ok) func(this)
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Failed) func(this)
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifFatal(crossinline func: Fatal<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Fatal) func(this)
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifWarned(crossinline func: Warn<T, E>.() -> Unit): ExResult<T, E> {
    if (this is Warn) func(this)
    return this
}

typealias Try = Result<Success, Error>
typealias TryWith<E> = Result<Success, E>
typealias Ret<T> = Result<T, Error>

val ok = Ok<Success, Error>(success)
fun <E : Error> ok(): Result<Success, E> = Ok(success)

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

// Type aliases for ExResult / ExResult 的类型别名
typealias ExTry = ExResult<Success, Error>
typealias ExTryWith<E> = ExResult<Success, E>
typealias ExRet<T> = ExResult<T, Error>

val exOk = Ok<Success, Error>(success)
fun <E : Error> exOk(): ExResult<Success, E> = Ok(success)

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
