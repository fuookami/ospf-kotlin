package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Execute a Try-returning operation, returning early on failure.
 * Reduces the repetitive when/Ok/Failed/Fatal boilerplate.
 */
inline fun Try.orReturn(failedHandler: (Error<ErrorCode>) -> Nothing, fatalHandler: (List<Error<ErrorCode>>) -> Nothing) {
    when (this) {
        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}
        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> failedHandler(error)
        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> fatalHandler(errors)
    }
}

/**
 * Execute a Ret<T>-returning operation, returning the value or throwing on failure.
 */
inline fun <T> Ret<T>.orReturn(
    failedHandler: (Error<ErrorCode>) -> Nothing,
    fatalHandler: (List<Error<ErrorCode>>) -> Nothing
): T {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> value as T
        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> failedHandler(error)
        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> fatalHandler(errors)
    }
}