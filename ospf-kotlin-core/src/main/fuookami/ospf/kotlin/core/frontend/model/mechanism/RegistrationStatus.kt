package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class RegistrationStatus(
    val emptySymbolAmount: UInt64,
    val readySymbolAmount: UInt64,
    val totalSymbolAmount: UInt64
) {
    val notEmptySymbolAmount: UInt64 get() = totalSymbolAmount - emptySymbolAmount
    val readyNotEmptySymbolAmount: UInt64 get() = readySymbolAmount - emptySymbolAmount

    val totalProgress: Flt64 get() = if (totalSymbolAmount neq UInt64.zero) {
        readySymbolAmount.toFlt64() / totalSymbolAmount.toFlt64()
    } else {
        Flt64.one
    }

    val notEmptyProgress: Flt64 get() = if (notEmptySymbolAmount neq UInt64.zero) {
        (readyNotEmptySymbolAmount.toFlt64() / notEmptySymbolAmount.toFlt64())
    } else {
        Flt64.one
    }
}

typealias RegistrationStatusCallBack = (RegistrationStatus) -> Try
