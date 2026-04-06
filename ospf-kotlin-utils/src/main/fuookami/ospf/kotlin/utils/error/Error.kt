/**
 * 错误类型定义
 *
 * Error type definitions for the ospf-kotlin-utils library.
 * ospf-kotlin-utils 库的错误类型定义。
 *
 * This file provides a sealed class hierarchy for representing errors,
 * including basic error types and an application exception wrapper.
 * 此文件提供了一个密封类层次结构用于表示错误，
 * 包括基本错误类型和应用程序异常包装器。
 */
package fuookami.ospf.kotlin.utils.error

/**
 * 错误基类
 *
 * Sealed base class for all error types in the library.
 * 库中所有错误类型的密封基类。
 *
 * This class provides a common interface for error handling with
 * error codes, messages, and optional values.
 * 此类为错误处理提供了通用接口，
 * 包含错误代码、消息和可选值。
 */
sealed class Error {
    companion object {
        /**
         * 创建错误实例
         *
         * Creates an Err instance with the specified code and message.
         * 使用指定的代码和消息创建 Err 实例。
         *
         * @param code 错误代码 / The error code
         * @param message 错误消息 / The error message
         * @return 新的 Err 实例 / A new Err instance
         */
        operator fun invoke(code: ErrorCode, message: String): Err {
            return Err(code, message)
        }
    }

    /**
     * 错误代码
     *
     * The error code identifying the type of error.
     * 标识错误类型的错误代码。
     */
    abstract val code: ErrorCode

    /**
     * 错误消息
     *
     * The human-readable error message.
     * 人类可读的错误消息。
     */
    abstract val message: String

    /**
     * 关联值
     *
     * An optional value associated with the error.
     * 与错误关联的可选值。
     */
    open val value: Any? = null

    /**
     * 是否有关联值
     *
     * Returns true if the error has an associated value.
     * 如果错误有关联值则返回 true。
     */
    val withValue get() = value != null

    /**
     * 返回错误字符串表示
     *
     * Returns the string representation of the error.
     * 返回错误的字符串表示。
     *
     * @return 格式化的错误字符串 / The formatted error string
     */
    override fun toString(): String {
        return if (value != null) {
            "$code: $message($value)"
        } else {
            "$code: $message"
        }
    }
}

/**
 * 基本错误类
 *
 * Basic error class for simple error cases without associated values.
 * 用于简单错误情况的基本错误类，不带关联值。
 *
 * @property code 错误代码 / The error code
 * @property message 错误消息 / The error message
 */
open class Err(
    override val code: ErrorCode,
    override val message: String
) : Error() {
    companion object {
        /**
         * 创建错误实例
         *
         * Creates an Err instance with the specified code and optional message.
         * 使用指定的代码和可选消息创建 Err 实例。
         *
         * @param code 错误代码 / The error code
         * @param message 可选的错误消息，默认使用代码的字符串表示 /
         *                Optional error message, defaults to code's string representation
         * @return 新的 Err 实例 / A new Err instance
         */
        operator fun invoke(code: ErrorCode, message: String? = null): Err {
            return if (message == null) {
                Err(code, code.toString())
            } else {
                Err(code, message)
            }
        }
    }
}

/**
 * 带值的扩展错误类
 *
 * Extended error class that carries an associated value of type T.
 * 携带类型 T 关联值的扩展错误类。
 *
 * This class is useful when additional context is needed for error handling,
 * such as the problematic value that caused the error.
 * 此类在错误处理需要额外上下文时很有用，
 * 例如导致错误的问题值。
 *
 * @param T 关联值的类型 / The type of the associated value
 * @property code 错误代码 / The error code
 * @property message 错误消息 / The error message
 * @property value 与错误关联的值 / The value associated with the error
 */
open class ExErr<T>(
    override val code: ErrorCode,
    override val message: String,
    override val value: T
) : Error() {
    /**
     * 创建只带代码和值的实例
     *
     * Creates an instance with code and value, using code's string representation as message.
     * 创建带代码和值的实例，使用代码的字符串表示作为消息。
     *
     * @param code 错误代码 / The error code
     * @param value 与错误关联的值 / The value associated with the error
     */
    constructor(code: ErrorCode, value: T) : this(code, code.toString(), value)
}

/**
 * 应用程序异常
 *
 * An exception wrapper for Error instances, allowing errors to be thrown as exceptions.
 * Error 实例的异常包装器，允许将错误作为异常抛出。
 *
 * @property error 包装的错误实例 / The wrapped error instance
 */
data class ApplicationException(
    val error: Error
) : Throwable() {
    /**
     * 异常消息
     *
     * The exception message, delegated from the wrapped error.
     * 异常消息，从包装的错误委托而来。
     */
    override val message: String by error::message

    /**
     * 返回异常字符串表示
     *
     * Returns the string representation of the exception.
     * 返回异常的字符串表示。
     *
     * @return 错误的字符串表示 / The string representation of the error
     */
    override fun toString(): String {
        return "$error"
    }
}
