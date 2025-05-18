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

    val totalProgress: Flt64 get() = readySymbolAmount.toFlt64() / totalSymbolAmount.toFlt64()
    val notEmptyProgress: Flt64 get() = (readyNotEmptySymbolAmount.toFlt64() / notEmptySymbolAmount.toFlt64())
}

typealias RegistrationStatusCallBack = (RegistrationStatus) -> Try
