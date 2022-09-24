package fuookami.ospf.kotlin.utils.error

sealed class Error {
    fun code(): ErrorCode = when (this) {
        is Err -> this.code
        is ExErr<*> -> this.code
    }

    fun message() = when (this) {
        is Err -> this.message
        is ExErr<*> -> this.message
    }

    fun withValue() = this is ExErr<*>

    fun value(): Any? = when (this) {
        is Err -> null
        is ExErr<*> -> this.value
    }
}

class Err internal constructor(
    val code: ErrorCode,
    val message: String
) : Error() {
    companion object {
        operator fun invoke(code: ErrorCode, message: String? = null): Err {
            return if(message == null) {
                Err(code, code.toString())
            } else {
                Err(code, message)
            }
        }
    }
}

class ExErr<T>(
    val code: ErrorCode,
    val message: String,
    val value: T
) : Error() {
    constructor(code: ErrorCode, value: T) : this(code, code.toString(), value)
}
