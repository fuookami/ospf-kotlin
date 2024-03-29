package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*

sealed class Result<out T, out E : Error> {
    open val ok = false
    open val failed = false
    open val value: T? = null

    abstract fun <U> map(transform: (T) -> U): Result<U, E>
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
