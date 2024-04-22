package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*

sealed class Result<out T, out E : Error> {
    open val ok = false
    open val failed = false
    open val value: T? = null

    abstract fun <U> map(transform: (T) -> U): Result<U, E>

    inline fun ifOk(crossinline func: Ok<T, E>.() -> Unit): Result<T, E> {
        when (this) {
            is Ok -> {
                func(this)
            }

            is Failed -> {}
        }
        return this
    }

    inline fun ifFailed(crossinline func: Failed<T, E>.() -> Unit): Result<T, E> {
        when (this) {
            is Ok -> {}

            is Failed -> {
                func(this)
            }
        }
        return this
    }
}

class Ok<out T, out E : Error>(
    override val value: T
) : Result<T, E>() {
    override val ok = true

    inline fun <reified U> getAs(): U? {
        return if (value is U) {
            value
        } else {
            null
        }
    }

    override fun <U> map(transform: Extractor<U, T>): Result<U, E> {
        return Ok(transform(value))
    }
}

class Failed<out T, out E : Error>(
    val error: E
) : Result<T, E>() {
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

    override val failed = true

    val code by error::code
    val message by error::message

    val withValue by error::withValue
    val errValue by error::value

    override fun <U> map(transform: (T) -> U): Result<U, E> {
        return Failed(error)
    }
}

class Success

val success = Success()

typealias Try = Result<Success, Error>
typealias TryWith<E> = Result<Success, E>
typealias Ret<T> = Result<T, Error>

val ok = Ok<Success, Error>(success)
fun <E : Error> ok(): Result<Success, E> = Ok(success)
