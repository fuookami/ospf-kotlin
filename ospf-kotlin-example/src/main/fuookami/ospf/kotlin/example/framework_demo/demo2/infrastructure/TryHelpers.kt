package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Execute a Try-returning operation, returning early on failure.
 * Reduces the repetitive when/Ok/Failed/Fatal boilerplate.
 */
inline fun Try.orReturn(failedHandler: (Error<ErrorCode>) -> Nothing, fatalHandler: (List<Error<ErrorCode>>) -> Nothing) {
    when (this) {
        is Ok -> {}
        is Failed -> failedHandler(error)
        is Fatal -> fatalHandler(errors)
    }
}

/**
 * Execute a Ret<T>-returning operation, returning the value or throwing on failure.
 */
inline fun <T> Ret<T>.orReturn(
    failedHandler: (Error<ErrorCode>) -> Nothing,
    fatalHandler: (List<Error<ErrorCode>>) -> Nothing
): T {
    return when (this) {
        is Ok -> value
        is Failed -> failedHandler(error)
        is Fatal -> fatalHandler(errors)
    }
}
