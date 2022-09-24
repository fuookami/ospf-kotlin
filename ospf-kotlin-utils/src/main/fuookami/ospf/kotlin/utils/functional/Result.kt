package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode

sealed class Result<T, E : Error> {
    fun isOk() = this is Ok
    fun isFailed() = this is Failed
    fun value(): T? = when (this) {
        is Ok -> {
            this.value
        }

        is Failed -> {
            null
        }
    }
}

class Ok<T, E : Error>(
    val value: T
) : Result<T, E>() {}

class Failed<T, E : Error>(
    val error: E
) : Result<T, E>() {
    val code: ErrorCode get() = this.error.code()
    val message: String get() = this.error.message()

    fun withValue() = this.error.withValue()
    fun errValue() = this.error.value()
}

class Success

val success = Success()

typealias Try<E> = Result<Success, E>
