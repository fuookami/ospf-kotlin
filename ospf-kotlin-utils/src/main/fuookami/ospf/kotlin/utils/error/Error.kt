package fuookami.ospf.kotlin.utils.error

sealed class Error {
    companion object {
        operator fun invoke(code: ErrorCode, message: String): Err {
            return Err(code, message)
        }
    }

    abstract val code: ErrorCode
    abstract val message: String
    open val value: Any? = null

    val withValue get() = value != null

    override fun toString(): String {
        return if (value != null) {
            "$code: $message($value)"
        } else {
            "$code: $message"
        }
    }
}

open class Err(
    override val code: ErrorCode,
    override val message: String
) : Error() {
    companion object {
        operator fun invoke(code: ErrorCode, message: String? = null): Err {
            return if (message == null) {
                Err(code, code.toString())
            } else {
                Err(code, message)
            }
        }
    }
}

open class ExErr<T>(
    override val code: ErrorCode,
    override val message: String,
    override val value: T
) : Error() {
    constructor(code: ErrorCode, value: T) : this(code, code.toString(), value)
}

data class ApplicationException(
    val error: Error
) : Throwable() {
    override val message: String by error::message

    override fun toString(): String {
        return "$error"
    }
}
