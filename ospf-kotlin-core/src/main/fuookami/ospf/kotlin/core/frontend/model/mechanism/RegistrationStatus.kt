package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class RegistrationStatus(
    val readySymbolAmount: UInt64,
    val totalSymbolAmount: UInt64
)

typealias RegistrationStatusCallBack = (RegistrationStatus) -> Try
