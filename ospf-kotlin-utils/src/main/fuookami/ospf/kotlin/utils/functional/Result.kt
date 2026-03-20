package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*

sealed interface Result<out T, out E : Error> {
    val ok get() = false
    val failed get() = false
    val value: T? get() = null

    fun <U> map(transform: (T) -> U): Result<U, E>
}

inline fun <T, E : Error> Result<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): Result<T, E> {
    when (this) {
        is Ok -> {
            func(this)
        }

        is Failed -> {}
    }
    return this
}

inline fun <T, E : Error> Result<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): Result<T, E> {
    when (this) {
        is Ok -> {}

        is Failed -> {
            func(this)
        }
    }
    return this
}

/**
 * Extended Result type that supports Warning and Fatal states.
 *
 * 扩展的结果类型，支持警告和致命错误状态。
 */
sealed interface ExResult<out T, out E : Error> {
    val ok get() = false
    val failed get() = false
    val warning get() = false
    val value: T? get() = null

    fun <U> emap(transform: (T) -> U): ExResult<U, E>
}

inline fun <T, E : Error> ExResult<T, E>.ifOk(crossinline func: Ok<T, E>.() -> Unit): ExResult<T, E> {
    when (this) {
        is Ok -> {
            func(this)
        }
        is Failed -> {}
        is Warning -> {}
        is Fatal -> {}
    }
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifFailed(crossinline func: Failed<T, E>.() -> Unit): ExResult<T, E> {
    when (this) {
        is Ok -> {}
        is Failed -> {
            func(this)
        }
        is Warning -> {}
        is Fatal -> {}
    }
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifWarning(crossinline func: Warning<T, E>.() -> Unit): ExResult<T, E> {
    when (this) {
        is Ok -> {}
        is Failed -> {}
        is Warning -> {
            func(this)
        }
        is Fatal -> {}
    }
    return this
}

inline fun <T, E : Error> ExResult<T, E>.ifFatal(crossinline func: Fatal<T, E>.() -> Unit): ExResult<T, E> {
    when (this) {
        is Ok -> {}
        is Failed -> {}
        is Warning -> {}
        is Fatal -> {
            func(this)
        }
    }
    return this
}

class Ok<out T, out E : Error>(
    override val value: T
) : Result<T, E>, ExResult<T, E> {
    override val ok = true
    override val failed = false

    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    override fun <U> map(transform: Extractor<U, T>): Result<U, E> {
        return Ok(transform(value))
    }

    override fun <U> emap(transform: Extractor<U, T>): ExResult<U, E> {
        return Ok(transform(value))
    }
}

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

    override val ok = false
    override val failed = true
    override val value = null

    val code by error::code
    val message by error::message

    val withValue by error::withValue
    val errValue by error::value

    override fun <U> map(transform: (T) -> U): Result<U, E> {
        return Failed(error)
    }

    override fun <U> emap(transform: (T) -> U): ExResult<U, E> {
        return Failed(error)
    }
}

/**
 * Warning state - represents operation completed with warnings.
 * 
 * 警告状态 - 表示操作完成但有警告。
 */
class Warning<out T, out E : Error>(
    override val value: T,
    val errors: List<E>
) : ExResult<T, E> {
    companion object {
        operator fun <T, E: Error> invoke(value: T, error: E): Warning<T, E> {
            return Warning(value, listOf(error))
        }
    }

    override val warning = true

    inline fun <reified U> getAs(): U? {
        return value as? U
    }

    override fun <U> emap(transform: (T) -> U): Warning<U, E> {
        return Warning(transform(value), errors)
    }
}

/**
 * Fatal state - represents operation failed with multiple errors.
 * 
 * 致命错误状态 - 表示操作失败并有多个错误。
 */
class Fatal<out T, out E : Error>(
    val errors: List<E>
) : ExResult<T, E> {
    companion object {
        operator fun <T> invoke(vararg errors: Error): Fatal<T, Error> {
            return Fatal(errors.toList())
        }

        operator fun <T> invoke(errors: List<Error>): Fatal<T, Error> {
            return Fatal(errors)
        }

        operator fun <T> invoke(error: Error): Fatal<T, Error> {
            return Fatal(listOf(error))
        }
    }

    override val failed = true

    val error: E get() = errors.first()
    val firstError: E? get() = errors.firstOrNull()
    val size: Int get() = errors.size
    val isEmpty: Boolean get() = errors.isEmpty()
    val isNotEmpty: Boolean get() = errors.isNotEmpty()

    val code by lazy { error.code }
    val message by lazy { error.message }

    override fun <U> emap(transform: (T) -> U): Fatal<U, E> {
        return Fatal(errors)
    }
}

class Success

val success = Success()

typealias Try = Result<Success, Error>
typealias TryWith<E> = Result<Success, E>
typealias Ret<T> = Result<T, Error>

typealias ExTry = ExResult<Success, Error>
typealias ExTryWith<E> = ExResult<Success, E>
typealias ExRet<T> = ExResult<T, Error>

val ok = Ok<Success, Error>(success)
fun <E : Error> ok(): Result<Success, E> = Ok(success)

fun run(
    vararg blocks: () -> Try
): Try {
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
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

            is Failed -> {
                return Failed(result.error)
            }
        }
    }

    return lastBlock()
}

/**
 * Runs blocks and collects warnings, returning ExResult.
 * 
 * 运行代码块并收集警告，返回 ExResult。
 */
fun <T> runWithWarnings(
    vararg blocks: () -> ExTry,
    lastBlock: () -> ExRet<T>
): ExRet<T> {
    val errors = mutableListOf<Error>()
    
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Warning -> {
                errors.addAll(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
    }

    return when (val result = lastBlock()) {
        is Ok -> {
            if (errors.isEmpty()) {
                Ok(result.value)
            } else {
                Warning(result.value, errors)
            }
        }

        is Failed -> {
            Failed(result.error)
        }

        is Warning -> {
            errors.addAll(result.errors)
            Warning(result.value, errors)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

/**
 * Runs blocks and collects warnings, returning ExResult.
 * 
 * 运行代码块并收集警告，返回 ExResult。
 */
suspend fun <T> syncRunWithWarnings(
    vararg blocks: suspend () -> ExTry,
    lastBlock: suspend () -> ExRet<T>
): ExRet<T> {
    val errors = mutableListOf<Error>()
    
    for (block in blocks) {
        when (val result = block()) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Warning -> {
                errors.addAll(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
    }

    return when (val result = lastBlock()) {
        is Ok -> {
            if (errors.isEmpty()) {
                Ok(result.value)
            } else {
                Warning(result.value, errors)
            }
        }

        is Failed -> {
            Failed(result.error)
        }

        is Warning -> {
            errors.addAll(result.errors)
            Warning(result.value, errors)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}
